# T03: AssetBackedPlannerPolicy 对象图导航决策分支

**优先级**: P0
**状态**: DONE
**依赖**: T02

## 目标

在 `AssetBackedPlannerPolicy.plan()` 现有决策树中新增"对象图导航"分支，识别贯穿/追溯类问句并交给 OntologyService，而非走单视图 NL2SQL。

## 技术设计

- 现有决策树：BusinessDirectResponse → TemplateMatcher → BusinessObjectCatalog → AgentBiReportCatalog → free Workflow。
- 新增分支插入位置：在 BusinessObjectCatalog 评估之后、free Workflow 之前；当问句涉及"≥2 个对象 + 关系/贯穿/全链路/追溯"语义时，优先返回对象图导航，避免被单对象画像误抢。
- 命中判定：基于 OntologyService 的对象/同义词识别 + 关系关键词（"从…到…""贯穿""全流程""追溯"）。
- 复用现有 4 处 Java 扩展点同款模式（不新增架构）。

## 影响范围

- `AssetBackedPlannerPolicy.java`：注入 OntologyService，新增分支。
- `ConversationPlannerService.ResponseKind`：新增 `OBJECT_GRAPH_NAVIGATION`。
- `OntologyService.OntologyModel`：暴露对象列表用于 planner 命中判定。
- 未新增 `OntologyNavigationCatalogService`；当前逻辑足够薄，保留在 planner 分支内。

## 验证

- [x] 贯穿类问句路由到对象图导航分支。
- [x] 单对象问句仍走原有分支（不误抢）。
- [x] F0/T03 回归基线无退化。

## 完成标准

- [x] planner 决策树新增分支有单测，命中/不命中边界清晰。

## 证据

- `it/test_object_graph_planner.sh`
- `it/evidence/20260530-local/object-graph-planner.md`
