# T01: copilot 语义草稿端点

**优先级**: P1
**状态**: DONE
**依赖**: F1-T02

## 目标

在 dts-copilot 提供"语义草稿"能力：当 agent 发现用户问题需要某个尚未定义的对象/指标/口径规则时，产出结构化草稿供晋升，而不是就地编造 SQL。

## 技术设计

- 新增草稿端点（如 `POST /api/copilot/semantic-drafts`），入参为草稿类型（object/indicator/caliber-rule）+ 结构化内容（对齐 F1-T02 主数据模型）+ 触发问题与证据。
- 草稿来源两路：
  1. **人工**：用户/分析师在 agent 对话中显式"建议补一个指标"。
  2. **自动信号**：路由落到 Tier C（直连明细）或频繁 miss 的问题 → agent 生成草稿候选（Sprint-32 telemetry 接入后增强）。
- 草稿仅落本地暂存 + 调治理层 draft（T02），**绝不直改 SoT / 不直写业务库**（沿用 sprint-26 决策 #3）。

## 影响范围

- `dts-copilot-ai`：草稿端点 + DTO + 暂存
- 关联 `AssetBackedPlannerPolicy`（miss 分支可触发草稿建议）

## 当前进展

- 已新增 `SemanticDraftService` 本地暂存 contract，支持 `object` / `indicator` / `caliber-rule` 三类语义草稿。
- 已新增 `POST /api/copilot/semantic-drafts` 端点，返回 `LOCAL_STAGED` / `REJECTED`、`NOT_SUBMITTED`、`SUBMIT_TO_GOVERNANCE_DRAFT` 与不触达 SoT/业务库标记。
- 2026-06-05：已补 `POST /api/copilot/semantic-drafts/{draftId}/submit` 入口，T01 输出可交给 T02 的治理 draft 提交 contract。

## 验证

- [x] 三类草稿均可结构化产出且 schema 校验通过：`mvn -q -pl dts-copilot-ai -Dtest=SemanticDraftServiceTest,SemanticDraftResourceTest test`
- [x] 草稿不触达 SoT / 业务库：服务无仓储依赖，响应中 `sotTouched=false`、`businessDatabaseTouched=false`

## 完成标准

- [x] 草稿端点可用，输出可被 T02 写入治理层 draft contract
