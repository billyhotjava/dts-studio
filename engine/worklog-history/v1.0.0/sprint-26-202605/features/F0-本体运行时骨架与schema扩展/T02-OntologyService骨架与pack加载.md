# T02: OntologyService 骨架与 pack 加载

**优先级**: P0
**状态**: DONE
**依赖**: T01

## 目标

新增薄运行时 `OntologyService`，从 `SemanticPackService` 已加载的 pack 构建内存对象图模型（对象 + 属性 + links + metrics + signals + actions 的索引），但本 Task 只做加载与索引，空跑不改变现有 planner 输出。

## 技术设计

- 位置：`dts-copilot-ai/.../service/copilot/OntologyService.java`，Spring `@Service`，依赖注入 `SemanticPackService`。
- 启动时（或首次访问，沿用现有 5 分钟缓存风格）构建：
  - `objectIndex`: name → 对象（含 view、keyDimensions、keyMeasures）。
  - `linkGraph`: 邻接表，from-object → [link]。
  - `metricIndex` / `signalIndex` / `actionIndex`: object → [...]。
- 暴露只读查询方法：`getObject(name)`、`neighbors(objectName)`、`metricsOf` / `signalsOf` / `actionsOf`。
- 本 Task **不**接入 planner 决策树（F1/T03 才接）。

## 影响范围

- 新增 `OntologyService.java`（单一职责，<300 行）。
- 不改 `AssetBackedPlannerPolicy`、不改现有 catalog 服务。

## 验证

- [x] 单测：加载 flowerbiz pack 后 objectIndex 含 10 个对象，linkGraph 在 F1 填 links 后能取到邻居（先以 mock pack 验证图构建逻辑）。
- [x] 单测：缺省四节时各 index 为空，不抛异常。
- [x] 现有 planner 输出回归无变化（OntologyService 仅加载、未被调用）。

## 完成标准

- [x] OntologyService 加载/索引有单测覆盖。
- [x] 不引入对现有 NL2SQL 路径的副作用。

## 证据

- `it/evidence/20260530-local/pack-schema-and-ontology-load.md`
- `mvn -pl dts-copilot-ai -Dtest=SemanticPackServiceTest,SemanticPackOntologySchemaTest,OntologyServiceTest,FlowerbizNl2SqlBaselineTest,AssetBackedPlannerPolicyTest,AgentBiReportCatalogServiceTest test`
