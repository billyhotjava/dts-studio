# MiniMax Anthropic Provider Design

**Goal:** 让 MiniMax 官方推荐的 Anthropic 兼容接口在 DTS Copilot 中稳定工作，并保留未来接入更多协议的扩展口。

## Problem
当前 AI provider 运行时统一走 `OpenAiCompatibleClient`。当 provider 配成 `https://api.minimaxi.com/anthropic` 时，系统仍按 OpenAI 协议调用 `/models`、`/chat/completions` 并按 OpenAI SSE 解析，导致对话已部分返回但最终不能正确结束，前端 watchdog 误报超时。

## Approach
引入协议层抽象，而不是继续在 `OpenAiCompatibleClient` 上堆条件分支。

- 新增统一接口 `LlmProviderClient`
- 保留现有 `OpenAiCompatibleClient` 作为 OpenAI-compatible 实现
- 新增 `AnthropicCompatibleClient` 处理 MiniMax Anthropic 兼容协议
- 新增 `LlmProviderClientFactory`，根据 `providerType` 和 `baseUrl` 选择协议实现
- `AiConfigService`、`AiConfigResource`、`AgentExecutionService`、`LlmGatewayService` 均通过工厂拿 client
- 增加 `MINIMAX` 模板，并将其默认协议指向 Anthropic 兼容地址

## Routing Rules
优先使用显式 `providerType`：
- `MINIMAX`、`ANTHROPIC` -> Anthropic-compatible
- 其他已知 provider -> OpenAI-compatible
- `CUSTOM` 或未知类型 -> 根据 `baseUrl` 推断；若路径包含 `/anthropic`，则走 Anthropic-compatible，否则走 OpenAI-compatible

## Streaming Behavior
Anthropic-compatible 流式输出在后端被归一化为现有 `StreamingChatResult`，保证前端不需要改交互协议。完成事件必须明确结束，避免 `CopilotChat` 的 idle watchdog 误报超时。

## Testing
先写失败测试，再实现：
- MiniMax/Anthropic provider 走 Anthropic-compatible client
- Anthropic stream 能正确聚合文本并结束
- `AiConfigService.testProvider` 对 MiniMax 使用 Anthropic models endpoint
- `AgentExecutionService` / `LlmGatewayService` 在 MiniMax default provider 下正常选路
