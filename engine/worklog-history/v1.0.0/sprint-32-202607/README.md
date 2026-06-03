# Sprint-32: Agent 数据访问路由阶梯与多场景接入套件

**时间**: 2026-07
**前缀**: RK (Routing & Kit)
**状态**: READY（依赖 Sprint-31 语义收口完成）
**目标**: 把 agent 数据访问路由固化为可观测的 5 层阶梯，治理 Trino 联邦层访问，把"场景接入范式"产品化为可复制套件，并用一个新空白域端到端验证多场景共存。

## 北极星目标（承接）

> 让 agent 在多业务场景下可靠地 NL2SQL 拿到口径正确的业务数据，且新场景接入可复制。

Sprint-31 解决了"口径只有一个真相"；本 sprint 解决"**怎么取数（路由）**"和"**怎么复制到新场景（套件）**"。

## 背景

三轮架构勘探结论中尚未落地的两块：

1. **路由未显式化**：agent 现有 5 条取数路（指标 / mart 模板 / 本体对象图 / Trino guardrail 联邦 / 直连明细）靠"谁先命中谁"，缺有原则的优先级阶梯与可观测性。应固化为"口径安全强度从强到弱"的阶梯，每层从治理层（Sprint-31 SoT）解析口径，并用 telemetry 把"落到弱路径的问题"变成"该建 mart 的信号"。
2. **多场景靠范式而非模块**：架构判断已确认——dts-platform `modeling` 耦合 catalog/dbt 深，不适合抽独立微服务；`CatalogDomain` 是自由参数化数据行，"加场景 = 配数据"；但**无 tenant/scenario 隔离**。所以多场景的正解是把"新 domain 行 + `<scene>_*` dbt namespace + 场景 pack + Trino catalog + glossary 派生"这套**范式产品化为套件**，硬隔离走 deploy-per-scenario。

## 设计依据

- 三轮架构讨论：统一访问平面（Trino）+ 统一治理（Sprint-31 SoT）+ 范式复用（非模块抽取）。
- 复用 Sprint-30 F6 `FederatedQueryGuardrail`、Sprint-30 F5 onboarding checklist、Sprint-31 收口范式。
- dts-stack 有 Ranger（行列级）、Trino、dbt 五层 `xycyl_*` namespace 既有能力。

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 状态 | 说明 |
|----|---------|---------|--------|------|------|
| F1 | 五层路由阶梯与 telemetry | 3 | P0 | READY | 显式优先级阶梯 + 可解释 + 候选信号 |
| F2 | Trino 联邦层访问治理 | 3 | P0 | READY | 读副本/限流 + Ranger 脱敏 + 资源护栏 |
| F3 | 场景接入套件产品化 | 3 | P1 | READY | 范式→可复制套件 + 脚手架 |
| F4 | 新场景端到端验证 | 2 | P1 | READY | 一个空白域跑通 + 多场景共存 |
| F5 | 范式固化与 IT 证据 | 2 | P2 | READY | 多场景接入手册 + 证据包 |

## 依赖顺序

```
Sprint-31(口径 SoT) ──> F1(路由阶梯) ─┐
                       F2(Trino 治理)─┴──> F3(接入套件) ──> F4(新场景验证) ──> F5(范式+IT)
```

## 本 sprint 不做

- 不抽 dts-platform `modeling` 为微服务（架构判断已否）。
- 不做多租户行级隔离改造（隔离需求走 deploy-per-scenario）。
- 不一次性接入全部空白域，只用套件跑通 1 个域作为范式验证。

## 完成标准

- [ ] `AssetBackedPlannerPolicy` 路由为显式 5 层阶梯（指标→mart→对象图→guardrail 联邦→直连明细），每层从治理层解析口径
- [ ] 路由可解释（每次回答记录走了哪层、为何 fallback），telemetry 看板可见 Tier B/C 命中分布
- [ ] Trino→biz MySQL 走读副本/连接限流，联邦路径接 Ranger 行列脱敏，有资源/超时/审计护栏
- [ ] 场景接入套件可执行：一条命令脚手架出新场景的 domain/dbt namespace/pack 模板/Trino catalog/glossary 接线
- [ ] 用 1 个空白域（库存/督导/薪资择一）走套件端到端跑通，并验证多场景口径不串、命名空间不撞
- [ ] `it/README.md` 真实证据：路由阶梯命中分布、联邦治理验证、新场景端到端、共存隔离

## 相邻 sprint 关系

- 输入：Sprint-31 口径 SoT + sync；Sprint-30 F6 联邦 + F5 checklist。
- 输出：可复制的场景接入套件 → 后续库存/督导/薪资/在摆历史域批量接入；路由 telemetry → 建 mart 优先级的数据驱动依据。

## 备注：任务细化策略

各 Feature 的子 task 已在对应 Feature README 的「Task 明细」中给出 task 级粒度（目标/设计/依赖/验证）。本 sprint 为 Sprint-31 的下游，部分子 task 的精确实现取决于 Sprint-31 收口产出与 F1 telemetry 数据，转入 IN_PROGRESS 时再拆为独立 `Txx-*.md` 文件并据实细化（避免过早过度规划，呼应路由 telemetry 数据驱动原则）。
