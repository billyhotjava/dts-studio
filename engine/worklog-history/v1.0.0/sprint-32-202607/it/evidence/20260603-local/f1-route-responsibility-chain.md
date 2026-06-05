# F1/T01 route responsibility chain evidence

**日期**: 2026-06-03
**范围**: `AssetBackedPlannerPolicy` 路由阶梯结构

## 根因

此前类似问题不断复现，不只是 SQL 写错，而是 Agent 路由在固定报表、资产库、dbt mart、业务对象画像和联邦查询之间没有一个稳定的责任链。表现为：

- 可用 ADS 被当成临时 SQL 重新生成。
- 未发布资产被当成固定报表提示用户去资产库查看。
- Trino 联邦入口下生成 `public.table`，执行层要求 `catalog.schema.table`。

F1/T02/T03 已经补齐 `routeTrace` 和 telemetry，但 `plan()` 仍然是长顺序分支，后续新增业务域时容易继续把判断塞回同一个方法。

## RED

新增结构性测试：

```bash
mvn -q -Dmaven.repo.local=/opt/prod/prs/source/.m2 -pl dts-copilot-ai -Dtest=AssetBackedPlannerPolicyTest test
```

首次失败：

```text
Expecting declared classes to contain:
  ["RouteEvaluationContext", "PlanRoute"]
but could not find:
  ["RouteEvaluationContext", "PlanRoute"]
```

说明 planner 只有隐式顺序分支，没有显式责任链。

## GREEN

实现：

- 新增 `RouteEvaluationContext`，集中承载用户问题、已发布指标匹配、模板匹配、业务路由、资产目录、业务对象目录和 `RouteTrace`。
- 新增 `PlanRoute`，每个候选路径返回 `Optional<ConversationPlan>`。
- 新增 `assetRouteChain()`，固定路由顺序：
  1. 已发布指标
  2. 业务能力直接回答
  3. 模板 SQL
  4. 固定报表模板
  5. 优先固定资产目录
  6. semantic pack signal
  7. 业务对象画像
  8. 本体对象图
  9. 资产目录兜底
  10. 现生成报表草稿
  11. 固定报表候选建议
  12. 澄清
  13. 通用受控联邦分析
- 保留原有 routeTrace 和 prompt 构造逻辑，不扩大业务命中范围。

复跑：

```bash
mvn -q -Dmaven.repo.local=/opt/prod/prs/source/.m2 -pl dts-copilot-ai -Dtest=AssetBackedPlannerPolicyTest test
```

结果 PASS。

## 结论

F1/T01 已从长分支推进为显式责任链。后续新增库存、采购、财务等业务域时，应新增或调整 route handler，而不是在 `planInternal` 里继续堆条件分支。
