# F5: 范式固化与 IT 证据

**优先级**: P2
**状态**: DONE

## 目标

把路由阶梯 + 联邦治理 + 场景接入套件的实践固化为多场景接入手册，并归档真实可重跑证据，供后续库存/督导/薪资/在摆历史域批量接入复用。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 多场景接入手册 | P2 | DONE | F1-F4 |
| T02 | Sprint-32 IT 证据包 | P2 | DONE | F1-F4 |

## Task 明细

### T01 多场景接入手册
- **目标**：一份"从零接入一个业务场景"的权威手册，串起 Sprint-31 收口 + Sprint-32 路由/联邦/套件。
- **设计**：合并 Sprint-31 语义收口 onboarding + F3 可执行套件 + F4 实战经验，给"软隔离 vs deploy-per-scenario"选型指南。形式 `assets/multi-scenario-onboarding-guide.md`。
- **验证**：F4 接入过程即手册的实证演练；可重跑 `../../it/test_f5_multi_scenario_guide.sh`。
- **产物**：`../../assets/multi-scenario-onboarding-guide.md`。

### T02 Sprint-32 IT 证据包
- **目标**：归档真实可重跑证据。
- **设计**：`it/` 含——路由阶梯命中分布与 routeTrace 样例（F1）、联邦读副本/限流/Ranger 脱敏/资源护栏验证（F2）、新场景端到端跑通日志（F4-T01）、多场景共存隔离回归（F4-T02）。每条带可重跑命令（沿用 sprint-30 `it/test_*.sh` 风格）。
- **验证**：已用 `../../it/test_f5_multi_scenario_guide.sh` 校验手册与证据索引；`../../it/test_sprint32_completion_gate.sh` 作为 Sprint-32 DONE 门禁，已覆盖 F1-F5 文档状态、Trino access-control/resource group 配置和 F4 runtime build/ADS 对账证据。

## 完成标准

- [x] 多场景接入手册成文，含隔离选型指南
- [x] `it/README.md` 证据齐全、可重跑、非占位；`test_sprint32_completion_gate.sh` exit code 为 0
