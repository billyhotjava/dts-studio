package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class HttpActionClientTest {
    private final ActionClient.Caller caller = new ActionClient.Caller("finance-user", "fixture-request");
    private ActionServiceProperties bindings(String url) {
        var properties = new ActionServiceProperties();
        properties.setServices(Map.of("business-api", new ActionServiceProperties.Binding(url, "service-token", "fixture-token")));
        return properties;
    }

    @Test void unboundTargetAndMissingIdentityNeverSendARequest() {
        var client = new HttpActionClient(new ObjectMapper(), new ActionServiceProperties());
        assertThat(client.invoke(new ActionClient.Target("unknown","POST","/draft"),Map.of(),caller).body())
                .containsEntry("code","ACTION_TARGET_UNBOUND");
        client = new HttpActionClient(new ObjectMapper(), bindings("http://127.0.0.1:1"));
        assertThat(client.invoke(new ActionClient.Target("business-api","POST","/draft"),Map.of(),null).body())
                .containsEntry("code","ACTION_IDENTITY_REQUIRED");
    }

    @Test void sendsOnlyBoundTargetWithActorAndCorrelationAndDoesNotFollowRedirects() throws Exception {
        AtomicReference<String> received = new AtomicReference<>();
        AtomicInteger externalCalls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/gateway/draft", exchange -> {
            received.set(exchange.getRequestMethod()+"|"+exchange.getRequestHeaders().getFirst("X-DTS-Service-Token")
                    +"|"+exchange.getRequestHeaders().getFirst("X-DTS-User-Id")+"|"+exchange.getRequestHeaders().getFirst("X-DTS-Trace-Id")
                    +"|"+new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] bytes="{\"code\":200,\"id\":42}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.createContext("/gateway/denied", exchange -> {
            byte[] bytes = "{\"success\":false}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.createContext("/gateway/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location","/unexpected"); exchange.sendResponseHeaders(302,-1); exchange.close();
        });
        server.createContext("/unexpected", exchange -> { externalCalls.incrementAndGet(); exchange.sendResponseHeaders(200,-1); exchange.close(); });
        server.start();
        try {
            var client = new HttpActionClient(new ObjectMapper(),bindings("http://127.0.0.1:"+server.getAddress().getPort()+"/gateway"));
            var response=client.invoke(new ActionClient.Target("business-api","PATCH","/draft"),Map.of("projectId",101),caller);
            assertThat(response.success()).isTrue();
            assertThat(received.get()).isEqualTo("PATCH|fixture-token|finance-user|fixture-request|{\"projectId\":101}");
            assertThat(client.invoke(new ActionClient.Target("business-api","POST","/redirect"),Map.of(),caller).body())
                    .containsEntry("code","ACTION_REMOTE_REJECTED");
            for(String path:java.util.List.of("//localhost/unexpected","http://localhost/unexpected","/../unexpected","/%2e%2e/unexpected","/%252e%252e/unexpected")) {
                assertThat(client.invoke(new ActionClient.Target("business-api","POST",path),Map.of(),caller).body())
                        .containsEntry("code","ACTION_TARGET_INVALID");
            }
            assertThat(client.invoke(new ActionClient.Target("business-api","POST","/denied"),Map.of(),caller).success()).isFalse();
            assertThat(externalCalls).hasValue(0);
        } finally { server.stop(0); }
    }

    @Test void boundsResponseBytesAndDoesNotPretendAWriteWasDefinitelyRejected() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/draft",exchange -> {
            exchange.sendResponseHeaders(200,2*1024*1024);
            try { exchange.getResponseBody().write(new byte[2*1024*1024]); } catch(java.io.IOException ignored) {} finally { exchange.close(); }
        });
        server.start();
        try {
            var client = new HttpActionClient(new ObjectMapper(),bindings("http://127.0.0.1:"+server.getAddress().getPort()));
            assertThat(client.invoke(new ActionClient.Target("business-api","POST","/draft"),Map.of(),caller).body())
                    .containsEntry("code","ACTION_STATUS_UNKNOWN");
        } finally { server.stop(0); }
    }
}
