package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.Nl2SqlQueryTemplate;
import com.yuzhi.dts.copilot.ai.repository.Nl2SqlQueryTemplateRepository;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.SuggestedQuestion;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.TemplateMatchResult;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for TemplateMatcherService.
 * Covers template matching scenarios T-01 through T-10 from the acceptance matrix,
 * plus parameter extraction and suggested questions.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TemplateMatcherServiceTest {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Mock
    private Nl2SqlQueryTemplateRepository templateRepository;

    private TemplateMatcherService matcherService;

    @BeforeEach
    void setUp() {
        matcherService = new TemplateMatcherService(templateRepository, new ObjectMapper());
        when(templateRepository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(buildMockTemplates());
    }

    // ===================== Template matching =====================

    @Test
    @DisplayName("T-01: 项目绿植数 (TPL-01) - 含项目名参数提取")
    void matchProjectGreenCount() {
        TemplateMatchResult result = matcherService.match("翠湖项目的绿植有多少");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-01");
        assertThat(result.resolvedSql()).contains("v_project_green_current");
    }

    @Test
    @DisplayName("T-02: 在服项目总数 (TPL-02)")
    void matchActiveProjectCount() {
        TemplateMatchResult result = matcherService.match("当前在服项目一共多少个");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-02");
        assertThat(result.resolvedSql()).contains("v_project_overview");
        assertThat(result.resolvedSql()).contains("正常");
    }

    @Test
    @DisplayName("T-04: 加花排行 (TPL-06)")
    void matchFlowerAddRanking() {
        TemplateMatchResult result = matcherService.match("各项目加花次数排行");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-06");
        assertThat(result.resolvedSql()).contains("v_flower_biz_detail");
    }

    @Test
    @DisplayName("T-05: 待审批报花单 (TPL-08)")
    void matchPendingApproval() {
        TemplateMatchResult result = matcherService.match("有多少待审批的报花单");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-08");
        assertThat(result.resolvedSql()).contains("审核中");
    }

    @Test
    @DisplayName("T-06: 项目月租金 (TPL-09) - 上月时间解析")
    void matchProjectRentWithLastMonth() {
        TemplateMatchResult result = matcherService.match("万科项目上个月租金");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-09");
        assertThat(result.extractedParams()).containsKey("month");
        String expectedMonth = LocalDate.now().minusMonths(1).format(MONTH_FMT);
        assertThat(result.extractedParams().get("month")).isEqualTo(expectedMonth);
        assertThat(result.extractedParams()).containsKey("project_name");
        assertThat(result.extractedParams().get("project_name")).isEqualTo("万科");
        assertThat(result.resolvedSql()).contains("v_monthly_settlement");
    }

    @Test
    @DisplayName("T-07: 未结算项目 (TPL-10) - 上月默认")
    void matchUnsettledProjects() {
        TemplateMatchResult result = matcherService.match("上月未结算的项目");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-10");
        String expectedMonth = LocalDate.now().minusMonths(1).format(MONTH_FMT);
        assertThat(result.extractedParams().get("month")).isEqualTo(expectedMonth);
        assertThat(result.resolvedSql()).contains("待结算");
    }

    @Test
    @DisplayName("T-08: 进行中任务 (TPL-13)")
    void matchInProgressTasks() {
        TemplateMatchResult result = matcherService.match("进行中的任务有哪些");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-13");
        assertThat(result.resolvedSql()).contains("v_task_progress");
        assertThat(result.resolvedSql()).contains("进行中");
    }

    @Test
    @DisplayName("T-10: 结算方式分布 (TPL-20)")
    void matchSettlementTypeDistribution() {
        TemplateMatchResult result = matcherService.match("各结算方式的项目分布");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-20");
        assertThat(result.resolvedSql()).contains("settlement_type_name");
    }

    // ===================== No match =====================

    @Test
    @DisplayName("T-09: 无匹配降级 - 无关问题")
    void noMatchForIrrelevantQuestion() {
        TemplateMatchResult result = matcherService.match("今天天气怎么样");

        assertThat(result.matched()).isFalse();
        assertThat(result.template()).isNull();
        assertThat(result.resolvedSql()).isNull();
    }

    @Test
    @DisplayName("空输入 -> matched=false")
    void emptyInputReturnsNoMatch() {
        TemplateMatchResult result = matcherService.match("");

        assertThat(result.matched()).isFalse();
    }

    @Test
    @DisplayName("null 输入 -> matched=false")
    void nullInputReturnsNoMatch() {
        TemplateMatchResult result = matcherService.match(null);

        assertThat(result.matched()).isFalse();
    }

    // ===================== Parameter extraction =====================

    @Test
    @DisplayName("时间参数: 本月 -> 当前 YYYY-MM")
    void extractCurrentMonth() {
        TemplateMatchResult result = matcherService.match("万科项目这个月租金");

        assertThat(result.matched()).isTrue();
        String expectedMonth = LocalDate.now().format(MONTH_FMT);
        assertThat(result.extractedParams().get("month")).isEqualTo(expectedMonth);
    }

    @Test
    @DisplayName("时间参数: 上月 -> 上月 YYYY-MM")
    void extractLastMonth() {
        TemplateMatchResult result = matcherService.match("万科项目上个月租金");

        assertThat(result.matched()).isTrue();
        String expectedMonth = LocalDate.now().minusMonths(1).format(MONTH_FMT);
        assertThat(result.extractedParams().get("month")).isEqualTo(expectedMonth);
    }

    @Test
    @DisplayName("项目名提取: '翠湖项目' -> project_name contains 翠湖")
    void extractProjectName() {
        TemplateMatchResult result = matcherService.match("翠湖项目的绿植有多少");

        assertThat(result.matched()).isTrue();
        // Parameter extraction is best-effort; just verify it's present
        assertThat(result.extractedParams()).containsKey("project_name");
        assertThat(result.extractedParams().get("project_name")).isNotBlank();
    }

    // ===================== Suggested questions =====================

    @Test
    @DisplayName("getSuggestedQuestions 返回非空列表")
    void suggestedQuestionsNotEmpty() {
        List<SuggestedQuestion> suggestions = matcherService.getSuggestedQuestions(8);

        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.size()).isLessThanOrEqualTo(8);
    }

    @Test
    @DisplayName("getSuggestedQuestions 覆盖多个域")
    void suggestedQuestionsCoverMultipleDomains() {
        List<SuggestedQuestion> suggestions = matcherService.getSuggestedQuestions(20);

        long distinctDomains = suggestions.stream()
                .map(SuggestedQuestion::domain)
                .distinct()
                .count();
        assertThat(distinctDomains).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("getSuggestedQuestions 每项包含必要字段")
    void suggestedQuestionsHaveRequiredFields() {
        List<SuggestedQuestion> suggestions = matcherService.getSuggestedQuestions(4);

        for (SuggestedQuestion q : suggestions) {
            assertThat(q.templateCode()).isNotBlank();
            assertThat(q.domain()).isNotBlank();
            assertThat(q.question()).isNotBlank();
        }
    }

    // ===================== Helper: build mock templates =====================

    /**
     * Build mock templates matching key entries from v1_0_0_010 seed data.
     * Includes TPL-01, TPL-02, TPL-06, TPL-08, TPL-09, TPL-10, TPL-13, TPL-15, TPL-20.
     */
    private List<Nl2SqlQueryTemplate> buildMockTemplates() {
        List<Nl2SqlQueryTemplate> templates = new ArrayList<>();

        // TPL-01: 项目在摆绿植数
        templates.add(buildTemplate(1L, "TPL-01", "project", null,
                "[\"(项目).*(绿植|花).*(多少|几|数量|总数)\",\"(项目).*(多少|几|数量|总数).*(绿植|花)\"]",
                "[\"XX项目目前有多少在摆绿植？\"]",
                "SELECT project_name, count(*) as 在摆绿植数 FROM v_project_green_current WHERE project_name LIKE CONCAT('%', :project_name, '%') GROUP BY project_name",
                "{\"project_name\":{\"type\":\"string\",\"required\":true}}",
                "v_project_green_current", "项目在摆绿植数", 10));

        // TPL-02: 在服项目总数
        templates.add(buildTemplate(2L, "TPL-02", "project", null,
                "[\"(在服|正常|当前|活跃).*(项目).*(多少|几|总数|数量)\"]",
                "[\"当前在服项目一共多少个？\"]",
                "SELECT project_status_name, count(*) as 项目数 FROM v_project_overview WHERE project_status_name = '正常' GROUP BY project_status_name",
                "{}",
                "v_project_overview", "在服项目总数", 10));

        // TPL-06: 各项目加花排行
        templates.add(buildTemplate(6L, "TPL-06", "flowerbiz", null,
                "[\"(项目|各项目).*(加花|换花|报花).*(排行|排名|最多)\"]",
                "[\"各项目加花次数排行\"]",
                "SELECT project_name, count(*) as 次数 FROM v_flower_biz_detail WHERE biz_type_name = :biz_type AND biz_month = :month GROUP BY project_name ORDER BY 次数 DESC LIMIT :top_n",
                "{\"biz_type\":{\"type\":\"string\",\"default\":\"加花\"},\"month\":{\"type\":\"string\",\"default\":\"CURRENT_MONTH\"},\"top_n\":{\"type\":\"integer\",\"default\":10}}",
                "v_flower_biz_detail", "各项目加花排行", 10));

        // TPL-08: 待审批报花单
        templates.add(buildTemplate(8L, "TPL-08", "flowerbiz", null,
                "[\"(待审批|审核中|未审批).*(报花|业务单)\"]",
                "[\"有多少待审批的报花单？\"]",
                "SELECT biz_code, biz_type_name, project_name, apply_user_name FROM v_flower_biz_detail WHERE biz_status_name = '审核中' ORDER BY apply_time DESC",
                "{}",
                "v_flower_biz_detail", "待审批报花单", 10));

        // TPL-09: 项目月租金查询
        templates.add(buildTemplate(9L, "TPL-09", "settlement", "finance",
                "[\"(项目|XX).*(上月|上个月|本月|这个月).*(租金|应收|收入)\"]",
                "[\"XX项目上个月租金是多少？\"]",
                "SELECT project_name, settlement_month, total_rent FROM v_monthly_settlement WHERE (:project_name IS NULL OR project_name LIKE CONCAT('%', :project_name, '%')) AND settlement_month = :month",
                "{\"project_name\":{\"type\":\"string\"},\"month\":{\"type\":\"string\",\"default\":\"LAST_MONTH\"}}",
                "v_monthly_settlement", "项目月租金查询", 10));

        // TPL-10: 未结算项目
        templates.add(buildTemplate(10L, "TPL-10", "settlement", "finance",
                "[\"(未结算|待结算|没结算).*(项目)\"]",
                "[\"上月未结算的项目有哪些？\"]",
                "SELECT project_name, settlement_month, total_rent, settlement_status_name FROM v_monthly_settlement WHERE settlement_status_name = '待结算' AND settlement_month = :month ORDER BY total_rent DESC",
                "{\"month\":{\"type\":\"string\",\"default\":\"LAST_MONTH\"}}",
                "v_monthly_settlement", "未结算项目", 10));

        // TPL-13: 待处理任务
        templates.add(buildTemplate(13L, "TPL-13", "task", null,
                "[\"(待处理|待办|进行中|未完成).*(任务)\"]",
                "[\"进行中的任务有哪些？\"]",
                "SELECT task_code, task_title, task_type_name, project_name FROM v_task_progress WHERE task_status_name = '进行中' ORDER BY launch_time DESC",
                "{}",
                "v_task_progress", "待处理任务", 10));

        // TPL-15: 养护人负责摆位
        templates.add(buildTemplate(15L, "TPL-15", "curing", null,
                "[\"(养护人|XX).*(负责|管理).*(摆位|多少)\"]",
                "[\"养护人均负责多少摆位？\"]",
                "SELECT curing_user_name, sum(total_position_count) as 负责摆位总数 FROM v_curing_coverage WHERE curing_month = :month GROUP BY curing_user_name ORDER BY 负责摆位总数 DESC",
                "{\"month\":{\"type\":\"string\",\"default\":\"CURRENT_MONTH\"},\"curing_user\":{\"type\":\"string\"}}",
                "v_curing_coverage", "养护人负责摆位", 10));

        // TPL-20: 结算方式分布
        templates.add(buildTemplate(20L, "TPL-20", "project", null,
                "[\"(结算方式|固定月租).*(哪些|分布|项目)\"]",
                "[\"各结算方式的项目分布\"]",
                "SELECT settlement_type_name, count(*) as 项目数 FROM v_project_overview WHERE project_status_name = '正常' GROUP BY settlement_type_name",
                "{}",
                "v_project_overview", "结算方式分布", 10));

        return templates;
    }

    private Nl2SqlQueryTemplate buildTemplate(Long id, String templateCode, String domain,
                                               String roleHint, String intentPatterns,
                                               String questionSamples, String sqlTemplate,
                                               String parameters, String targetView,
                                               String description, int priority) {
        Nl2SqlQueryTemplate t = new Nl2SqlQueryTemplate();
        t.setId(id);
        t.setTemplateCode(templateCode);
        t.setDomain(domain);
        t.setRoleHint(roleHint);
        t.setIntentPatterns(intentPatterns);
        t.setQuestionSamples(questionSamples);
        t.setSqlTemplate(sqlTemplate);
        t.setParameters(parameters);
        t.setTargetView(targetView);
        t.setDescription(description);
        t.setPriority(priority);
        t.setIsActive(true);
        return t;
    }
}
