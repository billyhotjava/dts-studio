package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Destination and credentials come only from deployment bindings, never from a Pack URL. */
@Component
public class HttpActionClient implements ActionClient {
    private static final int MAX_RESPONSE = 1024 * 1024;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private final ObjectMapper mapper;
    private final ActionServiceProperties properties;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public HttpActionClient(ObjectMapper mapper, ActionServiceProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public ActionResponse invoke(Target target, Map<String, Object> payload, Caller caller) {
        if (target == null) return failure("ACTION_TARGET_UNBOUND", "该动作在当前环境不可用");
        var binding = properties.getServices().get(target.serviceRef());
        if (binding == null || binding.baseUrl() == null || binding.baseUrl().isBlank())
            return failure("ACTION_TARGET_UNBOUND", "该动作在当前环境不可用");
        if (caller == null || caller.actorId() == null || caller.actorId().isBlank()
                || caller.requestId() == null || caller.requestId().isBlank())
            return failure("ACTION_IDENTITY_REQUIRED", "缺少动作发起人信息");
        if (binding.credential() == null || binding.credential().isBlank())
            return failure("ACTION_AUTH_UNCONFIGURED", "该动作的服务认证尚未配置");
        String authMode = binding.authMode() == null ? "service-token" : binding.authMode();
        if (!Set.of("service-token", "bearer").contains(authMode))
            return failure("ACTION_AUTH_UNSUPPORTED", "当前认证方式尚未接入");
        try {
            URI base = URI.create(binding.baseUrl());
            URI path = URI.create(target.path());
            if (!Set.of("http", "https").contains(base.getScheme()) || base.getHost() == null
                    || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null
                    || !target.path().startsWith("/") || path.isAbsolute() || path.getAuthority() != null
                    || path.getQuery() != null || path.getFragment() != null || path.getRawPath().contains("%")
                    || !Set.of("POST", "PUT", "PATCH", "DELETE").contains(target.method()))
                return failure("ACTION_TARGET_INVALID", "动作目标配置无效");
            com.yuzhi.dts.copilot.ai.service.pack.PackArchiveValidator.requireSafePath(path.getPath().substring(1));
            String baseUrl = binding.baseUrl().replaceAll("/+$", "");
            var request = HttpRequest.newBuilder(URI.create(baseUrl + target.path()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("X-DTS-User-Id", caller.actorId())
                    .header("X-DTS-Service", "dts-studio")
                    .header("X-DTS-Trace-Id", caller.requestId())
                    .header("Idempotency-Key", caller.requestId());
            if (authMode.equals("bearer")) request.header("Authorization", "Bearer " + binding.credential());
            else request.header("X-DTS-Service-Token", binding.credential());
            var response = sendBounded(request.method(target.method(), HttpRequest.BodyPublishers.ofString(
                    mapper.writeValueAsString(payload == null ? Map.of() : payload))).build());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                return failure("ACTION_REMOTE_REJECTED", "业务服务未接受该动作，请核对权限和业务状态");
            Map<String, Object> body = response.body().length == 0 ? Map.of() : mapper.readValue(response.body(), MAP_TYPE);
            Object code = body.get("code");
            boolean success = !Boolean.FALSE.equals(body.get("success")) && (code == null || "200".equals(code.toString()));
            return new ActionResponse(success, success ? "操作已受理" : "业务服务未接受该动作", body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failure("ACTION_STATUS_UNKNOWN", "动作调用已中断，请在业务系统核对处理结果");
        } catch (IOException e) {
            return failure("ACTION_STATUS_UNKNOWN", "暂时无法确认动作结果，请在业务系统核对后再操作");
        } catch (RuntimeException e) {
            return failure("ACTION_TARGET_INVALID", "动作目标配置无效");
        }
    }

    private HttpResponse<byte[]> sendBounded(HttpRequest request) throws IOException, InterruptedException {
        var pending = http.sendAsync(request, ignored -> new LimitedBody());
        try {
            return pending.get(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new java.net.http.HttpTimeoutException("Action deadline exceeded");
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IOException("Action transport failed", e.getCause());
        } finally {
            if (!pending.isDone()) pending.cancel(true);
        }
    }

    private static ActionResponse failure(String code, String message) {
        return new ActionResponse(false, message, Map.of("code", code));
    }

    /** Limits bytes as they arrive; the caller also enforces an overall deadline. */
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private Flow.Subscription subscription;
        public CompletionStage<byte[]> getBody() { return body; }
        public void onSubscribe(Flow.Subscription subscription) { this.subscription = subscription; subscription.request(1); }
        public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > MAX_RESPONSE - bytes.size()) {
                    subscription.cancel(); body.completeExceptionally(new IOException("Action response limit exceeded")); return;
                }
                byte[] data = new byte[buffer.remaining()]; buffer.get(data); bytes.writeBytes(data);
            }
            subscription.request(1);
        }
        public void onError(Throwable error) { body.completeExceptionally(error); }
        public void onComplete() { body.complete(bytes.toByteArray()); }
    }
}
