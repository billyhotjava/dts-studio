# F4: 本体范式固化与验收

**优先级**: P2
**状态**: IN_PROGRESS

## 目标

把报花域走通的本体三层抽象成一份可复用的《本体化域接入 checklist》，为后续复制到项目/采购/财务铺路；并汇总 Sprint-26 的端到端验收证据。

## 阻塞

T01 checklist 已基于 F0-F3 已完成部分固化。T02 证据包仍受 F3/T04 真实端到端阻塞影响：当前运行态缺正确 PRS adminapi gateway base URL 与业务 Authorization。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 本体化域接入 checklist | P2 | DONE | F1, F2, F3 |
| T02 | Sprint-26 IT 证据包 | P2 | BLOCKED | F1, F2, F3 |

## 完成标准

- [x] checklist 明确每个新域要补的 links/metrics/signals/actions 节、需扩展的 Java 点、需对账的 adminweb 报表、需准备的 adminapi 草稿端点。
- [ ] IT 证据覆盖 schema 兼容性、运行时加载、对象图导航、预警对账、Action 审计链路，非空占位。
