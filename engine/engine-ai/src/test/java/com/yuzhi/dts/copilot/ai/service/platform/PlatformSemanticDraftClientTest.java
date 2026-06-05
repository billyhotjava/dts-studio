package com.yuzhi.dts.copilot.ai.service.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.yuzhi.dts.copilot.ai.service.copilot.SemanticDraftGovernanceSubmissionService;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlatformSemanticDraftClientTest {

    @Test
    void postsGovernanceDraftSubmissionWithPlatformServiceHeaders() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/governance/indicators", exchange -> {
            assertThat(exchange.getRequestMethod()).isEqualTo("POST");
            assertThat(exchange.getRequestHeaders().getFirst("X-DTS-Service")).isEqualTo("dts-copilot");
            assertThat(exchange.getRequestHeaders().getFirst("X-DTS-Service-Token")).isEqualTo("service-secret");
            JsonNode request = objectMapper.readTree(exchange.getRequestBody());
            assertThat(request.path("code").asText()).isEqualTo("finance.discounted_receivable");
            assertThat(request.path("status").asText()).isEqualTo("DRAFT");
            byte[] body = """
                    {"data":{"id":"platform-indicator-1","status":"DRAFT"}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();

        try {
            PlatformSemanticDraftClient client = new PlatformSemanticDraftClient(
                    new PlatformSemanticDraftProperties(
                            "http://127.0.0.1:" + server.getAddress().getPort(),
                            "",
                            "dts-copilot",
                            "service-secret",
                            2),
                    objectMapper);

            SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult result = client.submit(
                    new SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmission(
                            "semantic-draft-1",
                            "GOVERNANCE_INDICATOR",
                            "/api/governance/indicators",
                            Map.of(
                                    "code", "finance.discounted_receivable",
                                    "name", "折后应收",
                                    "status", "DRAFT")));

            assertThat(result.submitted()).isTrue();
            assertThat(result.platformId()).isEqualTo("platform-indicator-1");
            assertThat(result.platformStatus()).isEqualTo("DRAFT");
            assertThat(result.governanceStatus()).isEqualTo("DRAFT_SUBMITTED");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void includesPlatformErrorBodyWhenSubmissionFails() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/governance/indicators", exchange -> {
            byte[] body = """
                    {"detail":"域编码不存在: finance"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();

        try {
            PlatformSemanticDraftClient client = new PlatformSemanticDraftClient(
                    new PlatformSemanticDraftProperties(
                            "http://127.0.0.1:" + server.getAddress().getPort(),
                            "",
                            "dts-copilot",
                            "service-secret",
                            2),
                    objectMapper);

            assertThatThrownBy(() -> client.submit(new SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmission(
                    "semantic-draft-1",
                    "GOVERNANCE_INDICATOR",
                    "/api/governance/indicators",
                    Map.of("code", "finance.discounted_receivable"))))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HTTP 400")
                    .hasMessageContaining("域编码不存在: finance");
        } finally {
            server.stop(0);
        }
    }
}
