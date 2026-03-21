# T01: 草稿回跳 Copilot 会话

**优先级**: P1
**状态**: READY
**依赖**: 无

## 目标

让从 Copilot 生成的草稿可以从查询页返回原会话。

## 技术设计

- 使用 `session_id` / `message_id`
- 在查询页来源上下文中增加回跳动作

## 影响范围

- `CardEditorPage.tsx`
- `CopilotChat.tsx`

## 验证

- [ ] `node --experimental-strip-types --test dts-copilot-webapp/tests/queryDraftHandoff.test.ts`

## 完成标准

- [ ] 草稿可明确回到来源会话
