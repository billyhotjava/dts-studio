package com.yuzhi.dts.copilot.analytics.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsUser;
import com.yuzhi.dts.copilot.analytics.service.AnalyticsSessionService;
import com.yuzhi.dts.copilot.analytics.service.JdbcDetailsResolver;
import com.yuzhi.dts.copilot.analytics.service.MetadataSyncService;
import com.yuzhi.dts.copilot.analytics.service.PlatformInfraClient;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class DatabaseResourceTest {

    private static final SlowSuccessDriver SLOW_SUCCESS_DRIVER = new SlowSuccessDriver();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private AnalyticsSessionService sessionService;

    @Mock
    private com.yuzhi.dts.copilot.analytics.repository.AnalyticsDatabaseRepository databaseRepository;

    @Mock
    private com.yuzhi.dts.copilot.analytics.repository.AnalyticsTableRepository tableRepository;

    @Mock
    private com.yuzhi.dts.copilot.analytics.repository.AnalyticsFieldRepository fieldRepository;

    @Mock
    private MetadataSyncService metadataSyncService;

    @Mock
    private JdbcDetailsResolver jdbcDetailsResolver;

    @Mock
    private PlatformInfraClient platformInfraClient;

    @BeforeAll
    static void registerDriver() throws SQLException {
        DriverManager.registerDriver(SLOW_SUCCESS_DRIVER);
    }

    @AfterAll
    static void deregisterDriver() throws SQLException {
        DriverManager.deregisterDriver(SLOW_SUCCESS_DRIVER);
    }

    @Test
    void validateConnectionAllowsSlowButSuccessfulJdbcHandshake() throws Exception {
        DatabaseResource resource = new DatabaseResource(
                sessionService,
                databaseRepository,
                tableRepository,
                fieldRepository,
                metadataSyncService,
                jdbcDetailsResolver,
                platformInfraClient,
                MAPPER);

        AnalyticsUser user = new AnalyticsUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setPasswordHash("secret");
        user.setSuperuser(true);
        user.setActive(true);
        when(sessionService.resolveUser(any())).thenReturn(Optional.of(user));
        when(jdbcDetailsResolver.resolve(any(), any())).thenReturn(new JdbcDetailsResolver.JdbcDetails("jdbc:slow:test", null, null));

        MockHttpServletRequest request = new MockHttpServletRequest();
        DatabaseResource.DatabaseRequest body = new DatabaseResource.DatabaseRequest(
                "test-db",
                "mysql",
                MAPPER.readTree("""
                        {
                          "host": "db.weitaor.com",
                          "port": 3306,
                          "database": "rs_cloud_flower",
                          "username": "flowerai",
                          "password": "ai2026Flower@demo"
                        }
                        """),
                null,
                false,
                null,
                null,
                null,
                true,
                true,
                false);

        long startedAt = System.nanoTime();
        ResponseEntity<?> response = resource.validateConnection(body, request);
        long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Map.of());
        assertThat(elapsedMs).isGreaterThanOrEqualTo(6_000);
    }

    private static final class SlowSuccessDriver implements Driver {

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) {
                return null;
            }
            try {
                Thread.sleep(6_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted while simulating a slow JDBC handshake", e);
            }
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[] {Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "isValid" -> true;
                        case "isClosed" -> false;
                        case "close" -> null;
                        case "unwrap" -> null;
                        case "isWrapperFor" -> false;
                        default -> defaultValue(method.getReturnType());
                    });
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:slow:");
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }

        private Object defaultValue(Class<?> returnType) {
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == byte.class || returnType == short.class || returnType == int.class || returnType == long.class) {
                return 0;
            }
            if (returnType == float.class || returnType == double.class) {
                return 0.0;
            }
            if (returnType == char.class) {
                return '\0';
            }
            return null;
        }
    }
}
