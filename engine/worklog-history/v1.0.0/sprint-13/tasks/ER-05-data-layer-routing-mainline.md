# ER-05: Data-layer 路由接入 Copilot 主链

**优先级**: P1
**状态**: READY
**依赖**: EL-05, ER-03, ER-04

## 问题

`routeWithDataLayer()` 已经存在，但没有接入聊天主链。当前产品仍主要走 Sprint-10 的老路由，不会真正根据“趋势/统计类”切换到主题层。

## 范围

- `ConversationPlannerService`
- `AssetBackedPlannerPolicy`
- `Nl2SqlService`
- `IntentRouterService`
- 路由结果在 chat metadata 中的透传

## 目标

- 明细/实时类优先视图层
- 趋势/统计类在主题层健康时优先走主题层
- 主链上可观测当前命中的 data layer

## 验收标准

- [ ] 主聊天链能消费 data-layer 路由结果
- [ ] session/message metadata 可看到路由层级
- [ ] 主题层模板问题不再回落到旧视图链
