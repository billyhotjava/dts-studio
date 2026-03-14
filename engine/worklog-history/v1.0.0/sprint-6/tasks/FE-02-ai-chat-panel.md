# FE-02: AI 聊天面板组件合并

**状态**: READY
**依赖**: FE-01

## 目标

从 dts-platform-webapp 和 dts-analytics-webapp 合并 AI 聊天组件到 dts-copilot-webapp。

## 技术设计

### 来源组件

**从 dts-platform-webapp:**
- `src/components/ai/AiChatPanel.tsx` — 主聊天面板
- `src/components/ai/AiChatButton.tsx` — 浮动按钮（FAB）
- `src/components/ai/ToolCallCard.tsx` — Tool 调用可视化
- `src/components/ai/ApprovalCard.tsx` — 审批动作 UI
- `src/components/ai/AgentMessage.tsx` — Agent 消息渲染
- `src/components/ai/UserMessage.tsx` — 用户消息渲染
- `src/api/ai-copilot.ts` — Copilot API 客户端
- `src/api/ai-agent.ts` — Agent API 客户端

**从 dts-analytics-webapp:**
- `src/components/copilot/CopilotChat.tsx` — 分析专用聊天
- `src/components/copilot/CopilotSidebar.tsx` — 会话侧边栏
- `src/components/copilot/TracePanel.tsx` — 执行追踪面板

### 合并策略

保留两套组件但统一 API 调用：
1. 全局 AI 聊天面板（AiChatPanel）— 通用对话，可在任何页面使用
2. Copilot 侧边栏（CopilotSidebar）— BI 场景专用，嵌入查询页面

## 影响文件

- `dts-copilot-webapp/src/components/ai/`（新建目录，复制组件）
- `dts-copilot-webapp/src/api/copilotAiApi.ts`（新建：统一 API 客户端）

## 完成标准

- [ ] AI 聊天按钮在全局布局中可见
- [ ] 点击打开聊天面板
- [ ] 可发送消息并接收流式响应
- [ ] Tool 调用结果正确渲染
- [ ] 会话历史正确展示
