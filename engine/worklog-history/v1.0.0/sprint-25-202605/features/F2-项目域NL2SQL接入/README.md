# F2: 项目域 NL2SQL 接入

**优先级**: P1  
**状态**: DONE

## 目标

把项目域 ADS/DWS 接入 dts-copilot 智能层，使“项目实摆总览、合同到期、摆位状态分布”等问句走可信数据面。

## 当前状态

2026-05-30 F1 已产出可 build 的 ADS/DWS，F2 已完成 baseline 接入：

- `semantic-packs/project-fulfillment.json` 指向 `public.xycyl_ads_project_overview`、`public.xycyl_dws_project_green_monthly`、`public.xycyl_dwd_project_green_snapshot`、`public.xycyl_ads_contract_expiry_alert` 等 Sprint-25 dbt mart。
- `AgentBiReportCatalogService` 增加项目总览、项目绿植月报、项目状态分布、合同到期预警四个 L2 report entry。
- `BusinessObjectCatalogService` 增加项目摆位和项目实摆绿植业务对象。
- `v1_0_0_021__project_dbt_query_templates.xml` 新增 `TPL-44` 至 `TPL-51` 8 个 project dbt templates，并停用 legacy `v_project_*` project templates。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | `project.json` 语义包与 constraints | P1 | DONE | F1 |
| T02 | routing/templates/enum liquibase | P1 | DONE | T01 |
| T03 | Java 目录接入与加载测试 | P1 | DONE | T01, T02 |

## 完成标准

- [x] 语义包运行时加载有单测，不只检查文件存在。
- [x] 项目域高频问句优先命中 ADS/DWS，不扫 ODS。
- [x] 业务对象问答带 pagePath、sourceRefs、qualityLevel、dataSurface。
- [x] query templates 至少覆盖 8 条项目域高频问句。

## 验证

证据见 `it/evidence/20260530-local/project-nl2sql-dbt-routing.md`。
