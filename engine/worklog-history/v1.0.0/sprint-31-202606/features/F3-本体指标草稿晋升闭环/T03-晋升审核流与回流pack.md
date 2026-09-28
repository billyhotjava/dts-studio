# T03: 晋升审核流 + 回流 pack

**优先级**: P1
**状态**: DONE
**依赖**: T02

## 目标

打通"草稿 → 人审晋升 → 正式定义 → 经 F2 sync 回流 pack"的最后一段，使整条 发现→定义→治理→消费 闭环可端到端跑通一例。

## 技术设计

- 晋升走治理层现有 review/approve（modeling review 机制），审通过后 DRAFT → 正式版本，进入 SoT。
- 正式化后由 F2-T02 生成器在下次 sync 时纳入 pack 生成区，回流到 agent。
- 闭环演练用例：选一个 finance/procurement 域真实缺失的指标或口径规则，走完 草稿→审→正式→回流，agent 之后能用上。
- 记录闭环时延（草稿到回流），作为流程基线。

## 影响范围

- dts-platform：晋升/approve 流（复用现有）
- `dts-copilot-ai`：回流由 F2 sync 承接，本任务验证端到端

## 进展

- 2026-06-05：新增 `POST /api/copilot/platform-indicators/sync` 与 `GET /api/copilot/platform-indicators/status`，可在治理维护者发布后手动触发平台 PUBLISHED 指标回流，返回 `fetched/added/updated/removed/caliberChangedCodes/stale` 作为可留证状态。
- 2026-06-05：live 验证 Copilot 服务身份无法调用平台 `/api/governance/indicators/{id}/publish`（HTTP 401），发布仍保留在治理维护者权限内，避免 Copilot 代审/直发。
- 2026-06-05：live 验证平台 DRAFT 草稿未进入 Copilot 回流目录；平台 PUBLISHED=1，Copilot `/sync` 返回 `fetched=1,catalogEntryCount=1`。
- 2026-06-05：治理维护者发布 `codex_sprint31_live_finance_1780635038` 后发现 Copilot 服务读取缺少机构上下文，平台列表只返回全局/root 指标；按 TDD 增加 `DTS_PLATFORM_ACTIVE_DEPT`，配置为 `1502` 后 `/sync` 返回 `fetched=2,catalogEntryCount=2`，Agent 命中 `PUBLISHED_INDICATOR`。

## 验证

- [x] 一例草稿走完全流程并在 agent 回答中体现（2026-06-05 live：`it/evidence/20260605-local/semantic-draft-backflow-sync-complete.md`）
- [x] 未审通过的草稿不进入 pack（2026-06-05 live：`it/evidence/20260605-local/semantic-draft-backflow-sync-partial.md`）

## 完成标准

- [x] 闭环端到端跑通一例并留证据（入 F5/IT）
