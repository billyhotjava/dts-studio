# F1 routeTrace 可解释路由证据

**日期**: 2026-06-03
**环境**: local `/opt/prod/prs/source/dts-copilot`
**范围**: Sprint-32 F1/T02

## RED

新增 `AssetBackedPlannerPolicyTest` routeTrace 断言后，首次运行失败，证明以下分支原先没有 trace：

- 固定报表目录：期望 `TIER_1_PUBLISHED_INDICATOR MISS -> TIER_2_MART_TEMPLATE HIT`，实际为空。
- 业务对象画像：期望 `TIER_1 MISS -> TIER_2 MISS -> TIER_5_DIRECT_DETAIL HIT`，实际为空。
- 本体对象图：期望 `TIER_1 MISS -> TIER_2 MISS -> TIER_3_ONTOLOGY_OBJECT_GRAPH HIT`，实际为空。
- 风险 signal：期望 `TIER_1 MISS -> TIER_2 MISS -> TIER_3_ONTOLOGY_OBJECT_GRAPH HIT`，实际为空。

## GREEN

重跑命令：

```bash
bash worklog/v1.0.0/sprint-32-202607/it/test_f1_route_trace.sh
```

结果：

- `AssetBackedPlannerPolicyTest` 通过。
- `CopilotChatContractTest` 通过。
- Maven exit code: `0`。

## 覆盖路径

| 路径 | 代表用例 | routeTrace |
|------|----------|------------|
| 已发布指标 | `publishedIndicatorMatchHasHigherPriorityThanTemplateAndViewRouting` | `TIER_1_PUBLISHED_INDICATOR HIT` |
| 模板 SQL | `templateMatchKeepsTemplateFastPath` | `TIER_1 MISS -> TIER_2_MART_TEMPLATE HIT` |
| 固定报表目录 | `explicitNewReportRequestUsesUnifiedFixedReportCatalogWhenAssetExists` | `TIER_1 MISS -> TIER_2_MART_TEMPLATE HIT` |
| 业务对象画像 | `procurementDeliveryRecordStatusQuestionUsesBusinessObjectProfileSurface` | `TIER_1 MISS -> TIER_2 MISS -> TIER_5_DIRECT_DETAIL HIT` |
| 本体对象图 | `traversalQuestionUsesObjectGraphNavigationInsteadOfSingleObjectProfile` | `TIER_1 MISS -> TIER_2 MISS -> TIER_3_ONTOLOGY_OBJECT_GRAPH HIT` |
| 风险 signal | `riskQuestionUsesOntologySignalBranchBeforeReportCatalog` | `TIER_1 MISS -> TIER_2 MISS -> TIER_3_ONTOLOGY_OBJECT_GRAPH HIT` |

## 结论

F1/T02 已具备可解释 routeTrace 基础能力；T03 仍需把 trace 聚合成 telemetry 与建 mart 候选信号。
