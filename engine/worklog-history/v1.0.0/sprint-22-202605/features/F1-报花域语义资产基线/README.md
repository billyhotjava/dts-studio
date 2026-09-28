# F1: 报花域语义资产基线

**优先级**: P0
**状态**: READY
**轨**: 1（dts-copilot 智能层）

## 目标

把 F0 产出的报花业务知识沉淀成 LLM 可识别的语义资产：`flowerbiz.json` 语义包 + `BizEnumDictionary` 字典 + `analytics_table/field` 元数据。**短期方案**，等 F4 OpenMetadata Glossary 落地后由其派生。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 报花 source 视图盘点与口径锁定 | P0 | READY | F0-T02, F0-T03 |
| T02 | flowerbiz.json 语义包落地 | P0 | READY | T01 |
| T03 | 报花字段 display 语义补齐 | P0 | READY | T02 |
| T04 | 报花枚举字典扩充（status / bizType / recovery_type / change_type）| P0 | READY | T01 |

## 完成标准

- [ ] `flowerbiz.json` 覆盖 8+ 业务对象（对应 F4 的 8 张 ads mart）+ 30+ 同义词 + 10+ fewShots + 8 guardrails
- [ ] `BizEnumDictionary` 增补 4 类报花枚举（共约 30 个值）
- [ ] `analytics_table` / `analytics_field` 报花相关行有中文 label + synonyms（短期）
- [ ] guardrails 显式禁止：跨 bizType 直接 SUM、跨 biz_category 混合聚合、用 create_time 作业务时间
