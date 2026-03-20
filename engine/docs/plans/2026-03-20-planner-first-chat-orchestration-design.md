# Planner-First Chat Orchestration Design

## Goal

将 Copilot 对话入口从“业务路由先拦截”重构为“planner-first”，让会话先进入统一规划层，再由规划层决定直接回复、模板快路径或 agent/tool 链路，彻底消除库表探索、泛化分析等请求被业务澄清文案提前截断的问题。

## Problem Statement

当前对话主链把 `IntentRouterService` 的业务域关键词路由放在最前面，并用 `needsClarification` 作为全局硬门：

- 业务路由本来只适用于业务 NL2SQL
- 但当前实现把它当成了所有会话的总入口
- 一旦返回 `needsClarification=true`，`AgentExecutionService` 会直接结束
- `schema_lookup`、`execute_query`、ReAct 规划都没有机会参与

这导致每增加一种新意图，都只能继续在 `ChatGroundingService` 里补特判。

## Target Architecture

### 1. Conversation Planner

新增显式规划层 `ConversationPlannerService`，统一输出 `ConversationPlan`：

- `DIRECT_RESPONSE`
- `TEMPLATE_FAST_PATH`
- `AGENT_WORKFLOW`

同时携带：

- `responseKind`
- `promptContext`
- `routedDomain`
- `targetView`
- `templateCode`
- `resolvedSql`
- `dataLayer`
- `martTable`

### 2. Planner Policy

规划层不再把“澄清”作为系统级拦截，而是降级成 agent 的提示策略：

- 问候 / 产品说明：直接回复
- 精确模板命中：模板快路径
- 库表探索：直接进入 agent，提示优先 `schema_lookup`
- 高置信业务分析：附带业务语义上下文进入 agent
- 低置信业务问题：仍进入 agent，但 planner prompt 明确要求先做 schema/上下文探索，再在必要时提出聚焦澄清
- 泛化数据分析问题：进入 agent，不走业务澄清硬拦截

### 3. Business Router Demotion

`IntentRouterService` 继续保留，但职责下沉为 `BusinessAnalysisRouter` 风格的内部组件：

- 只负责“业务分析上下文建议”
- 不再决定整个会话是否可继续
- 不再生成全局阻断型澄清响应

### 4. Execution Mainline

`AgentExecutionService` 改为消费 `ConversationPlan`：

- `DIRECT_RESPONSE`：直接返回
- `TEMPLATE_FAST_PATH`：走现有模板快路径
- `AGENT_WORKFLOW`：统一进入 ReAct/tool 链路

同步和流式路径都使用同一套 planner 结果，避免双实现漂移。

### 5. Frontend/Protocol Hardening

会话消息增加结构化 `responseKind` 元数据，前端不再依赖固定文案前缀：

- `GREETING_GUIDANCE`
- `ASSISTANT_META`
- `SCHEMA_EXPLORATION`
- `BUSINESS_ANALYSIS`
- `BUSINESS_CLARIFICATION`
- `GENERIC_ANALYSIS`

`copilotSessionBootstrap` 优先读取结构化类型，只在旧会话上回退到 legacy 前缀判断。

## Data Flow

1. analytics 收到消息
2. ai `AgentChatService` 调用 `ConversationPlannerService`
3. planner 输出 `ConversationPlan`
4. `AgentExecutionService` 根据 `plan.mode` 执行
5. assistant 消息持久化 `responseKind + routing metadata`
6. 前端恢复会话时读取结构化类型，而不是依赖旧文案

## Error Handling

- planner 不抛“业务澄清阻断”异常
- 低置信问题进入 agent，由模型在上下文不足时主动追问
- streaming/non-streaming 共用 planner
- 老会话兼容旧 `OLD_CLARIFICATION_PREFIX`，但新消息不再依赖该协议

## Testing Strategy

重点覆盖：

- 库表探索请求不再走业务澄清短路
- 模糊业务问题不再被系统级拦截，而是进入 agent
- 问候 / 产品说明仍走直接回复
- 模板命中仍保留快路径
- streaming 与 non-streaming 行为一致
- 会话恢复优先使用 `responseKind`

## Rollout

本次重构只改对话编排，不改工具协议和 analytics 对外 API。数据库仅新增 `ai_chat_message.response_kind` 字段，旧数据保持兼容。
