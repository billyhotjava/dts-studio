package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.annotation.JsonAlias;
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
public class FinanceApplicationMysqlAuthorityRegistry {

    private static final Logger log = LoggerFactory.getLogger(FinanceApplicationMysqlAuthorityRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/finance-application-mysql-authority-sql.v1.json";
    private static final String LEGACY_REGISTRY_RESOURCE = "governance/finance-application-mysql-oracle-sql.v1.json";
    private static final Pattern UNSAFE_SQL = Pattern.compile(
            "\\b(insert|update|delete|drop|alter|truncate|merge|create|grant|revoke|call)\\b",
            Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final FinanceAuthorityRegistry financeAuthorityRegistry;
    private Map<String, AuthoritySqlCase> cases = Map.of();

    public FinanceApplicationMysqlAuthorityRegistry(
            ObjectMapper objectMapper,
            FinanceAuthorityRegistry financeAuthorityRegistry) {
        this.objectMapper = objectMapper;
        this.financeAuthorityRegistry = financeAuthorityRegistry;
    }

    @PostConstruct
    public void init() {
        String resource = REGISTRY_RESOURCE;
        InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
        if (is == null) {
            resource = LEGACY_REGISTRY_RESOURCE;
            is = getClass().getClassLoader().getResourceAsStream(resource);
        }
        if (is == null) {
            log.warn("Finance application MySQL authority SQL resource not found: {}", REGISTRY_RESOURCE);
            this.cases = Map.of();
            return;
        }

        try (InputStream closeable = is) {
            AuthoritySqlCaseDocument document = objectMapper.readValue(closeable, AuthoritySqlCaseDocument.class);
            Map<String, AuthoritySqlCase> loaded = new LinkedHashMap<>();
            for (AuthoritySqlCase authorityCase : document.cases()) {
                assertAlignedWithAuthorityRegistry(authorityCase);
                assertSafeApplicationMysqlAuthority(authorityCase);
                loaded.put(authorityCase.id(), authorityCase);
            }
            this.cases = java.util.Collections.unmodifiableMap(loaded);
            log.info("Loaded {} finance application MySQL authority SQL case(s) from {}", cases.size(), resource);
        } catch (Exception e) {
            log.warn("Failed to load finance application MySQL authority SQL cases from {}: {}", resource, e.getMessage());
            this.cases = Map.of();
        }
    }

    public List<AuthoritySqlCase> cases() {
        return new ArrayList<>(cases.values());
    }

    public Optional<AuthoritySqlCase> caseById(String id) {
        return Optional.ofNullable(cases.get(id));
    }

    private void assertAlignedWithAuthorityRegistry(AuthoritySqlCase authorityCase) {
        FinanceAuthorityRegistry.AuthorityBinding binding = financeAuthorityRegistry.binding(authorityCase.authorityBindingId())
                .orElseGet(() -> {
                    financeAuthorityRegistry.init();
                    return financeAuthorityRegistry.binding(authorityCase.authorityBindingId()).orElseThrow(
                            () -> new IllegalStateException("Missing finance authority binding: "
                                    + authorityCase.authorityBindingId()));
                });
        if (!binding.chain().equals(authorityCase.chain())) {
            throw new IllegalStateException("Application MySQL authority case chain is not aligned with authority binding: "
                    + authorityCase.id());
        }
    }

    private static void assertSafeApplicationMysqlAuthority(AuthoritySqlCase authorityCase) {
        AuthorityQuery query = authorityCase.applicationMysqlQuery();
        if (!"application-mysql-sql".equals(query.kind())) {
            throw new IllegalStateException("Application MySQL authority query kind is invalid: " + authorityCase.id());
        }
        String sql = query.nativeSql().toLowerCase(Locale.ROOT);
        String trimmed = sql.stripLeading();
        if (!(trimmed.startsWith("select ") || trimmed.startsWith("with "))) {
            throw new IllegalStateException("Application MySQL authority query must be read-only: " + authorityCase.id());
        }
        if (sql.contains(";") || UNSAFE_SQL.matcher(sql).find()) {
            throw new IllegalStateException("Application MySQL authority query contains unsafe SQL: " + authorityCase.id());
        }
        if (sql.contains("public.ods_") || sql.contains("mysql.rs_cloud_flower")
                || sql.contains("jdbc:mysql") || sql.contains("password")) {
            throw new IllegalStateException("Application MySQL authority query must target application tables only: "
                    + authorityCase.id());
        }
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private record AuthoritySqlCaseDocument(String version, List<AuthoritySqlCase> cases) {
        private AuthoritySqlCaseDocument {
            cases = copyOrEmpty(cases);
        }
    }

    public record AuthoritySqlCase(
            String id,
            @JsonAlias("oracleBindingId")
            String authorityBindingId,
            String chain,
            String metricId,
            String metricName,
            List<String> dimensionKeys,
            String copilotQuestion,
            AuthorityQuery copilotQuery,
            AuthorityQuery applicationMysqlQuery,
            String notes) {

        public AuthoritySqlCase {
            id = textOrEmpty(id);
            authorityBindingId = textOrEmpty(authorityBindingId);
            chain = textOrEmpty(chain);
            metricId = textOrEmpty(metricId);
            metricName = textOrEmpty(metricName);
            dimensionKeys = copyOrEmpty(dimensionKeys);
            copilotQuestion = textOrEmpty(copilotQuestion);
            copilotQuery = copilotQuery == null ? AuthorityQuery.empty() : copilotQuery;
            applicationMysqlQuery = applicationMysqlQuery == null ? AuthorityQuery.empty() : applicationMysqlQuery;
            notes = textOrEmpty(notes);
        }

        public FinanceSummaryDualReconciliationService.SummarySpec reconciliationSpec() {
            return new FinanceSummaryDualReconciliationService.SummarySpec(id, chain, metricId, dimensionKeys);
        }

        public String oracleBindingId() {
            return authorityBindingId;
        }
    }

    public record AuthorityQuery(String kind, String database, String nativeSql) {
        public AuthorityQuery {
            kind = textOrEmpty(kind);
            database = textOrEmpty(database);
            nativeSql = textOrEmpty(nativeSql);
        }

        public static AuthorityQuery empty() {
            return new AuthorityQuery("", "", "");
        }
    }
}
