# F3: Tier3 写回 Action 闭环

**优先级**: P1
**状态**: IN_PROGRESS

## 目标

定义 Actions（带 params/guard/approval/audit），让本体结论一键回写 adminapi 租赁系统，真正闭环"反馈到管理"。安全姿态：dts-copilot 永远只到"草稿"，正式提交必经 adminapi 业务规则 + adminweb 人工确认——零绕过、全审计、人在环路。

## 阻塞

F1 对象、F2 signal→linkedActions 已完成；T01/T02/T03 已完成。T04 真实端到端当前阻塞在运行态入口：本机探测到的 `38012` 是 v2.2.3 `dts-admin`，不是 PRS 旧 `rs-gateway`；还缺正确 PRS adminapi base URL 与业务 Authorization。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | flowerbiz.json 补 actions 定义（映射 adminapi 端点） | P1 | DONE | F1/T01 |
| T02 | OntologyService Action 编排 + adminapi 草稿调用 | P1 | DONE | T01 |
| T03 | 审批卡片 + 权限 guard + 审计日志 | P1 | DONE | T02 |
| T04 | "一键坏账处理单草稿"端到端验证 | P1 | BLOCKED | T03, F2/T03 |

## 完成标准

- [x] actions 映射 adminapi `saveDraft*`/`save*` 双端点，声明 params、required、guard(权限)、approval=human、audit=true。
- [x] OntologyService 调用只到 `saveDraftFlowerBadDebt`（草稿），绝不直接调 `save*` 提交，绝不直写业务库。
- [x] copilot 回复挂"建议动作"卡片，用户确认后才发起草稿；guard 鉴权失败时拒绝并给出原因。
- [ ] 端到端跑通并留审计链路证据：问答风险 → 建议动作 → 草稿单生成 → adminweb 可见待确认。
