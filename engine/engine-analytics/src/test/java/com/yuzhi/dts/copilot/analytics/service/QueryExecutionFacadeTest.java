package com.yuzhi.dts.copilot.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.analytics.service.DatasetQueryService.DatasetConstraints;
import com.yuzhi.dts.copilot.analytics.service.DatasetQueryService.DatasetResult;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryExecutionFacadeTest {

    @Mock
    private DatasetQueryService datasetQueryService;

    @Mock
    private MbqlToSqlService mbqlToSqlService;

    @Mock
    private NativeQueryTemplateService nativeQueryTemplateService;

    @Mock
    private ScreenComplianceService screenComplianceService;

    @Captor
    private ArgumentCaptor<String> sqlCaptor;

    @Test
    void shouldRetryPostgresToDateDateArgumentErrorWithDateCast() throws Exception {
        QueryExecutionFacade facade = new QueryExecutionFacade(
                datasetQueryService, mbqlToSqlService, nativeQueryTemplateService, screenComplianceService);
        DatasetConstraints constraints = DatasetConstraints.defaults();
        QueryExecutionFacade.PreparedQuery prepared = new QueryExecutionFacade.PreparedQuery(
                2L,
                "native",
                """
                SELECT COUNT(*) AS "待处理单数"
                FROM public.xycyl_ads_flowerbiz_pending
                WHERE to_char(TO_DATE("申请时间", 'YYYY-MM-DD'), 'YYYY-MM') = '2026-03'
                """,
                List.of(),
                null,
                constraints);
        DatasetResult fixedResult = new DatasetResult(
                List.of(List.of(8L)),
                List.of(Map.of("name", "待处理单数", "display_name", "待处理单数", "base_type", "type/BigInteger")),
                List.of(),
                "Asia/Shanghai");
        SQLException postgresError = new SQLException(
                "ERROR: function to_date(date, unknown) does not exist\n"
                        + " Hint: No function matches the given name and argument types. You might need to add explicit type casts.");

        when(datasetQueryService.runNative(eq(2L), anyString(), eq(constraints), any()))
                .thenThrow(postgresError)
                .thenReturn(fixedResult);
        when(screenComplianceService.applyMasking(fixedResult)).thenReturn(fixedResult);

        List<QueryExecutionFacade.ExecutionAttempt> attempts = new ArrayList<>();
        DatasetResult result = facade.executeWithCompliance(prepared, attempts::add);

        assertThat(result).isSameAs(fixedResult);
        verify(datasetQueryService, times(2)).runNative(eq(2L), sqlCaptor.capture(), eq(constraints), any());
        assertThat(sqlCaptor.getAllValues().get(1))
                .contains("to_char(\"申请时间\"::date, 'YYYY-MM')")
                .doesNotContain("TO_DATE(\"申请时间\"");
        assertThat(attempts).hasSize(2);
        assertThat(attempts.get(0).retryPlanned()).isTrue();
        assertThat(attempts.get(0).rewrittenSql()).contains("\"申请时间\"::date");
        assertThat(attempts.get(1).success()).isTrue();
        assertThat(attempts.get(1).fromAutoFix()).isTrue();
    }

    @Test
    void shouldApplySameToDateAutoFixOnRawExecutionPath() throws Exception {
        QueryExecutionFacade facade = new QueryExecutionFacade(
                datasetQueryService, mbqlToSqlService, nativeQueryTemplateService, screenComplianceService);
        DatasetConstraints constraints = DatasetConstraints.defaults();
        QueryExecutionFacade.PreparedQuery prepared = new QueryExecutionFacade.PreparedQuery(
                8L,
                "native",
                """
                SELECT COUNT(*) AS "待处理单数"
                FROM public.xycyl_ads_flowerbiz_pending
                WHERE to_char(TO_DATE("申请时间", 'YYYY-MM-DD'), 'YYYY-MM') = '2026-03'
                """,
                List.of(),
                null,
                constraints);
        DatasetResult fixedResult = new DatasetResult(
                List.of(List.of(8L)),
                List.of(Map.of("name", "待处理单数", "display_name", "待处理单数", "base_type", "type/BigInteger")),
                List.of(),
                "Asia/Shanghai");
        SQLException postgresError = new SQLException(
                "ERROR: function to_date(date, unknown) does not exist\n"
                        + " Hint: No function matches the given name and argument types.");

        when(datasetQueryService.runNative(eq(8L), anyString(), eq(constraints), any()))
                .thenThrow(postgresError)
                .thenReturn(fixedResult);

        DatasetResult result = facade.executeRaw(prepared);

        assertThat(result).isSameAs(fixedResult);
        verify(datasetQueryService, times(2)).runNative(eq(8L), sqlCaptor.capture(), eq(constraints), any());
        assertThat(sqlCaptor.getAllValues().get(1))
                .contains("to_char(\"申请时间\"::date, 'YYYY-MM')")
                .doesNotContain("TO_DATE(\"申请时间\"");
    }
}
