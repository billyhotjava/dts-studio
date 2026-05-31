# Sprint-29: dts-platform 指标联邦接入 copilot

**时间**: 2026-05
**前缀**: IF (Indicator Federation)
**状态**: READY
**目标**: 把 dts-platform 治理过的**权威指标**接进 dts-copilot agent-first 单入口——用户问数时优先拿到「口径权威」的指标答案(命中即调平台 API 取值、用 copilot 自己的 echarts 渲染),未命中才退回 AI 现生成 SQL。

## 背景

dts-platform 已有完整治理指标子系统(`/api/governance/indicators*`),指标有权威口径且已编译成 dbt mart 产数。诉求:从平台**取指标**、用 **copilot 自己的可视化组件渲染**(非 iframe 嵌成品报表),延续「治理在 dts-stack、智能在 dts-copilot」。

设计依据:`docs/superpowers/specs/2026-05-31-dts-platform-indicator-federation-design.md`(brainstorm 产出,决策 D1–D7)。

## 范围与核心决策

- **D1 服务联邦**:copilot 实时调平台指标 API 取值,自己渲染;口径单一源在平台。
- **D2/D3 指标优先路由(C,copilot 目标态)**:指标目录同步进 copilot 语义层,复用现有意图路由,优先级 `已发布指标 > 视图/mart > 现生成 SQL`。
- **D4 平台零改码**:4 个端点(`/indicators`、`/indicators/dashboard`、`/{id}/detail`、`/{id}/drilldown`)+ `IndicatorDto` 字段均已就位,copilot 单方适配。
- **D5 试用期单机器账号**:简单快部署;⚠️ 生产/多租户前切回令牌透传恢复行列权限/密级(显式权宜,非永久)。
- **D6 显式降级**:平台不可达 → 提示 + 一键退回现生成 SQL,不静默。

## 底座依赖

复用 **sprint-27**:F4 活产物画布、F5 口径芯片/乐观执行、F6 溯源、F8 契约;**sprint-28** 已收口路由/资产入口/契约缺口。
⚠️ 若 sprint-27 的 F4/F6/F8 仍有 live 缺口,本 Sprint 落地前需确认已由 sprint-28 压实,否则指标产物会踩同样运行时坑。

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 阶段 | 状态 |
|----|---------|---------|--------|------|------|
| F1 | 跨服务接入与指标目录同步(后端) | 5 | P0 | P1 | READY |
| F2 | 指标产物与渲染(前端) | 4 | P0 | P1 | READY |
| F3 | 指标优先路由(后端) | 3 | P0 | P2 | READY |
| F4 | 路由结果接入 agent-first UI(前端) | 3 | P1 | P2 | READY |
| F5 | 下钻与增强健壮性(全栈) | 4 | P2 | P3 | READY |

## 依赖顺序

```
P1: F1(后端接入+同步+BFF) ──┬─> F2(前端产物+渲染+资产库浏览)   ← 最快可演示闭环
                            │
P2: F1 ─> F3(指标优先路由) ──┴─> F4(命中→芯片/溯源/乐观执行合流, 复用 sprint-27 F5/F6)
                            └─> 未命中 → 退回现生成 SQL(existing)
P3: F5(drilldown 下钻 / 口径version提醒 / 多候选切换 / 降级·微缓存 / 可观测) 依赖 F2+F4
```

## 落地分段(贴合「尽快简单上手部署」)

- **P1**:F1+F2 —— 机器账号认证 + 目录同步 + 资产库「平台指标」浏览 + 点开调 API 渲染为 Indicator 产物。先上能演示的取数+渲染闭环。
- **P2**:F3+F4 —— 指标优先路由 + 命中接入 agent-first 乐观执行/芯片/权威溯源。
- **P3**:F5 —— 下钻、口径变更提醒、多候选、降级体验、可观测。

## 非目标

- 不做指标的创建/编辑(写回平台);copilot 只读消费已发布指标。
- 不 iframe 嵌平台成品报表/大屏;不在 copilot 重算口径(口径恒取平台)。
- dts-platform 不改业务代码(仅配置机器账号只读角色)。

## 完成标准

- [ ] copilot 用机器账号能拉到 dts-platform 已发布指标目录并定期刷新(P1)
- [ ] 资产库「平台指标」可浏览,点开调 `dashboard/detail` 用 copilot echarts 渲染为 Indicator 产物(P1)
- [ ] 问数命中已发布指标时,优先调指标 API 取权威值;未命中退回现生成 SQL(P2)
- [ ] 命中指标做成可改芯片(切候选/退回),溯源显示「平台指标X·口径vN」vs「现生成」(P2,复用 sprint-27 F5/F6)
- [ ] 平台不可达/超时有显式降级 + 一键退回,不静默不阻断问数(全程)
- [ ] dts-platform 零代码改动(仅配置机器账号只读角色)
- [ ] `it/README.md` 有真实集成验证证据(本地 + 对平台的 live contract)
