package com.yuzhi.dts.copilot.ai.service.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.yuzhi.dts.copilot.ai.config.PlatformDbtFreshnessConfiguration;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PlatformDbtFreshnessClientTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-07T04:00:00Z"), ZoneOffset.UTC);
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PlatformDbtFreshnessConfiguration.class)
            .withBean(ObjectMapper.class)
            .withBean(PlatformDbtFreshnessClient.class)
            .withPropertyValues(
                    "copilot.platform.dbt-freshness.enabled=false",
                    "copilot.platform.dbt-freshness.base-url=");

    @Test
    void registersAsDbtFreshnessResolverBean() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(PlatformDbtFreshnessClient.class)
                .hasSingleBean(DbtFreshnessResolver.class));
    }

    @Test
    void resolveFreshnessUsesPlatformDiagnosticsAndServiceHeaders() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/etl/dbt/models/xycyl_ads_finance_voucher_monthly/diagnostics", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isNull();
            assertThat(exchange.getRequestHeaders().getFirst("X-DTS-Service")).isEqualTo("dts-copilot");
            assertThat(exchange.getRequestHeaders().getFirst("X-DTS-Service-Token")).isEqualTo("service-secret");
            assertThat(exchange.getRequestHeaders().getFirst("X-Active-Dept")).isEqualTo("1502");
            byte[] body = """
                    {"data":{
                      "enabled":true,
                      "success":true,
                      "model":"xycyl_ads_finance_voucher_monthly",
                      "relationName":"public.xycyl_ads_finance_voucher_monthly",
                      "current":{"exists":true,"rowCount":12},
                      "runtime":{"dbtRun":{"present":true,"status":"SUCCESS","generatedAt":"2026-06-07T03:30:00Z"}}
                    }}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();

        try {
            PlatformDbtFreshnessClient client = new PlatformDbtFreshnessClient(
                    properties(server, 24),
                    new ObjectMapper(),
                    FIXED_CLOCK);

            Map<String, String> freshness = client.resolveFreshness(
                    List.of("public.xycyl_ads_finance_voucher_monthly"));

            assertThat(freshness)
                    .containsEntry("public.xycyl_ads_finance_voucher_monthly", "FRESH");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolveFreshnessMarksMissingWhenCurrentRelationDoesNotExist() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/etl/dbt/models/xycyl_ads_finance_voucher_monthly/diagnostics", exchange -> {
            byte[] body = """
                    {"data":{
                      "enabled":true,
                      "success":true,
                      "model":"xycyl_ads_finance_voucher_monthly",
                      "current":{"exists":false,"rowCount":null},
                      "runtime":{"dbtRun":{"present":true,"status":"SUCCESS","generatedAt":"2026-06-07T03:30:00Z"}}
                    }}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();

        try {
            PlatformDbtFreshnessClient client = new PlatformDbtFreshnessClient(
                    properties(server, 24),
                    new ObjectMapper(),
                    FIXED_CLOCK);

            Map<String, String> freshness = client.resolveFreshness(
                    List.of("public.xycyl_ads_finance_voucher_monthly"));

            assertThat(freshness)
                    .containsEntry("public.xycyl_ads_finance_voucher_monthly", "MISSING");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolveFreshnessMarksStaleWhenLatestSuccessfulRunIsTooOld() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/etl/dbt/models/xycyl_ads_finance_voucher_monthly/diagnostics", exchange -> {
            byte[] body = """
                    {"data":{
                      "enabled":true,
                      "success":true,
                      "model":"xycyl_ads_finance_voucher_monthly",
                      "current":{"exists":true,"rowCount":12},
                      "runtime":{"dbtRun":{"present":true,"status":"SUCCESS","generatedAt":"2026-06-05T03:30:00Z"}}
                    }}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();

        try {
            PlatformDbtFreshnessClient client = new PlatformDbtFreshnessClient(
                    properties(server, 24),
                    new ObjectMapper(),
                    FIXED_CLOCK);

            Map<String, String> freshness = client.resolveFreshness(
                    List.of("public.xycyl_ads_finance_voucher_monthly"));

            assertThat(freshness)
                    .containsEntry("public.xycyl_ads_finance_voucher_monthly", "STALE");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolveFreshnessReturnsEmptyWhenDisabled() {
        PlatformDbtFreshnessClient client = new PlatformDbtFreshnessClient(
                new PlatformDbtFreshnessProperties(
                        false,
                        "http://127.0.0.1:9",
                        "",
                        "dts-copilot",
                        "service-secret",
                        "",
                        2,
                        24),
                new ObjectMapper(),
                FIXED_CLOCK);

        assertThat(client.resolveFreshness(List.of("public.xycyl_ads_finance_voucher_monthly"))).isEmpty();
    }

    private static PlatformDbtFreshnessProperties properties(HttpServer server, int staleAfterHours) {
        return new PlatformDbtFreshnessProperties(
                true,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "",
                "dts-copilot",
                "service-secret",
                "1502",
                2,
                staleAfterHours);
    }
}
