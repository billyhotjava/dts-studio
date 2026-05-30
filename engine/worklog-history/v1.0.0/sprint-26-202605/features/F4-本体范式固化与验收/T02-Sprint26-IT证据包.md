# T02: Sprint-26 IT 证据包

**优先级**: P2
**状态**: DONE
**依赖**: F1, F2, F3

## 目标

汇总 Sprint-26 端到端验收证据，确保每个 Feature 的完成标准都有真实证据支撑，非空占位。

## 技术设计

证据清单：
- **F0**：pack schema 向后兼容测试结果、OntologyService 加载单测、NL2SQL 回归基线快照。
- **F1**：对象图 JOIN 生成单测、贯穿类 Golden Questions 命中率报告。
- **F2**：signals 求值单测、预警对账误差报告。
- **F3**：Action 安全边界单测、一键坏账草稿端到端审计链路。

## 影响范围

- `it/README.md` 索引、`it/evidence/<日期>-local/` 各证据文件。

## 验证

- [x] 每个完成标准条目都能在 IT 证据中定位到对应文件。
- [x] 无空占位证据。

## 完成标准

- [ ] IT 证据包完整，可作为 sprint DONE 的依据。

## 验收结论

- F0/F1/F2 和 F3/T01-T03 均已有可重跑证据。
- F3/T04 已补运行态证据：`test_action_runtime_env_wiring.sh` 静态配置渲染通过，`RUN_LIVE=1` 通过，Copilot 审计日志 `id=326`，adminapi 草稿 `2060736510340108288`，adminweb 坏账 `listPage` 返回该草稿。
- 证据：`it/evidence/20260530-local/action-runtime-live.md`、`it/evidence/20260530-local/baddebt-e2e-auth-blocker.md`。
