package com.yuzhi.dts.copilot.ai.service.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AnthropicCompatibleClientTest {

    @Test
    void postsMessagesRequestAndNormalizesTextResponse() throws Exception {
        AtomicReference<String> observedPath = new AtomicReference<>();
        AtomicReference<String> observedBody = new AtomicReference<>();
        try (TestHttpServer server = TestHttpServer.json(exchange -> {
            observedPath.set(exchange.getRequestURI().getPath());
            observedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            return """
                    {
                      "id":"msg_1",
                      "type":"message",
                      "role":"assistant",
                      "content":[
                        {"type":"thinking","thinking":"先分析问题"},
                        {"type":"text","text":"最终答案"}
                      ],
                      "stop_reason":"end_turn"
                    }
                    """;
        })) {
            AnthropicCompatibleClient client = new AnthropicCompatibleClient(
                    server.baseUrl("/anthropic"),
                    "test-key",
                    5);

            JsonNode response = client.chatCompletion(
                    "MiniMax-M2.7",
                    List.of(
                            Map.of("role", "system", "content", "你是分析助手"),
                            Map.of("role", "user", "content", "你好")),
                    0.3,
                    128,
                    null);

            assertThat(observedPath.get()).isEqualTo("/anthropic/v1/messages");
            assertThat(observedBody.get()).contains("\"system\":\"你是分析助手\"");
            assertThat(observedBody.get()).contains("\"messages\"");
            assertThat(response.at("/choices/0/message/content").asText()).isEqualTo("最终答案");
            assertThat(response.at("/choices/0/message/reasoning_content").asText()).isEqualTo("先分析问题");
            assertThat(response.at("/choices/0/finish_reason").asText()).isEqualTo("stop");
        }
    }

    @Test
    void normalizesToolUseResponseIntoOpenAiToolCalls() throws Exception {
        try (TestHttpServer server = TestHttpServer.json(exchange -> """
                {
                  "id":"msg_2",
                  "type":"message",
                  "role":"assistant",
                  "content":[
                    {"type":"tool_use","id":"toolu_1","name":"schema_lookup","input":{"table":"project"}}
                  ],
                  "stop_reason":"tool_use"
                }
                """)) {
            AnthropicCompatibleClient client = new AnthropicCompatibleClient(
                    server.baseUrl("/anthropic"),
                    "test-key",
                    5);

            JsonNode response = client.chatCompletion(
                    "MiniMax-M2.7",
                    List.of(Map.of("role", "user", "content", "查 project 表")),
                    0.3,
                    128,
                    List.of(Map.of("type", "function", "function", Map.of("name", "schema_lookup"))));

            assertThat(response.at("/choices/0/message/tool_calls")).hasSize(1);
            assertThat(response.at("/choices/0/message/tool_calls/0/id").asText()).isEqualTo("toolu_1");
            assertThat(response.at("/choices/0/message/tool_calls/0/function/name").asText()).isEqualTo("schema_lookup");
            assertThat(response.at("/choices/0/message/tool_calls/0/function/arguments").asText())
                    .isEqualTo("{\"table\":\"project\"}");
            assertThat(response.at("/choices/0/finish_reason").asText()).isEqualTo("tool_calls");
        }
    }

    @Test
    void streamsThinkingTextAndToolUseDeltas() throws Exception {
        try (TestHttpServer server = TestHttpServer.sse(exchange -> """
                event: content_block_start
                data: {"type":"content_block_start","index":0,"content_block":{"type":"thinking"}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"先判断意图"}}

                event: content_block_start
                data: {"type":"content_block_start","index":1,"content_block":{"type":"tool_use","id":"toolu_1","name":"schema_lookup"}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":1,"delta":{"type":"input_json_delta","partial_json":"{\\"table\\":\\"project\\"}"}}

                event: message_delta
                data: {"type":"message_delta","delta":{"stop_reason":"tool_use"}}

                event: message_stop
                data: {"type":"message_stop"}

                """)) {
            AnthropicCompatibleClient client = new AnthropicCompatibleClient(
                    server.baseUrl("/anthropic"),
                    "test-key",
                    5);
            CopyOnWriteArrayList<String> reasoningDeltas = new CopyOnWriteArrayList<>();
            CopyOnWriteArrayList<String> contentDeltas = new CopyOnWriteArrayList<>();

            OpenAiCompatibleClient.StreamingChatResult result = client.chatCompletionStream(
                    "MiniMax-M2.7",
                    List.of(Map.of("role", "user", "content", "查 project 表")),
                    0.3,
                    128,
                    List.of(Map.of("type", "function", "function", Map.of("name", "schema_lookup"))),
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

            assertThat(reasoningDeltas).containsExactly("先判断意图");
            assertThat(contentDeltas).isEmpty();
            assertThat(result.reasoningContent()).isEqualTo("先判断意图");
            assertThat(result.toolCalls()).hasSize(1);
            assertThat(result.toolCalls().getFirst().get("id")).isEqualTo("toolu_1");
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
            return "http://127.0.0.1:" + server.getAddress().getPort() + suffix;
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
