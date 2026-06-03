# T02: 草稿写入治理层 draft 状态

**优先级**: P1
**状态**: READY
**依赖**: T01, F2-T01

## 目标

把 copilot 草稿写入治理层的 draft 状态，复用 dts-platform `modeling` 已有的 review/version 机制，使草稿进入正式治理流程而非游离在 copilot。

## 技术设计

- 复用治理层现有承载体：`ModelingGlossaryTerm` + `ModelingGlossaryTermReview` + `ModelingGlossaryTermVersion`、`ModelingPlan` + `ModelingPlanReview` + `ModelingPlanVersion`、`DataStandard` + 版本/状态——它们已有 draft/review/version 三件套。
- copilot 经治理层写入 API 提交 draft，标注来源 = copilot、附触发问题与证据，落 `DRAFT` 状态。
- 域归属用 `CatalogDomain`（多场景就绪）。
- ⚠️ 涉及改 dts-platform modeling，按其 CLAUDE.md 先跑 `gitnexus_impact`，HIGH/CRITICAL 风险先报。

## 影响范围

- dts-platform：草稿接收/写入端点（若现有 review 接口够用则仅适配）
- `dts-copilot-ai`：草稿提交客户端

## 验证

- [ ] copilot 草稿成功落治理层 DRAFT，可在治理界面/接口看到
- [ ] 草稿不影响正式 SoT（DRAFT 不被 F2 sync 下发）

## 完成标准

- [ ] 草稿进入治理层 draft 流，等待 T03 晋升
