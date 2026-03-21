# T02: 采购域 semantic pack 与字段语义补齐

**优先级**: P0
**状态**: READY
**依赖**: T01

## 目标

让采购域不再以“裸表名 + 裸字段名”暴露给 LLM，而是具备稳定的业务语义。

## 技术设计

- 新增采购域 semantic pack，覆盖：
  - 采购明细
  - 采购人 / 发起人
  - 采购金额
  - 数量 / 规格 / 产品
  - 月度采购分析
- 为关键字段补齐 display / semantic 映射：
  - `good_name`
  - `good_specs`
  - `purchase_user_name`
  - `purchase_time`
  - `parchase_price`
  - `parchase_number`
  - `real_purchase_number`
- 固化问法别名：
  - `采购人`、`发起人`
  - `采购金额`、`金额`
  - `产品`、`物品`
  - `采购详细情况`、`采购明细`
- 明确精确匹配 `绿萝` 与模糊匹配 `%绿萝%` 的不同业务口径

## 影响范围

- `dts-copilot-ai/src/main/resources/semantic-packs/`
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/SemanticPackService.java`
- `copilot_analytics.analytics_table`
- `copilot_analytics.analytics_field`

## 验证

- [ ] semantic pack 能覆盖采购域核心问法和字段别名
- [ ] 关键采购字段不再完全依赖裸字段名理解

## 完成标准

- [ ] 采购域具备独立 semantic pack
- [ ] 采购域关键字段和问法映射被显式沉淀
