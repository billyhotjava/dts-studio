# F1: 五层路由阶梯与 telemetry

**优先级**: P0
**状态**: DONE

## 目标

把 agent 的取数路由从"谁先命中谁"重构为"口径安全强度从强到弱"的显式 5 层阶梯，每层从治理层（Sprint-31 SoT）解析口径，路由可解释，并用 telemetry 把"落到弱路径的问题"沉淀为"该建 mart 的候选信号"。

## 路由阶梯（目标态）

```
1 指标优先        → dts-platform 已发布指标（治理，口径最安全）
  └ miss → 2 模板/mart  → dbt ADS via 模板（口径已物化）
        └ miss → 3 本体对象图 → links/metrics/signals 导航（链路固定）
              └ miss → 4 guardrail 联邦 → Trino(mysql biz + postgres lake)，pack guardrails
                    └ miss → 5 直连明细只读 → biz DB（窄，最后手段）
每层口径/语义均从治理层解析，而非各自硬编码。
```

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | AssetBackedPlannerPolicy 路由阶梯重构 | P0 | DONE | S31-F2 |
| T02 | 可解释路由（每层 fallback 决策与原因） | P0 | DONE | T01 |
| T03 | 路由 telemetry 与建 mart 候选信号 | P1 | DONE | T01 |

## Task 明细

### T01 路由阶梯重构
- **目标**：把现有隐式路由分支显式化为 5 层有序阶梯，统一 fallback 契约。
- **设计**：重构 `AssetBackedPlannerPolicy.plan()` 为有序责任链，每层 `{canHandle, resolve, onMiss}`；口径从 Sprint-31 治理层 SoT 解析（不硬编码）。保持现有指标优先（sprint-29）、模板（TemplateMatcher）、本体（sprint-26 OntologyService）、联邦（sprint-30 F6）、直连五条路的既有实现，只重组顺序与衔接。⚠️ 改 planner 前评估 blast radius。
- **影响**：`dts-copilot-ai` planner；不改各路自身实现。
- **验证**：覆盖各层命中与逐层 fallback 的单测；既有 planner 测试不回归。
- **当前进展（2026-06-03）**：已补齐 Tier 5 业务对象画像弱路径的完整 fallback 契约，低库存/采购配送画像现在稳定记录 `TIER_1 MISS -> TIER_2 MISS -> TIER_3 MISS -> TIER_4 MISS -> TIER_5 HIT`；`AgentBiReportCatalogService` 的非固定报表目录命中按 `responseKind` 映射到 Tier 4/5，不再全部冒充 Tier 2；新增 `RouteTier` 枚举，routeTrace 写入点不再散落裸字符串，五层顺序已有结构化单测锁定。`AssetBackedPlannerPolicy` 已拆出 `RouteEvaluationContext` 与 `PlanRoute`，`assetRouteChain()` 以固定责任链顺序评估发布指标、模板 SQL、固定资产、本体 signal/对象图、业务对象画像、联邦草稿、候选建议、澄清和通用分析 fallback。
- **验证证据**：`it/evidence/20260603-local/f1-route-responsibility-chain.md`。

### T02 可解释路由
- **目标**：每次回答记录"走了哪层、为何没走更上层"，便于诊断与 telemetry。
- **设计**：路由决策产出 `routeTrace`（命中层 + 各层 miss 原因），透出到响应元数据（不泄漏内部细节给终端用户，仅诊断面）。
- **影响**：planner + 响应 DTO + 前端诊断面（可选）。
- **验证**：典型问题的 routeTrace 正确反映实际路径。

### T03 路由 telemetry 与候选信号
- **目标**：把"落到 Tier 4/5（联邦/直连）"的问题聚合成"该建 mart/指标"的候选信号。
- **设计**：将 routeTrace 的最终层级、目标对象、用户问题写入 assistant message 的 trace.telemetry；提供 AI 内部与 analytics 代理查询接口，按弱路径聚合建 mart 候选。
- **影响**：`AgentChatService`、`RouteTelemetryService`、AI internal/public chat resource、analytics chat proxy。
- **验证**：构造一批落 Tier 4/5 的问题，候选信号正确聚合排序；analytics 可代理读取 telemetry 摘要。

## 完成标准

- [x] 路由为显式 5 层有序阶梯，口径从治理层解析
- [x] routeTrace 可解释，每次回答可还原路径
- [x] telemetry 可见 Tier 分布与建 mart 候选信号
