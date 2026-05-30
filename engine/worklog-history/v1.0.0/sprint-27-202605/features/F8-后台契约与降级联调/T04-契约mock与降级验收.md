# T04: 契约 mock 与降级验收

**优先级**: P0
**状态**: READY
**依赖**: T01,T02,T03

## 目标

为 F5/F6 提供可重复的 mock 契约验证，并把真实后台未就绪时的降级行为纳入 IT 证据，防止把 mock 通过误标为生产链路通过。

## 技术设计

新增契约 fixture：

- 高置信 `done`：含 `assumptions` / `confidence >= 0.6` / `trace`
- 低置信 `done`：含 `confidence < 0.6` / `clarifications`
- 重算 `done`：含更新后的 `assumptions` / 新 SQL / 同一 artifact id
- 溯源缺失 `done`：仅有 `generatedSql` / `sourceRefs`，验证降级

验收分层：

- **Mock Contract**：前端组件和事件处理能消费新契约。
- **Degraded Runtime**：真实后台未返回字段时，UI 不报错并提示/隐藏高级能力。
- **Live Contract**：真实后台返回新字段，IT05/IT06/IT09 才能标为完整通过。

## 影响范围

- `dts-copilot-webapp/src/components/copilot/**.test.ts`
- `dts-copilot-webapp/src/api/modules/copilot.ts` 的测试 fixture
- `worklog/v1.0.0/sprint-27-202605/it/README.md`

## 验证

- [ ] mock 高置信契约驱动口径芯片显示。
- [ ] mock 低置信契约驱动澄清芯片显示。
- [ ] mock trace 契约驱动溯源面板完整显示。
- [ ] 真实后台缺字段时 IT 记录为 degraded，而不是失败或假通过。

## 完成标准

- [ ] F5/F6 的高级能力都有 mock 契约测试。
- [ ] `it/README.md` 明确区分 mock、degraded、live 三类证据。
- [ ] 后台契约未完成时，P1a 仍可上线基础问数；P1b 完整验收必须等 F8 live contract 通过。
