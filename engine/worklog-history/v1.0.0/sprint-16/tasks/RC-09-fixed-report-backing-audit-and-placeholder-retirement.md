# RC-09 固定报表 backing 审计与占位模板退役

**状态**: DONE
**目标**: 核对当前固定报表模板的真实 backing 状态，避免继续向用户暴露“目录可见但不可执行”的概念模板。

## 已确认问题

1. `0040_seed_finance_procurement_templates.xml` 的 `16` 个模板中，大多数 `target_object` 不是当前已验证存在的数据面。
2. `rs_cloud_flower` 测试库当前 `VIEW = 0`，而 `0036_business_views_metadata.xml` 中登记的 `7` 个 `v_*` 视图并未实际落库。
3. `FixedReportResource` 当前仅返回 route / rationale / sourceType / freshness 等执行计划元数据，不返回真实报表结果。
4. Copilot 已可命中固定报表并展示“查看固定报表”入口，但这并不等价于该模板已有真实数据执行能力。

## 审计范围

- `analytics_report_template`
- `0040_seed_finance_procurement_templates.xml`
- `TemplateMatcherService` 中的固定报表 intent
- `FixedReportResource`
- `FixedReportsPage` / `FixedReportRunPage`
- `business_view_registry`
- `mart_project_fulfillment_daily`
- `fact_field_operation_event`

## 审计输出

每个模板至少给出：

- `templateCode`
- `name`
- `targetObject`
- `现网页面锚点`
- `backingStatus`
  - `BACKED`
  - `DECLARED_ONLY`
  - `PLACEHOLDER_ONLY`
- `nextAction`
  - `保留`
  - `重命名`
  - `改 targetObject`
  - `隐藏/不发布`
  - `等待取数面建设`

## 实施要求

1. 目录默认不再把 `PLACEHOLDER_ONLY` 模板当作“可运行固定报表”
2. `FixedReportResource` 对未完成 backing 的模板要给出明确状态，不再伪装成 `READY`
3. `TemplateMatcherService` 的固定报表 intent 必须和目录种子保持一致，避免 AI 命中不存在或已退役模板
4. sprint-16 完成时必须输出一份 `template backing` 对照表

## 已完成

- `0040_seed_finance_procurement_templates.xml` 当前 `16` 个种子已全部显式标记 `placeholderReviewRequired=true`
- `FixedReportResource` 已将占位模板收口为：
  - `supported=false`
  - `executionStatus=BACKING_REQUIRED`
- `ReportTemplateCatalogService` 已向目录接口暴露 `placeholderReviewRequired`
- `FixedReportsPage` / `FixedReportRunPage` 已显式展示“待补数据面”，并禁用占位模板执行
- `template backing` 对照表已输出到：
  - [template-backing-audit.md](/opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-16/template-backing-audit.md)
