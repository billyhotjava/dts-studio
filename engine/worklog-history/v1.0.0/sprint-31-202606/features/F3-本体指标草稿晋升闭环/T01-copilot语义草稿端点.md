# T01: copilot 语义草稿端点

**优先级**: P1
**状态**: READY
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

## 验证

- [ ] 三类草稿均可结构化产出且 schema 校验通过
- [ ] 草稿不触达 SoT / 业务库（仅暂存 + 调 draft 接口）

## 完成标准

- [ ] 草稿端点可用，输出可被 T02 写入治理层 draft
