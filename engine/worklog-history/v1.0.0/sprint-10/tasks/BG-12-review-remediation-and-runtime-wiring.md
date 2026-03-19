# BG-12: Sprint 10 Review 修复与运行时接线

**优先级**: P0
**状态**: IN_PROGRESS
**依赖**: BG-03, BG-04, BG-07, BG-11

## 目标

把 sprint-10 已实现但未接入主链的能力真正接到 `CopilotChat` 生产路径上，同时清理误导性的旁路接口，保证：

1. 聊天主链能消费业务路由与语义上下文
2. 推荐问句与反馈接口端到端可用
3. 不保留绕开 session / 权限模型的 SQL 执行旁路
4. 模糊问题能触发澄清，而不是硬路由

## 修复范围

- AI: `IntentRouterService`、`SemanticPackService`、`AgentExecutionService`、`Nl2SqlResource`
- Analytics: `CopilotQueryResource`、`CopilotQueryService`
- Webapp: `CopilotChat`、`WelcomeCard`、`FeedbackButtons`、`analyticsApi`

## 验收标准

- 欢迎卡优先显示后端推荐问句，后端不可用时再回退到本地默认问句
- 反馈按钮提交后能真实落到 `ai_chat_feedback`
- 用户实际聊天路径包含业务语义上下文，而不是仅通用 prompt + RAG
- 低置信度问题返回澄清意图
- 未接线的 `copilot execute` 旁路不再保留权限偏差
