package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FinanceApplicationMysqlOracleRegistry {

    private static final Logger log = LoggerFactory.getLogger(FinanceApplicationMysqlOracleRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/finance-application-mysql-oracle-sql.v1.json";
    private static final Pattern UNSAFE_SQL = Pattern.compile(
            "\\b(insert|update|delete|drop|alter|truncate|merge|create|grant|revoke|call)\\b",
            Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final FinanceOracleRegistry financeOracleRegistry;
    private Map<String, OracleSqlCase> cases = Map.of();

    public FinanceApplicationMysqlOracleRegistry(
            ObjectMapper objectMapper,
            FinanceOracleRegistry financeOracleRegistry) {
        this.objectMapper = objectMapper;
        this.financeOracleRegistry = financeOracleRegistry;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(REGISTRY_RESOURCE)) {
            if (is == null) {
                log.warn("Finance application MySQL oracle SQL resource not found: {}", REGISTRY_RESOURCE);
                this.cases = Map.of();
                return;
            }
            OracleSqlCaseDocument document = objectMapper.readValue(is, OracleSqlCaseDocument.class);
            Map<String, OracleSqlCase> loaded = new LinkedHashMap<>();
            for (OracleSqlCase oracleCase : document.cases()) {
                assertAlignedWithOracleRegistry(oracleCase);
                assertSafeApplicationMysqlOracle(oracleCase);
                loaded.put(oracleCase.id(), oracleCase);
            }
            this.cases = java.util.Collections.unmodifiableMap(loaded);
            log.info("Loaded {} finance application MySQL oracle SQL case(s) from {}", cases.size(), REGISTRY_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load finance application MySQL oracle SQL cases from {}: {}", REGISTRY_RESOURCE, e.getMessage());
            this.cases = Map.of();
        }
    }

    public List<OracleSqlCase> cases() {
        return new ArrayList<>(cases.values());
    }

    public Optional<OracleSqlCase> caseById(String id) {
        return Optional.ofNullable(cases.get(id));
    }

    private void assertAlignedWithOracleRegistry(OracleSqlCase oracleCase) {
        FinanceOracleRegistry.OracleBinding binding = financeOracleRegistry.binding(oracleCase.oracleBindingId())
                .orElseGet(() -> {
                    financeOracleRegistry.init();
                    return financeOracleRegistry.binding(oracleCase.oracleBindingId()).orElseThrow(
                            () -> new IllegalStateException("Missing finance oracle binding: " + oracleCase.oracleBindingId()));
                });
        if (!binding.chain().equals(oracleCase.chain())) {
            throw new IllegalStateException("Application MySQL oracle case chain is not aligned with oracle binding: "
                    + oracleCase.id());
        }
    }

    private static void assertSafeApplicationMysqlOracle(OracleSqlCase oracleCase) {
        OracleQuery query = oracleCase.applicationMysqlQuery();
        if (!"application-mysql-sql".equals(query.kind())) {
            throw new IllegalStateException("Application MySQL oracle query kind is invalid: " + oracleCase.id());
        }
        String sql = query.nativeSql().toLowerCase(Locale.ROOT);
        String trimmed = sql.stripLeading();
        if (!(trimmed.startsWith("select ") || trimmed.startsWith("with "))) {
            throw new IllegalStateException("Application MySQL oracle query must be read-only: " + oracleCase.id());
        }
        if (sql.contains(";") || UNSAFE_SQL.matcher(sql).find()) {
            throw new IllegalStateException("Application MySQL oracle query contains unsafe SQL: " + oracleCase.id());
        }
        if (sql.contains("public.ods_") || sql.contains("mysql.rs_cloud_flower")
                || sql.contains("jdbc:mysql") || sql.contains("password")) {
            throw new IllegalStateException("Application MySQL oracle query must target application tables only: "
                    + oracleCase.id());
        }
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private record OracleSqlCaseDocument(String version, List<OracleSqlCase> cases) {
        private OracleSqlCaseDocument {
            cases = copyOrEmpty(cases);
        }
    }

    public record OracleSqlCase(
            String id,
            String oracleBindingId,
            String chain,
            String metricId,
            String metricName,
            List<String> dimensionKeys,
            String copilotQuestion,
            OracleQuery copilotQuery,
            OracleQuery applicationMysqlQuery,
            String notes) {

        public OracleSqlCase {
            id = textOrEmpty(id);
            oracleBindingId = textOrEmpty(oracleBindingId);
            chain = textOrEmpty(chain);
            metricId = textOrEmpty(metricId);
            metricName = textOrEmpty(metricName);
            dimensionKeys = copyOrEmpty(dimensionKeys);
            copilotQuestion = textOrEmpty(copilotQuestion);
            copilotQuery = copilotQuery == null ? OracleQuery.empty() : copilotQuery;
            applicationMysqlQuery = applicationMysqlQuery == null ? OracleQuery.empty() : applicationMysqlQuery;
            notes = textOrEmpty(notes);
        }

        public FinanceSummaryDualReconciliationService.SummarySpec reconciliationSpec() {
            return new FinanceSummaryDualReconciliationService.SummarySpec(id, chain, metricId, dimensionKeys);
        }
    }

    public record OracleQuery(String kind, String database, String nativeSql) {
        public OracleQuery {
            kind = textOrEmpty(kind);
            database = textOrEmpty(database);
            nativeSql = textOrEmpty(nativeSql);
        }

        public static OracleQuery empty() {
            return new OracleQuery("", "", "");
        }
    }
}
