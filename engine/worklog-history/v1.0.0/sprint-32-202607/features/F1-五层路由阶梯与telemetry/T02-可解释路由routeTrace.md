# T02: 可解释路由 routeTrace

**优先级**: P0
**状态**: DONE
**依赖**: T01

## 目标

让 agent 每次路由都能输出可诊断的 `routeTrace`，说明上层路径为什么没有命中，以及最终落到哪一层。

## 技术设计

- 在 `ConversationPlan` 增加 `routeTrace` 与 `RouteStep`，统一承载 `tier/label/status/reason/target`。
- `AssetBackedPlannerPolicy` 在关键路由分支写入 trace：
  - `TIER_1_PUBLISHED_INDICATOR`：平台已发布指标。
  - `TIER_2_MART_TEMPLATE`：dbt mart、模板 SQL、固定报表/资产目录。
  - `TIER_3_ONTOLOGY_OBJECT_GRAPH`：semantic pack 对象图与 signal。
  - `TIER_4_GUARDRAIL_FEDERATED`：现生成报表草稿、通用受控联邦查询。
  - `TIER_5_DIRECT_DETAIL`：业务对象只读明细/画像。
- `CopilotChatContract` 将 `routeTrace` 放入 trace 元数据，供前端诊断面和后续 telemetry 使用。

## 影响范围

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/ConversationPlannerService.java`
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/AssetBackedPlannerPolicy.java`
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/CopilotChatContract.java`
- `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/AssetBackedPlannerPolicyTest.java`
- `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/CopilotChatContractTest.java`

## 验证

- [x] RED：固定报表目录、业务对象画像、本体对象图、风险 signal 缺少 `routeTrace` 时测试失败。
- [x] GREEN：`AssetBackedPlannerPolicyTest` 覆盖 TIER_1/2/3/5 命中与 miss 链路。
- [x] GREEN：`CopilotChatContractTest` 覆盖 `trace.routeTrace` 对外透出。
- [x] 运行 `worklog/v1.0.0/sprint-32-202607/it/test_f1_route_trace.sh` 并沉淀证据。

## 完成标准

- [x] planner 分支不再只有隐式命中，至少核心五层路径可以被 trace 解释。
- [x] trace 不改变原有路由结果、prompt 和 SQL，只增加诊断元数据。
- [x] IT 证据归档到 `it/evidence/20260603-local/f1-route-trace.md`。
