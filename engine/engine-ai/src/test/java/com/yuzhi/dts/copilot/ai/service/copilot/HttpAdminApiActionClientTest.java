package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HttpAdminApiActionClientTest {

    @Test
    void shouldRejectDraftCallWhenAdminApiBaseUrlIsMissing() {
        HttpAdminApiActionClient client = new HttpAdminApiActionClient(
                new ObjectMapper(),
                "",
                "");

        AdminApiActionClient.AdminApiActionResponse response = client.postDraft(
                "/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt",
                Map.of("projectId", 101));

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("copilot.action.adminapi.base-url");
    }

    @Test
    void shouldSendConfiguredAuthorizationHeaderToDraftEndpoint() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "{\"code\":200,\"msg\":\"ok\",\"data\":{\"id\":9001}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            HttpAdminApiActionClient client = new HttpAdminApiActionClient(
                    new ObjectMapper(),
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "Bearer test-token");

            AdminApiActionClient.AdminApiActionResponse response = client.postDraft(
                    "/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt",
                    Map.of("projectId", 101));

            assertThat(response.success()).isTrue();
            assertThat(authorization.get()).isEqualTo("Bearer test-token");
        } finally {
            server.stop(0);
        }
    }
}
