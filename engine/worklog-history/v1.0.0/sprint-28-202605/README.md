# Sprint-28: Agent-First 收口与缺陷修复

**时间**: 2026-05
**状态**: DONE
**目标**: 修复 Sprint-27 重构后暴露的路由接线、资产入口、跨域信号与状态簿记缺口,让 Agent-First 工作台从“骨架可用”收口为“导航可用、证据可信、运行可验”。

## 背景

三路代理诊断确认:当前 typecheck/test/build 全绿,页面问题不是编译错误,而是运行时连接缺失。关键断点集中在:

- `/agent-bi?view=sessions` 与 `/agent-bi?view=signals` 已被导航指向,但 `AgentWorkspacePage` 未消费 `view` 参数。
- `/agent-bi?fixedReport=xxx` 已由固定报表入口生成,但工作台未消费 `fixedReport` 参数。
- `/assets` 直接嵌入旧资产页面,和 `/dashboards`、`/questions`、`/collections` 形成双入口与双 PageHeader。
- Sprint-26 已有 signals/action 等后端能力,但前端信号入口仍是空壳或占位数据。
- `sprint-queue.md` 与 S25/S26/S27 真实证据存在不一致,容易把未验证项误读为完成。

本 Sprint 是收口修复批次,不是重新设计 Agent-First。优先修 P0 运行断链,再修 P1 跨域能力和状态簿记,最后清理 P2 孤儿页/死常量。

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 状态 |
|----|---------|---------|--------|------|
| F1 | 工作台路由接线 | 4 | P0 | DONE |
| F2 | 资产库入口收口 | 3 | P0 | DONE |
| F3 | 信号与跨域能力接入 | 3 | P1 | DONE |
| F4 | 状态簿记与证据校准 | 4 | P1 | DONE |
| F5 | 清理与防回归 | 3 | P2 | DONE |
| F6 | 单窗口结果面收口 | 1 | P0 | DONE |

## 依赖顺序

```text
F1-T01/T02/T03 先修导航进入工作台后的消费端
    └─> F1-T04 用浏览器验证 view/fixedReport 链路

F2 依赖 F1 路由口径稳定后收口资产入口
F3 依赖 F1-T02 的 signals 视图容器
F4 可并行,但任何状态改 DONE 必须先有证据文件
F5 在 F1/F2/F3 主要运行链路稳定后清理
```

## 非目标

- 不重写 Sprint-27 的 Agent-First 主视觉和核心布局。
- 不新增新的业务数据面或大范围后端建模。
- 不把无 live 或浏览器证据的项目标为 DONE。
- 不为了收口资产库而删除卡片、看板、集合详情和编辑深链。

## 完成标准

- [x] `/agent-bi?view=sessions` 能进入历史会话视图,不是冷启动空壳。
- [x] `/agent-bi?view=signals` 要么展示真实信号数据,要么在真实数据未接通前隐藏入口并给出证据,不能保留死链。
- [x] `/agent-bi?fixedReport=xxx` 能保留固定报表上下文并触发对应执行或明确错误态。
- [x] `/assets` 与旧列表入口不再出现双 PageHeader/双入口混乱;旧列表 URL 的行为有测试覆盖。
- [x] 冷启动信号卡不再使用会被误解为真实业务预警的占位数据。
- [x] S25/S26/S27 queue 状态与证据口径完成一次校准,所有变更有证据链接。
- [x] `pnpm typecheck`、`pnpm test`、`pnpm build` 和 Playwright 路由 smoke 全绿后才能关闭 Sprint。
- [x] Agent BI active conversation 收敛为单窗口结果面,不再挂载右侧 `CanvasPanel`。
