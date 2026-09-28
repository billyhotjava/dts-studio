# F1: 工作台路由接线

**优先级**: P0
**状态**: DONE

## 目标

让左侧导航和固定报表入口进入 `/agent-bi` 后能渲染对应工作台视图,而不是全部落到冷启动空壳。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 消费 `view=sessions` 并渲染历史会话 | P0 | DONE | 无 |
| T02 | 消费 `view=signals` 并渲染信号容器 | P0 | DONE | T01 |
| T03 | 消费 `fixedReport` / `fixedReportTemplate` 上下文 | P0 | DONE | T01 |
| T04 | 工作台 query 路由浏览器验证 | P0 | DONE | T01-T03 |

## 完成标准

- [x] `AgentWorkspacePage` 读取并规范化 query 参数。
- [x] `view=sessions`、`view=signals`、`fixedReport` 均有明确渲染路径和错误态。
- [x] 导航点击行为有静态测试和 Playwright smoke 证据。
