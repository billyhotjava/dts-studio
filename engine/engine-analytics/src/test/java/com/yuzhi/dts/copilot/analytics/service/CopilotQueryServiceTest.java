package com.yuzhi.dts.copilot.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for CopilotQueryService.
 * Covers SQL source validation (view whitelist) and finance restriction detection.
 */
@ExtendWith(MockitoExtension.class)
class CopilotQueryServiceTest {

    @Mock
    private DatasetQueryService datasetQueryService;

    private CopilotQueryService service;

    @BeforeEach
    void setUp() {
        service = new CopilotQueryService(datasetQueryService);
    }

    // ===================== Allowed view queries =====================

    @Test
    @DisplayName("P-01: 白名单视图 v_project_overview 通过")
    void allowedViewProjectOverviewPasses() {
        assertDoesNotThrow(() -> service.validateSqlSources(
                "SELECT project_name, count(*) FROM v_project_overview GROUP BY project_name"));
    }

    @Test
    @DisplayName("白名单视图 v_flower_biz_detail 通过")
    void allowedViewFlowerBizDetailPasses() {
        assertDoesNotThrow(() -> service.validateSqlSources(
                "SELECT biz_type_name, count(*) FROM v_flower_biz_detail GROUP BY biz_type_name"));
    }

    @Test
    @DisplayName("白名单视图 v_project_green_current 通过")
    void allowedViewGreenCurrentPasses() {
        assertDoesNotThrow(() -> service.validateSqlSources(
                "SELECT * FROM v_project_green_current WHERE project_name = '翠湖'"));
    }

    @Test
    @DisplayName("白名单视图 v_monthly_settlement 通过")
    void allowedViewMonthlySettlementPasses() {
        assertDoesNotThrow(() -> service.validateSqlSources(
                "SELECT project_name, total_rent FROM v_monthly_settlement"));
    }

    @Test
    @DisplayName("白名单视图 v_task_progress 通过")
    void allowedViewTaskProgressPasses() {
        assertDoesNotThrow(() -> service.validateSqlSources(
                "SELECT * FROM v_task_progress WHERE task_status_name = '进行中'"));
    }

    @Test
    @DisplayName("白名单视图 v_curing_coverage 通过")
    void allowedViewCuringCoveragePasses() {
        assertDoesNotThrow(() -> service.validateSqlSources(
                "SELECT * FROM v_curing_coverage"));
    }

    @Test
    @DisplayName("白名单视图 v_pendulum_progress 通过")
    void allowedViewPendulumProgressPasses() {
        assertDoesNotThrow(() -> service.validateSqlSources(
                "SELECT * FROM v_pendulum_progress"));
    }

    // ===================== Blocked raw table queries =====================

    @Test
    @DisplayName("P-02: 原始表 p_project 被拦截")
    void rawTableProjectBlocked() {
        ForbiddenQueryException ex = assertThrows(ForbiddenQueryException.class,
                () -> service.validateSqlSources("SELECT * FROM p_project WHERE status = 1"));

        assertThat(ex.getMessage()).contains("p_project");
    }

    @Test
    @DisplayName("P-03: 原始表 t_flower_biz_info 被拦截")
    void rawTableFlowerBizInfoBlocked() {
        ForbiddenQueryException ex = assertThrows(ForbiddenQueryException.class,
                () -> service.validateSqlSources("SELECT * FROM t_flower_biz_info"));

        assertThat(ex.getMessage()).contains("t_flower_biz_info");
    }

    @Test
    @DisplayName("原始表 sys_user 被拦截")
    void rawTableSysUserBlocked() {
        assertThrows(ForbiddenQueryException.class,
                () -> service.validateSqlSources("SELECT * FROM sys_user"));
    }

    // ===================== Subquery aliases =====================

    @Test
    @DisplayName("P-05: 子查询短别名 (po, fb) 不被误拦截")
    void subqueryAliasesNotBlocked() {
        assertDoesNotThrow(() -> service.validateSqlSources(
                "SELECT po.project_name, fb.biz_type_name " +
                "FROM v_project_overview po " +
                "JOIN v_flower_biz_detail fb ON po.project_name = fb.project_name"));
    }

    @Test
    @DisplayName("单字母别名 t 不被误拦截")
    void singleLetterAliasNotBlocked() {
        assertDoesNotThrow(() -> service.validateSqlSources(
                "SELECT t.task_title FROM v_task_progress t WHERE t.task_status_name = '进行中'"));
    }

    // ===================== Multiple JOINs =====================

    @Test
    @DisplayName("P-06: 多个白名单视图 JOIN 通过")
    void multipleAllowedViewJoinPasses() {
        assertDoesNotThrow(() -> service.validateSqlSources(
                "SELECT a.project_name, b.total_rent " +
                "FROM v_project_overview a " +
                "JOIN v_monthly_settlement b ON a.project_name = b.project_name"));
    }

    @Test
    @DisplayName("P-07: JOIN 含非法表失败")
    void joinWithDisallowedTableFails() {
        assertThrows(ForbiddenQueryException.class,
                () -> service.validateSqlSources(
                        "SELECT * FROM v_project_overview JOIN p_project ON v_project_overview.id = p_project.id"));
    }

    @Test
    @DisplayName("LEFT JOIN 含非法表失败")
    void leftJoinWithDisallowedTableFails() {
        assertThrows(ForbiddenQueryException.class,
                () -> service.validateSqlSources(
                        "SELECT * FROM v_project_overview LEFT JOIN t_customer ON 1=1"));
    }

    // ===================== Edge cases =====================

    @Test
    @DisplayName("P-08: 空 SQL 抛出异常")
    void emptySqlThrowsException() {
        assertThrows(ForbiddenQueryException.class,
                () -> service.validateSqlSources(""));
    }

    @Test
    @DisplayName("P-09: null SQL 抛出异常")
    void nullSqlThrowsException() {
        assertThrows(ForbiddenQueryException.class,
                () -> service.validateSqlSources(null));
    }

    @Test
    @DisplayName("纯空白 SQL 抛出异常")
    void blankSqlThrowsException() {
        assertThrows(ForbiddenQueryException.class,
                () -> service.validateSqlSources("   "));
    }

    // ===================== Finance restriction detection =====================

    @Test
    @DisplayName("P-10: v_monthly_settlement 识别为财务受限")
    void monthlySettlementIsFinanceRestricted() {
        boolean restricted = service.isFinanceRestricted(
                "SELECT * FROM v_monthly_settlement WHERE settlement_month = '2026-02'");

        assertThat(restricted).isTrue();
    }

    @Test
    @DisplayName("P-11: 非结算视图不受财务限制")
    void nonSettlementViewNotFinanceRestricted() {
        boolean restricted = service.isFinanceRestricted(
                "SELECT * FROM v_task_progress");

        assertThat(restricted).isFalse();
    }

    @Test
    @DisplayName("多视图 JOIN 含 v_monthly_settlement 为财务受限")
    void joinWithSettlementIsFinanceRestricted() {
        boolean restricted = service.isFinanceRestricted(
                "SELECT * FROM v_project_overview JOIN v_monthly_settlement ON 1=1");

        assertThat(restricted).isTrue();
    }

    @Test
    @DisplayName("isFinanceRestricted: null SQL 返回 false")
    void nullSqlNotFinanceRestricted() {
        assertThat(service.isFinanceRestricted(null)).isFalse();
    }

    @Test
    @DisplayName("isFinanceRestricted: 空 SQL 返回 false")
    void emptySqlNotFinanceRestricted() {
        assertThat(service.isFinanceRestricted("")).isFalse();
    }
}
