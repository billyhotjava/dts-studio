# F3: 指标优先路由(后端)

**优先级**: P0
**状态**: READY

## 目标

让 agent 问数时**优先匹配 dts-platform 已发布指标**:命中 → 调 F1 取值客户端拿权威聚合值并填进活产物;未命中 → 退回现有「现生成 SQL」主链。在 copilot 后端 (`dts-copilot-ai`) 复用现有意图路由(`IntentRouterService` / `AssetBackedPlannerPolicy`)接入点,落地硬优先级:

```
已发布指标(高置信) > 视图/mart > 现生成 SQL
```

命中时把指标口径(`IndicatorDto.definition/expressionSql/version`)填进 sprint-27 F8 契约的 `trace.metricCaliber`,命中指标名做成**可改假设芯片**供前端切候选/退回,数据填进产物;取值失败时**显式降级**,绝不静默假装有数(对应设计 §5 路由判定逻辑、D6)。

## 范围与接入点(已核实真实代码)

- 路由核心决策点:`dts-copilot-ai/.../service/copilot/AssetBackedPlannerPolicy.java#plan(...)` —— 现已串接 `templateMatcherService` → `intentRouterService.routeWithDataLayer(...)` → ontology/signal/report 多分支,F3 在其**最前**插入「已发布指标」分支。
- 意图路由引擎:`service/copilot/IntentRouterService.java`(`route` / `routeWithDataLayer`,关键词打分 + 数据层判定)。
- 同义词字典(sprint-8 NV-07):`service/copilot/SemanticPackService.java#getSynonyms(domain)`,从 `semantic-packs/*.json` 加载。
- 计划对象:`service/copilot/ConversationPlannerService.java#ConversationPlan`(承载路由结果,经 `ChatExecutionResult` 传到契约层)。
- F8 契约写入:`service/copilot/CopilotChatContract.java`(`buildTrace` 产出 `trace.metricCaliber`,`buildAssumptions` 产出可改芯片;前端类型 `dts-copilot-webapp/src/api/types.ts` 的 `CopilotTraceMetricCaliber`/`CopilotAssumption` 已就位)。
- 取值/目录依赖(F1 产出,本 Feature 仅消费):`IndicatorCatalogSyncService`(本地已发布指标目录缓存)、`PlatformIndicatorClient`(调 `dashboard/detail/drilldown` + 机器账号鉴权 + 超时降级)。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 指标匹配引擎(`IndicatorMatcherService`) | P0 | READY | F1(目录已同步到语义层) |
| T02 | 路由优先级集成(接入 `AssetBackedPlannerPolicy`,新增「已发布指标」最高优先目标) | P0 | READY | T01 |
| T03 | 命中取值 + 响应契约(复用 sprint-27 F8) | P0 | READY | T02、F1(取值客户端) |

## 完成标准

- [ ] 问句命中高置信已发布指标时,路由判定 `PUBLISHED_INDICATOR` 优先于视图/mart 与现生成 SQL。
- [ ] 命中 → 调 F1 `PlatformIndicatorClient` 取值,`trace.metricCaliber` 填入 `name`=指标名、`formula`=`expressionSql`、`version`=指标 version、`domain`=指标 category;命中指标名以 `editable=true` 的假设芯片暴露(key=`indicator`)供前端切候选/退回。
- [ ] 未命中(低置信/无候选)→ 走原有 `AssetBackedPlannerPolicy` 后续分支,行为与接入前一致(回归无破坏)。
- [ ] 取值失败(命中但超时/报错)→ 显式降级:口径芯片标「平台指标服务暂不可达」+ 退回现生成 SQL,不阻断问数、不静默。
- [ ] 仅匹配 `status=已发布` 指标;F1 目录无缓存时指标路由临时禁用、全退回现生成 SQL。
- [ ] 新增逻辑有单元测试(匹配引擎、路由优先级、契约填充、降级路径),`./mvnw test` 通过。
