package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.junit.jupiter.api.Test;

class FinanceDetailReconciliationHttpPayloadProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withUserConfiguration(FinanceDetailReconciliationHttpPayloadProvider.class)
            .withPropertyValues(
                    "copilot.finance.reconciliation.oracle-base-url=http://adminapi.example",
                    "copilot.finance.reconciliation.analytics-base-url=http://analytics.example",
                    "copilot.finance.reconciliation.authorization=test-token",
                    "copilot.finance.reconciliation.cookie=portal_session=abc");

    @Test
    void shouldBeSpringConstructableAsPayloadProvider() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(FinanceDetailReconciliationJsonSourceClient.PayloadProvider.class));
    }

    @Test
    void shouldCallOraclePostEndpointAndCopilotDatasetEndpointWithConfiguredHeaders() throws Exception {
        AtomicReference<String> oracleMethod = new AtomicReference<>();
        AtomicReference<String> oracleAuth = new AtomicReference<>();
        AtomicReference<String> oracleCookie = new AtomicReference<>();
        AtomicReference<String> oracleBody = new AtomicReference<>();
        AtomicReference<String> copilotBody = new AtomicReference<>();

        HttpServer adminapi = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        adminapi.createContext("/rs-flowers-base/operate/monthAccount/getMonthSettlementData", exchange -> {
            oracleMethod.set(exchange.getRequestMethod());
            oracleAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            oracleCookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            oracleBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = """
                    {"code":200,"msg":"ok","data":{"businessKey":"结算2026060008","projectId":"1001","yearAndMonth":"202606",
                    "receivableTotalAmount":1128.00,"netReceiptTotalAmount":1128.00,"foldingAfterTotalAmount":1128.00,"totalAmount":1128.00}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        adminapi.start();

        HttpServer analytics = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        analytics.createContext("/api/dataset", exchange -> {
            assertThat(exchange.getRequestMethod()).isEqualTo("POST");
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-token");
            assertThat(exchange.getRequestHeaders().getFirst("Cookie")).isEqualTo("portal_session=abc");
            copilotBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = """
                    {"status":"completed","data":{"rows":[["结算2026060008","1001","202606",1128.00,1128.00,1128.00,1128.00]],
                    "cols":[{"name":"businessKey"},{"name":"projectId"},{"name":"accountPeriod"},{"name":"receivableTotalAmount"},
                    {"name":"netReceiptTotalAmount"},{"name":"foldingAfterTotalAmount"},{"name":"totalAmount"}]}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        analytics.start();

        try {
            FinanceDetailReconciliationHttpPayloadProvider provider =
                    new FinanceDetailReconciliationHttpPayloadProvider(
                            objectMapper,
                            "http://127.0.0.1:" + adminapi.getAddress().getPort(),
                            "http://127.0.0.1:" + analytics.getAddress().getPort(),
                            "test-token",
                            "portal_session=abc",
                            2);

            String oraclePayload = provider.oraclePayload(monthSampleWithNativeSql());
            String copilotPayload = provider.copilotPayload(monthSampleWithNativeSql());

            assertThat(oracleMethod.get()).isEqualTo("POST");
            assertThat(oracleAuth.get()).isEqualTo("Bearer test-token");
            assertThat(oracleCookie.get()).isEqualTo("portal_session=abc");
            assertThat(oracleBody.get()).contains("\"projectId\":\"1001\"", "\"yearAndMonth\":\"202606\"");
            assertThat(copilotBody.get())
                    .contains("\"database\":7")
                    .contains("\"type\":\"native\"")
                    .contains("\"query\":\"select * from finance_detail where business_key = '结算2026060008'\"");
            assertThat(oraclePayload).contains("\"code\":200");
            assertThat(copilotPayload).contains("\"status\":\"completed\"");
        } finally {
            adminapi.stop(0);
            analytics.stop(0);
        }
    }

    @Test
    void shouldSupportSeparateOracleAndAnalyticsCredentialsForLiveReconciliation() throws Exception {
        AtomicReference<String> oracleAuth = new AtomicReference<>();
        AtomicReference<String> oracleCookie = new AtomicReference<>();
        AtomicReference<String> analyticsAuth = new AtomicReference<>();
        AtomicReference<String> analyticsCookie = new AtomicReference<>();

        HttpServer adminapi = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        adminapi.createContext("/rs-flowers-base/operate/monthAccount/getMonthSettlementData", exchange -> {
            oracleAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            oracleCookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            byte[] body = """
                    {"code":200,"data":{"businessKey":"结算2026060008","projectId":"1001","yearAndMonth":"202606",
                    "receivableTotalAmount":1128.00,"netReceiptTotalAmount":1128.00,"foldingAfterTotalAmount":1128.00,"totalAmount":1128.00}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        adminapi.start();

        HttpServer analytics = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        analytics.createContext("/api/dataset", exchange -> {
            analyticsAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            analyticsCookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            byte[] body = """
                    {"status":"completed","data":{"rows":[],"cols":[]}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        analytics.start();

        try {
            FinanceDetailReconciliationHttpPayloadProvider provider =
                    new FinanceDetailReconciliationHttpPayloadProvider(
                            objectMapper,
                            "http://127.0.0.1:" + adminapi.getAddress().getPort(),
                            "http://127.0.0.1:" + analytics.getAddress().getPort(),
                            "oracle-token",
                            "portal_session=oracle",
                            "analytics-token",
                            "api_session=analytics",
                            2);

            provider.oraclePayload(monthSampleWithNativeSql());
            provider.copilotPayload(monthSampleWithNativeSql());

            assertThat(oracleAuth.get()).isEqualTo("Bearer oracle-token");
            assertThat(oracleCookie.get()).isEqualTo("portal_session=oracle");
            assertThat(analyticsAuth.get()).isEqualTo("Bearer analytics-token");
            assertThat(analyticsCookie.get()).isEqualTo("api_session=analytics");
        } finally {
            adminapi.stop(0);
            analytics.stop(0);
        }
    }

    @Test
    void shouldCallOracleGetEndpointWithSampleRequestAsQueryString() throws Exception {
        AtomicReference<String> query = new AtomicReference<>();
        HttpServer adminapi = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        adminapi.createContext("/rs-flowers-base/operate/saleAccount/listSaleAccountPage", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] body = """
                    {"rows":[{"bizCode":"BX202606030968","projectId":"1001","accountPriod":"202606",
                    "receivableAmount":3451.68,"netReceiptsAmount":3451.68,"bizAmount":3451.68}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        adminapi.start();

        try {
            FinanceDetailReconciliationHttpPayloadProvider provider =
                    new FinanceDetailReconciliationHttpPayloadProvider(
                            objectMapper,
                            "http://127.0.0.1:" + adminapi.getAddress().getPort(),
                            "http://127.0.0.1:1",
                            "",
                            "",
                            2);

            String payload = provider.oraclePayload(saleSample());

            assertThat(query.get()).contains("projectId=1001", "bizCode=BX202606030968");
            assertThat(payload).contains("\"rows\"");
        } finally {
            adminapi.stop(0);
        }
    }

    @Test
    void shouldExplainLegacyOracleRouteRequirementWhenOracleEndpointRejectsRequest() throws Exception {
        HttpServer dtsAdminLikeServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        dtsAdminLikeServer.createContext("/rs-flowers-base/operate/saleAccount/listSaleAccountPage", exchange -> {
            byte[] body = """
                    {"error":"insufficient_scope"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(403, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        dtsAdminLikeServer.start();

        try {
            FinanceDetailReconciliationHttpPayloadProvider provider =
                    new FinanceDetailReconciliationHttpPayloadProvider(
                            objectMapper,
                            "http://127.0.0.1:" + dtsAdminLikeServer.getAddress().getPort(),
                            "http://127.0.0.1:1",
                            "admin-session-token",
                            "",
                            2);

            assertThatThrownBy(() -> provider.oraclePayload(saleSample()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("legacy adminapi")
                    .hasMessageContaining("rs-gateway")
                    .hasMessageContaining("rs-flowers-base")
                    .hasMessageContaining("oracle-base-url")
                    .hasMessageContaining("/flowers-dev-api");
        } finally {
            dtsAdminLikeServer.stop(0);
        }
    }

    @Test
    void shouldRejectHttpFetchWhenBaseUrlsAreMissing() {
        FinanceDetailReconciliationHttpPayloadProvider provider =
                new FinanceDetailReconciliationHttpPayloadProvider(objectMapper, "", "", "", "", 2);

        assertThatThrownBy(() -> provider.oraclePayload(monthSampleWithNativeSql()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("oracle-base-url");
        assertThatThrownBy(() -> provider.copilotPayload(monthSampleWithNativeSql()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("analytics-base-url");
    }

    private static FinanceDetailReconciliationSampleRegistry.DetailSample monthSampleWithNativeSql() {
        return new FinanceDetailReconciliationSampleRegistry.DetailSample(
                "month-settlement-js2026060008",
                "month-settlement",
                "rent-settlement",
                "POST /rs-flowers-base/operate/monthAccount/getMonthSettlementData",
                "结算2026060008",
                "1001",
                "202606",
                "查询 202606 账期 1001 项目 结算2026060008 的月对账三级金额明细",
                List.of("receivableTotalAmount", "netReceiptTotalAmount", "foldingAfterTotalAmount", "totalAmount"),
                Map.of("projectId", "1001", "yearAndMonth", "202606", "businessKey", "结算2026060008"),
                Map.of(
                        "database", "7",
                        "nativeSql", "select * from finance_detail where business_key = '结算2026060008'"));
    }

    private static FinanceDetailReconciliationSampleRegistry.DetailSample saleSample() {
        return new FinanceDetailReconciliationSampleRegistry.DetailSample(
                "sale-account-bx202606030968",
                "sale-account",
                "sale-gift-bad-debt",
                "GET /rs-flowers-base/operate/saleAccount/listSaleAccountPage",
                "BX202606030968",
                "1001",
                "202606",
                "查询 202606 账期 1001 项目 BX202606030968 的售账应收实收明细",
                List.of("receivableAmount", "netReceiptsAmount", "bizAmount"),
                Map.of("projectId", "1001", "bizCode", "BX202606030968"),
                Map.of("database", "7", "nativeSql", "select * from sale_detail"));
    }
}
