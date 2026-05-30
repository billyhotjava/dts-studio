package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.Nl2SqlQueryTemplate;
import com.yuzhi.dts.copilot.ai.repository.Nl2SqlQueryTemplateRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentBiReportCatalogService {

    private static final List<ReportCatalogEntry> ENTRIES = List.of(
	            new ReportCatalogEntry(
	                    "prs.flowerbiz.overview",
	                    "flowerbiz",
	                    "FIXED_REPORT",
	                    "L2_FIXED_REPORT",
	                    "public.xycyl_ads_flowerbiz_overview",
	                    "MEDIUM",
	                    List.of("报花/租赁总览依赖 dbt ADS 与 screen JSON，客户关联维度需要结合明细核验。"),
	                    "table",
	                    List.of("报花总览", "租赁经营总览", "经营总览", "大屏", "看板"),
	                    List.of("fixed-report:PRS-FLOWERBIZ-OVERVIEW", "dbt-model:public.xycyl_ads_flowerbiz_overview", "semantic-pack:flowerbiz"),
	                    true),
	            new ReportCatalogEntry(
	                    "prs.flowerbiz.lease_execution_monthly",
	                    "flowerbiz",
	                    "FIXED_REPORT",
	                    "L2_FIXED_REPORT",
	                    "public.xycyl_ads_flowerbiz_lease_summary",
	                    "MEDIUM",
	                    List.of("默认时间窗口为 2025-05-01 至当前日期。", "客户关联存在缺口时，客户维度结果需要人工核验。"),
	                    "table",
	                    List.of("租赁收入", "租赁", "收入", "趋势", "按月", "月度", "每月", "加摆", "撤摆", "2025年5月", "2025-05"),
	                    List.of("fixed-report:PRS-FLOWERBIZ-LEASE-EXECUTION", "dbt-model:public.xycyl_ads_flowerbiz_lease_summary", "semantic-pack:flowerbiz"),
	                    true),
            new ReportCatalogEntry(
	                    "prs.flowerbiz.baddebt_rank",
	                    "flowerbiz",
	                    "FIXED_REPORT",
	                    "L2_FIXED_REPORT",
	                    "public.xycyl_ads_flowerbiz_baddebt_summary",
	                    "MEDIUM",
	                    List.of("坏账金额按报花业务口径归集，涉及客户/项目关联时需要核验源单。"),
	                    "table",
	                    List.of("坏账", "金额最高", "客户", "项目", "排名", "排行"),
	                    List.of("fixed-report:PRS-FLOWERBIZ-FINANCE-BADDEBT", "dbt-model:public.xycyl_ads_flowerbiz_baddebt_summary", "semantic-pack:flowerbiz"),
	                    true),
            new ReportCatalogEntry(
	                    "prs.flowerbiz.pending_approval",
	                    "workflow_audit",
	                    "FIXED_REPORT",
	                    "L2_FIXED_REPORT",
	                    "public.xycyl_ads_flowerbiz_pending",
	                    "MEDIUM",
	                    List.of("待审批状态取自报花链路，处理人和实时状态以 adminapi 明细为准。"),
	                    "table",
	                    List.of("待审批", "审核中", "在途", "滞留", "报花单", "按类型"),
	                    List.of("fixed-report:PRS-FLOWERBIZ-PENDING-STATUS", "dbt-model:public.xycyl_ads_flowerbiz_pending", "semantic-pack:flowerbiz"),
	                    true),
            new ReportCatalogEntry(
                    "prs.rental.pending_bill_detail",
                    "rental_receivable",
                    "BUSINESS_DETAIL",
                    "L0_ADMINAPI_READONLY",
                    "/rs-flowers-base/operate/monthAccount/listGreenAccountingPage",
                    "MEDIUM",
                    List.of("账单当前状态以 adminapi 只读接口为准，汇总口径不替代业务页面确认。"),
                    "table",
                    List.of("待确认账单", "待确认", "账单", "月账", "这个项目", "待开票", "待回款"),
                    List.of("adminapi:/rs-flowers-base/operate/monthAccount/listGreenAccountingPage"),
                    true),
            new ReportCatalogEntry(
                    "prs.rental.collection_followup_proposal",
                    "rental_receivable",
                    "ACTION_PROPOSAL",
                    "ACTION_PROPOSAL",
                    "rental.create_collection_followup",
                    "MEDIUM",
                    List.of("催收类动作必须先生成提案并由用户确认，不允许 Agent 直接写业务系统。"),
                    "table",
                    List.of("催收", "发起催收", "回款异常", "跟进", "待办", "任务"),
                    List.of("action:rental.create_collection_followup"),
                    false),
            new ReportCatalogEntry(
	                    "prs.project.customer_value",
	                    "project",
	                    "FIXED_REPORT",
	                    "L2_FIXED_REPORT",
	                    "public.xycyl_ads_flowerbiz_project_customer",
	                    "MEDIUM",
	                    List.of("客户贡献分析受客户关联完整度影响，需保留数据质量提示。"),
	                    "table",
	                    List.of("客户贡献", "客户", "项目", "排行", "排名", "价值", "top"),
	                    List.of("fixed-report:PRS-FLOWERBIZ-PROJECT-CUSTOMER-TOP", "dbt-model:public.xycyl_ads_flowerbiz_project_customer", "semantic-pack:flowerbiz"),
	                    true),
            new ReportCatalogEntry(
                    "prs.purchase.inventory_turnover",
                    "purchase_inventory",
                    "BUSINESS_INSIGHT",
                    "L0_ADMINAPI_READONLY",
                    "/rs-flowers-base/store/info/listStockInfo",
                    "MEDIUM",
                    List.of("采购库存第一阶段优先用业务只读接口，后续再沉淀 dbt 主题表。"),
                    "table",
                    List.of("采购", "库存", "周转", "低库存", "补货"),
                    List.of("adminapi:/rs-flowers-base/store/info/listStockInfo"),
                    true),
            new ReportCatalogEntry(
                    "prs.task.supervise_workload",
                    "task_supervise",
                    "BUSINESS_INSIGHT",
                    "L0_ADMINAPI_READONLY",
                    "/rs-flowers-base/tasknew/dailyTaskInfo/listDailyTaskInfoPage",
                    "MEDIUM",
                    List.of("任务和巡检执行状态以移动端/adminapi 当前状态为准。"),
                    "table",
                    List.of("任务", "养护", "巡检", "整改", "工作量"),
                    List.of("adminapi:/rs-flowers-base/tasknew/dailyTaskInfo/listDailyTaskInfoPage"),
                    true),
            new ReportCatalogEntry(
                    "prs.workflow.audit_backlog",
                    "workflow_audit",
                    "BUSINESS_INSIGHT",
                    "L0_ADMINAPI_READONLY",
                    "/workflow/process/todoList",
                    "HIGH",
                    List.of("流程待办和操作链路来自审批/日志系统，适合做过程审计。"),
                    "table",
                    List.of("审批", "流程", "待办", "操作日志", "审计"),
                    List.of("adminapi:/workflow/process/todoList"),
                    true)
    );

    private final Nl2SqlQueryTemplateRepository templateRepository;

    public AgentBiReportCatalogService() {
        this(null, null, null);
    }

    @Autowired
    public AgentBiReportCatalogService(
            SemanticPackService semanticPackService,
            Nl2SqlQueryTemplateRepository templateRepository,
            ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
    }

    public List<ReportCatalogEntry> entries() {
        return enrichWithRuntimeAssets(ENTRIES);
    }

    public Optional<ReportCatalogEntry> findBestMatch(String userQuestion, String routedDomain) {
        if (!StringUtils.hasText(userQuestion)) {
            return Optional.empty();
        }
        String normalized = normalize(userQuestion);
        List<ScoredEntry> scoredEntries = new ArrayList<>();
        for (ReportCatalogEntry entry : entries()) {
            int score = 0;
            int keywordScore = 0;
            for (String keyword : entry.keywords()) {
                if (StringUtils.hasText(keyword) && normalized.contains(normalize(keyword))) {
                    int point = keyword.length() >= 4 ? 2 : 1;
                    keywordScore += point;
                    score += point;
                }
            }
            if (keywordScore == 0) {
                continue;
            }
            if (StringUtils.hasText(routedDomain) && entry.matchesDomain(routedDomain)) {
                score += 2;
            }
            if (score > 0) {
                scoredEntries.add(new ScoredEntry(entry, score));
            }
        }
        return scoredEntries.stream()
                .max(Comparator.comparingInt(ScoredEntry::score))
                .map(ScoredEntry::entry);
    }

    private List<ReportCatalogEntry> enrichWithRuntimeAssets(List<ReportCatalogEntry> baseEntries) {
        List<Nl2SqlQueryTemplate> templates = findActiveTemplates();
        if (templates.isEmpty()) {
            return baseEntries;
        }
        return baseEntries.stream()
                .map(entry -> enrichWithTemplates(entry, templates))
                .toList();
    }

    private List<Nl2SqlQueryTemplate> findActiveTemplates() {
        if (templateRepository == null) {
            return List.of();
        }
        try {
            return templateRepository.findByIsActiveTrueOrderByPriorityDesc();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private ReportCatalogEntry enrichWithTemplates(
            ReportCatalogEntry entry,
            List<Nl2SqlQueryTemplate> templates) {
        LinkedHashSet<String> sourceRefs = new LinkedHashSet<>(entry.sourceRefs());
        for (Nl2SqlQueryTemplate template : templates) {
            if (matchesTemplate(entry, template) && StringUtils.hasText(template.getTemplateCode())) {
                sourceRefs.add("query-template:" + template.getTemplateCode().trim());
            }
        }
        return entry.withSourceRefs(List.copyOf(sourceRefs));
    }

    private boolean matchesTemplate(ReportCatalogEntry entry, Nl2SqlQueryTemplate template) {
        if (!entry.matchesDomain(template.getDomain())) {
            return false;
        }
        String target = normalize(entry.primaryTarget());
        if (!StringUtils.hasText(target)) {
            return false;
        }
        String templateTarget = normalize(template.getTargetView());
        if (target.equals(templateTarget)) {
            return true;
        }
        return normalize(template.getSqlTemplate()).contains(target);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record ScoredEntry(ReportCatalogEntry entry, int score) {}

    public record ReportCatalogEntry(
            String reportCode,
            String domain,
            String responseKind,
            String dataSurface,
            String primaryTarget,
            String qualityLevel,
            List<String> qualityNotes,
            String defaultDisplay,
            List<String> keywords,
            List<String> sourceRefs,
            boolean businessActionAllowed
    ) {
        public ReportCatalogEntry {
            qualityNotes = qualityNotes == null ? List.of() : List.copyOf(qualityNotes);
            keywords = keywords == null ? List.of() : List.copyOf(keywords);
            sourceRefs = sourceRefs == null ? List.of() : List.copyOf(sourceRefs);
        }

        private ReportCatalogEntry withSourceRefs(List<String> sourceRefs) {
            return new ReportCatalogEntry(
                    reportCode,
                    domain,
                    responseKind,
                    dataSurface,
                    primaryTarget,
                    qualityLevel,
                    qualityNotes,
                    defaultDisplay,
                    keywords,
                    sourceRefs,
                    businessActionAllowed);
        }

        private boolean matchesDomain(String routedDomain) {
            if (!StringUtils.hasText(routedDomain)) {
                return false;
            }
            String normalized = normalize(routedDomain);
            if (domain.equals(normalized)) {
                return true;
            }
            return switch (normalized) {
                case "settlement", "finance", "财务" -> "rental_receivable".equals(domain);
                case "flower", "flowerbiz", "curing", "pendulum" -> "flowerbiz".equals(domain);
                case "purchase", "procurement", "inventory", "stock", "warehouse" -> "purchase_inventory".equals(domain);
                case "task", "supervise" -> "task_supervise".equals(domain);
                case "workflow", "audit" -> "workflow_audit".equals(domain);
                default -> false;
            };
        }
    }
}
