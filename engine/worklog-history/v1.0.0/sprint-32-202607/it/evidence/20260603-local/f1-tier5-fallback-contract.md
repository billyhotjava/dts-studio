# F1/T01 Tier 5 fallback contract evidence

**日期**: 2026-06-03
**范围**: `AssetBackedPlannerPolicy` 五层路由阶梯弱路径契约

## 背景

Sprint-32 F1/T01 要求把 agent 取数路由显式化为：

`TIER_1_PUBLISHED_INDICATOR -> TIER_2_MART_TEMPLATE -> TIER_3_ONTOLOGY_OBJECT_GRAPH -> TIER_4_GUARDRAIL_FEDERATED -> TIER_5_DIRECT_DETAIL`

现有实现里业务对象画像会直接从 Tier 2 miss 跳到 Tier 5 hit，缺少 Tier 3/Tier 4 的可解释 fallback，导致 telemetry 虽能识别最终弱路径，但不能还原完整阶梯。

## RED

先给 `lowStockAlertUsesWarehouseBusinessObjectInsteadOfUnpublishedAsset` 增加断言：

- `低库存预警` 不应命中未发布资产。
- 最终命中 `business-object:prs.warehouse.stock_info`。
- routeTrace 必须完整记录：
  - `TIER_1_PUBLISHED_INDICATOR MISS`
  - `TIER_2_MART_TEMPLATE MISS`
  - `TIER_3_ONTOLOGY_OBJECT_GRAPH MISS`
  - `TIER_4_GUARDRAIL_FEDERATED MISS`
  - `TIER_5_DIRECT_DETAIL HIT`

首次运行：

```bash
mvn -q -Dmaven.repo.local=/opt/prod/prs/source/.m2 -pl dts-copilot-ai -Dtest=AssetBackedPlannerPolicyTest#lowStockAlertUsesWarehouseBusinessObjectInsteadOfUnpublishedAsset test
```

结果失败，实际 trace 只有：

```text
TIER_1_PUBLISHED_INDICATOR MISS
TIER_2_MART_TEMPLATE MISS
TIER_5_DIRECT_DETAIL HIT
```

## GREEN

实现补齐：

- `RouteTier`：将五层顺序固化为 `AssetBackedPlannerPolicy` 内部枚举，新增结构测试 `routeTierOrderIsExplicitAndStable`，防止后续改动把层级命名或顺序改乱。
- `RouteTrace.missIfAbsent(...)`：同一 tier 只记录一次 fallback。
- Tier 5 业务对象路径进入前补齐 Tier 3/Tier 4 miss。
- `AgentBiReportCatalogService.ReportCatalogEntry` 二次命中时按 `responseKind` 映射：
  - `FIXED_REPORT` -> Tier 2
  - `REPORT_DRAFT` / `ACTION_PROPOSAL` -> Tier 4
  - `BUSINESS_DETAIL` / `BUSINESS_INSIGHT` -> Tier 5
- 生成报表、澄清、通用业务分析进入 Tier 4 前补齐 Tier 3 miss。

复跑：

```bash
mvn -q -Dmaven.repo.local=/opt/prod/prs/source/.m2 -pl dts-copilot-ai -Dtest=AssetBackedPlannerPolicyTest#routeTierOrderIsExplicitAndStable test
mvn -q -Dmaven.repo.local=/opt/prod/prs/source/.m2 -pl dts-copilot-ai -Dtest=AssetBackedPlannerPolicyTest test
bash worklog/v1.0.0/sprint-32-202607/it/test_f1_route_trace.sh
bash worklog/v1.0.0/sprint-32-202607/it/test_f1_route_telemetry.sh
```

结果均 PASS。`test_f1_route_telemetry.sh` 中的 `upstream unavailable` 为既有流式失败持久化用例主动模拟，脚本退出码为 0。

## 当前边界

本次只补齐五层 fallback 契约和 trace 真实性，`AssetBackedPlannerPolicy.plan()` 仍未完全抽象为 `{canHandle, resolve, onMiss}` 责任链；因此 F1/T01 仍保持 `IN_PROGRESS`，不能标记 DONE。
