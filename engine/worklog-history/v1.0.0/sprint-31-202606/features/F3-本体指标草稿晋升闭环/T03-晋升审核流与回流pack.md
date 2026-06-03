# T03: 晋升审核流 + 回流 pack

**优先级**: P1
**状态**: READY
**依赖**: T02

## 目标

打通"草稿 → 人审晋升 → 正式定义 → 经 F2 sync 回流 pack"的最后一段，使整条 发现→定义→治理→消费 闭环可端到端跑通一例。

## 技术设计

- 晋升走治理层现有 review/approve（modeling review 机制），审通过后 DRAFT → 正式版本，进入 SoT。
- 正式化后由 F2-T02 生成器在下次 sync 时纳入 pack 生成区，回流到 agent。
- 闭环演练用例：选一个 finance/procurement 域真实缺失的指标或口径规则，走完 草稿→审→正式→回流，agent 之后能用上。
- 记录闭环时延（草稿到回流），作为流程基线。

## 影响范围

- dts-platform：晋升/approve 流（复用现有）
- `dts-copilot-ai`：回流由 F2 sync 承接，本任务验证端到端

## 验证

- [ ] 一例草稿走完全流程并在 agent 回答中体现
- [ ] 未审通过的草稿不进入 pack

## 完成标准

- [ ] 闭环端到端跑通一例并留证据（入 F5/IT）
