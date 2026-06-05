package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FinanceDetailReconciliationHarness {

    private final FinanceDetailReconciliationSampleRegistry sampleRegistry;
    private final FinanceDetailReconciliationService reconciliationService;

    public FinanceDetailReconciliationHarness(
            FinanceDetailReconciliationSampleRegistry sampleRegistry,
            FinanceDetailReconciliationService reconciliationService) {
        this.sampleRegistry = sampleRegistry;
        this.reconciliationService = reconciliationService;
    }

    public HarnessReport run(String sampleId, DetailSourceClient sourceClient) {
        if (sourceClient == null) {
            return HarnessReport.failed(sampleId, "Detail reconciliation source client is required.");
        }
        return sampleRegistry.sample(sampleId)
                .map(sample -> runSample(sample, sourceClient))
                .orElseGet(() -> HarnessReport.failed(sampleId, "Detail reconciliation sample not found: " + safeText(sampleId)));
    }

    private HarnessReport runSample(
            FinanceDetailReconciliationSampleRegistry.DetailSample sample,
            DetailSourceClient sourceClient) {
        List<FinanceDetailReconciliationService.DetailRow> oracleRows = sourceClient.fetchOracleRows(sample);
        List<FinanceDetailReconciliationService.DetailRow> copilotRows = sourceClient.fetchCopilotRows(sample);
        FinanceDetailReconciliationService.DetailReconciliationReport reconciliation =
                reconciliationService.reconcile(sample.reconciliationSpec(), copilotRows, oracleRows);
        if (reconciliation.passed()) {
            return new HarnessReport(sample.id(), true, reconciliation, "");
        }
        return new HarnessReport(
                sample.id(),
                false,
                reconciliation,
                "Detail reconciliation sample failed: sampleId=" + sample.id()
                        + ", oracleEndpoint=" + sample.oracleEndpoint()
                        + ", reason=" + reconciliation.failureMessage());
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    public interface DetailSourceClient {
        List<FinanceDetailReconciliationService.DetailRow> fetchOracleRows(
                FinanceDetailReconciliationSampleRegistry.DetailSample sample);

        List<FinanceDetailReconciliationService.DetailRow> fetchCopilotRows(
                FinanceDetailReconciliationSampleRegistry.DetailSample sample);
    }

    public record HarnessReport(
            String sampleId,
            boolean passed,
            FinanceDetailReconciliationService.DetailReconciliationReport reconciliation,
            String failureMessage) {

        public HarnessReport {
            sampleId = safeText(sampleId);
            reconciliation = reconciliation == null
                    ? new FinanceDetailReconciliationService.DetailReconciliationReport(false, List.of(), "")
                    : reconciliation;
            failureMessage = safeText(failureMessage);
        }

        public static HarnessReport failed(String sampleId, String failureMessage) {
            return new HarnessReport(
                    sampleId,
                    false,
                    new FinanceDetailReconciliationService.DetailReconciliationReport(false, List.of(), failureMessage),
                    failureMessage);
        }
    }
}
