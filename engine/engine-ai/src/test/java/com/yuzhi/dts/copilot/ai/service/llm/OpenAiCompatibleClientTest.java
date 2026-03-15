package com.yuzhi.dts.copilot.ai.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleClientTest {

    @Test
    void usesVersionedBaseUrlDirectlyForChatCompletion() throws Exception {
        AtomicReference<String> observedPath = new AtomicReference<>();
        try (TestHttpServer server = TestHttpServer.json(exchange -> {
            observedPath.set(exchange.getRequestURI().getPath());
            return """
                    {"choices":[{"message":{"content":"ok"}}]}
                    """;
        })) {
            OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                    server.baseUrl("/compatible-mode/v1"),
                    "test-key",
                    5);

            JsonNode response = client.chatCompletion(
                    "qwen-plus",
                    List.of(Map.of("role", "user", "content", "hello")),
                    0.3,
                    128,
                    null);

            assertThat(response.path("choices")).hasSize(1);
            assertThat(observedPath.get()).isEqualTo("/compatible-mode/v1/chat/completions");
        }
    }

    @Test
    void appendsDefaultV1PathWhenBaseUrlHasNoVersionSegment() throws Exception {
        AtomicReference<String> observedPath = new AtomicReference<>();
        try (TestHttpServer server = TestHttpServer.json(exchange -> {
            observedPath.set(exchange.getRequestURI().getPath());
            return """
                    {"data":[{"id":"deepseek-chat"}]}
                    """;
        })) {
            OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                    server.baseUrl(""),
                    "test-key",
                    5);

            JsonNode response = client.listModels();

            assertThat(response.path("data")).hasSize(1);
            assertThat(observedPath.get()).isEqualTo("/v1/models");
        }
    }

    private interface ResponseHandler {
        String handle(HttpExchange exchange) throws IOException;
    }

    private static final class TestHttpServer implements AutoCloseable {
        private final HttpServer server;

        private TestHttpServer(HttpServer server) {
            this.server = server;
        }

        static TestHttpServer json(ResponseHandler handler) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                byte[] body = handler.handle(exchange).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(body);
                }
            });
            server.start();
            return new TestHttpServer(server);
        }

        String baseUrl(String suffix) {
            String normalizedSuffix = suffix == null ? "" : suffix;
            return "http://127.0.0.1:" + server.getAddress().getPort() + normalizedSuffix;
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
