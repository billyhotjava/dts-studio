package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FinanceDetailReconciliationSampleRegistry {

    private static final Logger log = LoggerFactory.getLogger(FinanceDetailReconciliationSampleRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/finance-detail-reconciliation-samples.v1.json";

    private final ObjectMapper objectMapper;
    private final FinanceAuthorityRegistry financeAuthorityRegistry;
    private Map<String, DetailSample> samples = Map.of();

    public FinanceDetailReconciliationSampleRegistry(
            ObjectMapper objectMapper,
            FinanceAuthorityRegistry financeAuthorityRegistry) {
        this.objectMapper = objectMapper;
        this.financeAuthorityRegistry = financeAuthorityRegistry;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(REGISTRY_RESOURCE)) {
            if (is == null) {
                log.warn("Finance detail reconciliation sample resource not found: {}", REGISTRY_RESOURCE);
                this.samples = Map.of();
                return;
            }
            DetailSampleDocument document = objectMapper.readValue(is, DetailSampleDocument.class);
            Map<String, DetailSample> loaded = new LinkedHashMap<>();
            for (DetailSample sample : document.samples()) {
                assertAlignedWithOracleRegistry(sample);
                loaded.put(sample.id(), sample);
            }
            this.samples = java.util.Collections.unmodifiableMap(loaded);
            log.info("Loaded {} finance detail reconciliation sample(s) from {}", samples.size(), REGISTRY_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load finance detail reconciliation samples from {}: {}", REGISTRY_RESOURCE, e.getMessage());
            this.samples = Map.of();
        }
    }

    public List<DetailSample> samples() {
        return new ArrayList<>(samples.values());
    }

    public Optional<DetailSample> sample(String id) {
        return Optional.ofNullable(samples.get(id));
    }

    private void assertAlignedWithOracleRegistry(DetailSample sample) {
        FinanceAuthorityRegistry.AuthorityBinding binding = financeAuthorityRegistry.binding(sample.authorityBindingId())
                .orElseGet(() -> {
                    financeAuthorityRegistry.init();
                    return financeAuthorityRegistry.binding(sample.authorityBindingId()).orElseThrow(
                            () -> new IllegalStateException("Missing finance authority binding: " + sample.authorityBindingId()));
                });
        if (!binding.chain().equals(sample.chain())) {
            throw new IllegalStateException("Detail reconciliation sample chain is not aligned with authority binding: " + sample.id());
        }
        boolean endpointRegistered = binding.endpoints().stream()
                .map(FinanceAuthorityRegistry.AuthorityEndpoint::signature)
                .anyMatch(sample.authorityEndpoint()::equals);
        if (!endpointRegistered) {
            throw new IllegalStateException("Detail reconciliation sample endpoint is not registered: " + sample.authorityEndpoint());
        }
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static <K, V> Map<K, V> copyMapOrEmpty(Map<K, V> values) {
        return values == null ? Map.of() : Map.copyOf(values);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private record DetailSampleDocument(String version, List<DetailSample> samples) {
        private DetailSampleDocument {
            samples = copyOrEmpty(samples);
        }
    }

    public record DetailSample(
            String id,
            @JsonAlias("authorityBindingId")
            String oracleBindingId,
            String chain,
            @JsonAlias("authorityEndpoint")
            String oracleEndpoint,
            String businessKey,
            String projectId,
            String accountPeriod,
            String copilotQuestion,
            List<String> amountFields,
            @JsonAlias("authorityRequest")
            Map<String, String> oracleRequest,
            Map<String, String> copilotRequest) {

        public DetailSample {
            id = textOrEmpty(id);
            oracleBindingId = textOrEmpty(oracleBindingId);
            chain = textOrEmpty(chain);
            oracleEndpoint = textOrEmpty(oracleEndpoint);
            businessKey = textOrEmpty(businessKey);
            projectId = textOrEmpty(projectId);
            accountPeriod = textOrEmpty(accountPeriod);
            copilotQuestion = textOrEmpty(copilotQuestion);
            amountFields = copyOrEmpty(amountFields);
            oracleRequest = copyMapOrEmpty(oracleRequest);
            copilotRequest = copyMapOrEmpty(copilotRequest);
        }

        public FinanceDetailReconciliationService.DetailReconciliationSpec reconciliationSpec() {
            return new FinanceDetailReconciliationService.DetailReconciliationSpec(
                    authorityBindingId(),
                    chain,
                    amountFields);
        }

        public String authorityBindingId() {
            return oracleBindingId;
        }

        public String authorityEndpoint() {
            return oracleEndpoint;
        }

        public Map<String, String> authorityRequest() {
            return oracleRequest;
        }
    }
}
