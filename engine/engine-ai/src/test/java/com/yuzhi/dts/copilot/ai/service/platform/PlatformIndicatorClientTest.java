package com.yuzhi.dts.copilot.ai.service.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PlatformIndicatorClientTest {

    @Test
    void listPublishedIndicatorsUsesPlatformServiceHeadersWhenConfigured() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/governance/indicators", exchange -> {
            assertThat(exchange.getRequestURI().getRawQuery()).contains("status=PUBLISHED");
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isNull();
            assertThat(exchange.getRequestHeaders().getFirst("X-DTS-Service")).isEqualTo("dts-copilot");
            assertThat(exchange.getRequestHeaders().getFirst("X-DTS-Service-Token")).isEqualTo("service-secret");
            assertThat(exchange.getRequestHeaders().getFirst("X-Active-Dept")).isNull();
            byte[] body = """
                    {"data":{"content":[
                      {
                        "id":"cash-in",
                        "code":"cash_in",
                        "name":"回款金额",
                        "status":"已发布",
                        "version":"v2"
                      }
                    ],"page":0,"size":10,"totalPages":1}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();

        try {
            PlatformIndicatorClient client = new PlatformIndicatorClient(
                    new PlatformIndicatorProperties(
                            "http://127.0.0.1:" + server.getAddress().getPort(),
                            "",
                            "",
                            "",
                            "ignored-bearer",
                            "dts-copilot",
                            "service-secret",
                            "",
                            2),
                    new ObjectMapper());

            PlatformIndicatorPage page = client.listPublishedIndicators(0, 10);

            assertThat(page.items()).hasSize(1);
            assertThat(page.items().getFirst().name()).isEqualTo("回款金额");
            assertThat(page.totalPages()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void listPublishedIndicatorsSendsActiveDeptWhenConfigured() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/governance/indicators", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("X-Active-Dept")).isEqualTo("1502");
            byte[] body = """
                    {"data":{"content":[],"page":0,"size":10,"totalPages":1}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();

        try {
            PlatformIndicatorClient client = new PlatformIndicatorClient(
                    new PlatformIndicatorProperties(
                            "http://127.0.0.1:" + server.getAddress().getPort(),
                            "",
                            "",
                            "",
                            "",
                            "dts-copilot",
                            "service-secret",
                            "1502",
                            2),
                    new ObjectMapper());

            PlatformIndicatorPage page = client.listPublishedIndicators(0, 10);

            assertThat(page.items()).isEmpty();
        } finally {
            server.stop(0);
        }
    }
}
