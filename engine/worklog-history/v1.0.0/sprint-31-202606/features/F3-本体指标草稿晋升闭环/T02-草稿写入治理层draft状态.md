# T02: 草稿写入治理层 draft 状态

**优先级**: P1
**状态**: DONE
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

## 当前进展

- 已只读确认 dts-platform 现有 `POST /api/semantic/business-objects`、`POST /api/governance/indicators`、`POST /api/modeling/standards` 均具备 DRAFT 创建承载面；platform 仓库当前未被 GitNexus 索引，暂不直接改 platform modeling。
- 已新增 copilot 侧 `SemanticDraftGovernanceSubmissionService`，将 `object` / `indicator` / `caliber-rule` 草稿分别映射到现有治理 DRAFT API。
- 已新增 `PlatformSemanticDraftClient` 与配置 `copilot.platform.semantic-draft.*`，提交时带服务头或 bearer token；提交失败时本地保持 `NOT_SUBMITTED`。
- 已新增 `POST /api/copilot/semantic-drafts/{draftId}/submit`，把本地暂存草稿提交到治理 draft contract。
- 2026-06-05：提交成功后本地暂存草稿回写 `governanceStatus=DRAFT_SUBMITTED`、platform draft id/status、目标 API 与 `WAIT_FOR_GOVERNANCE_REVIEW`，避免仅返回一次性提交结果。
- 2026-06-05：live 联调打通 `dts-copilot-ai -> dts-platform` 服务账号写入链路；指标草稿 `finance` 本地域映射到平台 `CatalogDomain.code=S10-FIN` 后，治理层生成 `DRAFT` 指标。
- 2026-06-05：补平台侧 `GovIndicatorDefinition` jsonb 字段 Hibernate JSON 绑定，修复 `dependency_indicators` 等 jsonb 列被按 varchar 写入导致的 live 500。

## 验证

- [x] copilot 草稿提交请求映射为治理层 `DRAFT` payload：`mvn -q -pl dts-copilot-ai -Dtest=SemanticDraftGovernanceSubmissionServiceTest,SemanticDraftResourceTest,PlatformSemanticDraftClientTest test`
- [x] 草稿提交失败时保持 `NOT_SUBMITTED`，不影响正式 SoT
- [x] 草稿提交成功后回写本地状态，可追踪 platform draft id/status
- [x] live platform 联调：`codex_sprint31_live_finance_1780635038` 可由平台治理指标接口读回，`domain=S10-FIN,status=DRAFT,version=draft`
- [x] 平台 jsonb 持久化回归：`./mvnw -q -Dtest=GovIndicatorDefinitionJsonbMappingTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 完成标准

- [x] 草稿进入治理层 draft 流，等待 T03 晋升
