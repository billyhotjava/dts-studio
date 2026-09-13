# T03: Chat/stream 响应契约接入

**优先级**: P0
**状态**: DONE
**依赖**: T02

## 目标

让同步 chat 和 SSE done 事件都返回 `accuracyEvidence`，前端无需解析文本判断可信度。

## 技术设计

- 同步响应：在现有 templateCode、targetView、responseKind 旁追加 `accuracyEvidence`。
- 流式响应：done event 追加同名字段，兼容旧前端字段缺失。
- 错误响应：保留错误文本，同时 evidence 标为 `UNTRUSTED` 并写入错误原因。

## 影响范围

- Internal/public chat resource。
- stream event builder。
- `useCopilotStream` 消费契约。

## 验证

- [x] 同步接口返回 evidence。
- [x] SSE done event 返回 evidence。
- [x] 老字段仍保持向后兼容。

## 完成标准

- [x] 前后端 contract test 能证明 evidence 出现在 NL2SQL 完成事件中。
