# F4: 差分抽样与持续对账记分卡

**优先级**: P1
**状态**: IN_PROGRESS

## 目标

把正确性从"一次性证明"变成"持续监控的属性"：对代表性过滤网格做 copilot vs 权威基准差分测试，并做成每日可跑的对账记分卡（通过率/漂移/逐项差异），弱路径财务问题自动入对账集。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 代表性过滤网格差分测试 | P1 | IN_PROGRESS | F1,F2 |
| T02 | 持续对账记分卡 | P1 | IN_PROGRESS | T01 |
| T03 | telemetry 接入（弱路径财务问题入对账集） | P2 | IN_PROGRESS | T01,S32-F1 |

## 完成标准

- [x] 本地代表性过滤网格 contract 可重跑，覆盖项目×月份×日期区间边界并输出差异定位
- [ ] 代表性过滤网格（项目×月份×日期区间）copilot vs 权威基准端点 live 全绿（到分）
- [x] 本地持续对账记分卡 contract 可重跑，输出四类通过率/漂移/逐项差异，差异越限告警
- [x] 应用 MySQL proof 可作为 opt-in F2 scorecard evidence provider，且不会伪造 F1/F4 live 证据
- [x] 差分网格可作为 opt-in F4 scorecard evidence provider，且没有真实行源时不会注册或伪造 PASS
- [x] summary SQL 可作为差分网格本地行源，按项目/账期/日期区间切片映射真实 copilot/权威基准行，未知过滤条件直接失败而不伪造证据
- [ ] 持续对账记分卡每日 live 可跑，输出通过率/漂移/逐项差异，差异越限告警
- [x] 本地 route telemetry 弱路径财务候选 contract 可重跑，未覆盖候选进入 scorecard drift
- [x] 本地 scheduled publisher 可从 route telemetry 汇总发布候选快照并创建本地语义草稿
- [ ] 路由 telemetry 中落弱路径的财务问题 live 复核已自动纳入对账集
