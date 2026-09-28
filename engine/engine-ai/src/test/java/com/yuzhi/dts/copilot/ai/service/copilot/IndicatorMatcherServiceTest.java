package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.ai.service.platform.IndicatorCatalogEntry;
import com.yuzhi.dts.copilot.ai.service.platform.IndicatorCatalogStore;
import com.yuzhi.dts.copilot.ai.service.platform.SyncResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndicatorMatcherServiceTest {

    private IndicatorCatalogStore store;
    private SemanticPackService semanticPackService;
    private IndicatorMatcherService matcher;

    @BeforeEach
    void setUp() {
        store = new IndicatorCatalogStore();
        semanticPackService = mock(SemanticPackService.class);
        matcher = new IndicatorMatcherService(store, semanticPackService);
    }

    @Test
    void exactNameHitReturnsHighConfidencePublishedIndicator() {
        store.replaceAll(List.of(entry("cash-in", "现金流入", "finance", "已发布", "v1",
                        List.of("资金"), List.of("month_id"), "统计现金流入")),
                SyncResult.success(1, 1, 0, 0, List.of()));

        IndicatorMatcherService.IndicatorMatchResult result = matcher.match("本月现金流入是多少");

        assertThat(result.tier()).isEqualTo(IndicatorMatcherService.Confidence.HIGH);
        assertThat(result.candidates()).hasSize(1);
        assertThat(result.candidates().getFirst().code()).isEqualTo("cash-in");
        assertThat(result.candidates().getFirst().matchedSignals()).contains("name:现金流入");
        assertThat(result.candidates().getFirst().confidence()).isGreaterThanOrEqualTo(0.8d);
    }

    @Test
    void sortsMultipleCandidatesByConfidence() {
        store.replaceAll(List.of(
                        entry("cash-in", "现金流入", "finance", "已发布", "v1",
                                List.of("资金", "回款"), List.of("month_id"), "统计已确认回款金额"),
                        entry("cash-balance", "现金余额", "finance", "已发布", "v1",
                                List.of("资金"), List.of("month_id"), "余额快照")),
                SyncResult.success(2, 2, 0, 0, List.of()));

        IndicatorMatcherService.IndicatorMatchResult result = matcher.match("本月回款资金按月统计");

        assertThat(result.tier()).isEqualTo(IndicatorMatcherService.Confidence.MEDIUM);
        assertThat(result.candidates()).extracting(IndicatorMatcherService.IndicatorMatch::code)
                .containsExactly("cash-in", "cash-balance");
        assertThat(result.candidates().getFirst().confidence())
                .isGreaterThan(result.candidates().get(1).confidence());
    }

    @Test
    void ignoresDraftOrOfflineIndicatorsEvenIfTheyMatch() {
        store.replaceAll(List.of(
                        entry("draft-cash", "现金流入", "finance", "草稿", "v1",
                                List.of("资金"), List.of("month_id"), "统计现金流入")),
                SyncResult.success(1, 1, 0, 0, List.of()));

        IndicatorMatcherService.IndicatorMatchResult result = matcher.match("现金流入");

        assertThat(result.tier()).isEqualTo(IndicatorMatcherService.Confidence.NONE);
        assertThat(result.candidates()).isEmpty();
    }

    @Test
    void returnsNoneWhenCatalogIsEmpty() {
        IndicatorMatcherService.IndicatorMatchResult result = matcher.match("现金流入");

        assertThat(result.tier()).isEqualTo(IndicatorMatcherService.Confidence.NONE);
        assertThat(result.candidates()).isEmpty();
    }

    @Test
    void expandsSemanticPackSynonymsBeforeMatching() {
        when(semanticPackService.getSynonyms("finance")).thenReturn(Map.of("回款", "现金流入"));
        store.replaceAll(List.of(entry("cash-in", "现金流入", "finance", "已发布", "v1",
                        List.of("资金"), List.of("month_id"), "统计已确认金额")),
                SyncResult.success(1, 1, 0, 0, List.of()));

        IndicatorMatcherService.IndicatorMatchResult result = matcher.match("本月回款是多少");

        assertThat(result.tier()).isEqualTo(IndicatorMatcherService.Confidence.HIGH);
        assertThat(result.candidates()).extracting(IndicatorMatcherService.IndicatorMatch::code)
                .containsExactly("cash-in");
        assertThat(result.candidates().getFirst().matchedSignals()).contains("synonym:回款->现金流入");
    }

    private static IndicatorCatalogEntry entry(
            String code,
            String name,
            String domain,
            String status,
            String version,
            List<String> tags,
            List<String> dimensions,
            String definition) {
        return new IndicatorCatalogEntry(
                "id-" + code,
                code,
                name,
                domain,
                domain,
                definition,
                "sum(amount)",
                status,
                version,
                tags,
                dimensions,
                "biz_date",
                "month",
                "SUM",
                "amount",
                "MEDIUM",
                "owner",
                List.of(name, code));
    }
}
