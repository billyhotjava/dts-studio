# T03: telemetry 接入（弱路径财务问题入对账集）

**优先级**: P2
**状态**: IN_PROGRESS
**依赖**: T01, Sprint-32 F1

## 目标

让 Sprint-32 F1 路由 telemetry 中"落到 Tier 4/5（联邦/直连）的财务问题"自动成为对账集候选——弱路径=口径风险高=最该被对账覆盖。

## 技术设计

- 消费 Sprint-32 `RouteTelemetryService` 的财务域弱路径聚合（已有 trace.telemetry 写入）。
- 高频落 Tier 4/5 的财务问题 → 自动登记为对账候选,进入 F1/F4 对账集与 F3 不变量采样种子。
- 闭环:对账暴露的口径错误 → 触发 Sprint-31 F3 语义草稿（补 mart/指标定义）→ 该问题上移到强路径。
- 与"建 mart 候选信号"共数据源,财务视角加权。

## 影响范围

- `dts-copilot-ai`:telemetry 消费 + 对账候选登记
- 关联 Sprint-32 F1-T03、Sprint-31 F3

## 当前进度

- 已落地 `FinanceWeakPathReconciliationCandidateRegistry`：机器化读取 `finance-weak-path-reconciliation-candidates.v1.json`，固化财务域、弱路径 tier、关键词、频次阈值、对账集目标和 IT 脚本路径。
- 已落地 `FinanceWeakPathReconciliationCandidateService`：消费 Sprint-32 `RouteTelemetryService.RouteTelemetrySummary`，筛选高频财务弱路径信号，生成 F1 明细、F3 不变量 seed、F4 差分网格候选。
- 已实现候选门禁：未覆盖的弱路径候选可转成 scorecard failure/drift；已登记 candidateKey 时不重复报警。
- 已实现 Sprint-31 F3 草稿触发标记：候选频次达到 `semanticDraftThreshold` 时输出 `CREATE_SPRINT31_DRAFT`。
- 未完成：live telemetry 查询、候选持久化、真实 scorecard baseline、Sprint-31 F3 草稿审核流尚未接线。

## 验证

- [x] 本地 contract：构造落 Tier 4/5 的财务问题 → 自动进对账候选
- [x] 本地 contract：未覆盖候选进入 scorecard drift；已登记候选不重复报警
- [x] 本地 contract：高频候选标记 Sprint-31 F3 语义草稿动作
- [x] `it/test_f4_weak_path_reconciliation_candidates.sh`
- [ ] live：从真实 route telemetry 观察财务弱路径问题并持久化入对账候选/草稿流

## 完成标准

- [ ] 弱路径财务问题自动纳入对账集,闭环可演示一例
