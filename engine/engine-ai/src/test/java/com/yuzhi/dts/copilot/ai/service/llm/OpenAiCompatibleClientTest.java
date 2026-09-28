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
import java.util.concurrent.CopyOnWriteArrayList;

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

    @Test
    void streamsReasoningAndContentDeltasSeparately() throws Exception {
        try (TestHttpServer server = TestHttpServer.sse(exchange -> """
                data: {"choices":[{"delta":{"reasoning_content":"先判断用户意图"},"finish_reason":null}]}

                data: {"choices":[{"delta":{"content":"最终答案"},"finish_reason":null}]}

                data: {"choices":[{"delta":{},"finish_reason":"stop"}]}

                data: [DONE]

                """)) {
            OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                    server.baseUrl("/v1"),
                    "test-key",
                    5);
            CopyOnWriteArrayList<String> reasoningDeltas = new CopyOnWriteArrayList<>();
            CopyOnWriteArrayList<String> contentDeltas = new CopyOnWriteArrayList<>();

            OpenAiCompatibleClient.StreamingChatResult result = client.chatCompletionStream(
                    "deepseek-reasoner",
                    List.of(Map.of("role", "user", "content", "你好")),
                    0.3,
                    128,
                    null,
                    new OpenAiCompatibleClient.StreamEventHandler() {
                        @Override
                        public void onReasoningDelta(String delta) {
                            reasoningDeltas.add(delta);
                        }

                        @Override
                        public void onContentDelta(String delta) {
                            contentDeltas.add(delta);
                        }
                    });

            assertThat(reasoningDeltas).containsExactly("先判断用户意图");
            assertThat(contentDeltas).containsExactly("最终答案");
            assertThat(result.reasoningContent()).isEqualTo("先判断用户意图");
            assertThat(result.content()).isEqualTo("最终答案");
            assertThat(result.toolCalls()).isEmpty();
            assertThat(result.finishReason()).isEqualTo("stop");
        }
    }

    @Test
    void streamsAndAccumulatesToolCallsAcrossChunks() throws Exception {
        try (TestHttpServer server = TestHttpServer.sse(exchange -> """
                data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_1","type":"function","function":{"name":"schema_lookup","arguments":"{\\"table\\":\\"" }}]},"finish_reason":null}]}

                data: {"choices":[{"delta":{"tool_calls":[{"index":0,"function":{"arguments":"project\\"}"}}]},"finish_reason":null}]}

                data: {"choices":[{"delta":{},"finish_reason":"tool_calls"}]}

                data: [DONE]

                """)) {
            OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                    server.baseUrl("/v1"),
                    "test-key",
                    5);

            OpenAiCompatibleClient.StreamingChatResult result = client.chatCompletionStream(
                    "deepseek-chat",
                    List.of(Map.of("role", "user", "content", "查表")),
                    0.3,
                    128,
                    List.of(Map.of("type", "function", "function", Map.of("name", "schema_lookup"))),
                    OpenAiCompatibleClient.StreamEventHandler.noop());

            assertThat(result.toolCalls()).hasSize(1);
            assertThat(result.toolCalls().getFirst().get("id")).isEqualTo("call_1");
            assertThat(result.toolCalls().getFirst().get("function"))
                    .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                    .containsEntry("name", "schema_lookup")
                    .containsEntry("arguments", "{\"table\":\"project\"}");
            assertThat(result.finishReason()).isEqualTo("tool_calls");
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

        static TestHttpServer sse(ResponseHandler handler) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                byte[] body = handler.handle(exchange).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
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
