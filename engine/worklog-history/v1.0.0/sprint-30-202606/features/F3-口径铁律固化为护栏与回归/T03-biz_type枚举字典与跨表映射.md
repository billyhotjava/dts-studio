# T03: biz_type 枚举字典与跨表映射

**优先级**: P1
**状态**: BLOCKED
**依赖**: 无

## 目标

把 `biz_type`/`status` 等多表多义枚举整理成权威字典与跨表对照表,消除"同名字段不同义"导致的 JOIN/过滤错误。

## 技术设计

- 列出每张表的枚举:`t_flower_biz_info.biz_type`、`t_flower_biz_item.biz_type`、编号生成函数、`a_invoice_item.biz_type`、`a_collection_item.biz_type`、`t_warehousing.warehousing_type`、`t_ex_warehouse.out_house_type`、`a_green_accounting.source_type` 等。
- 标注每个值的中文含义与来源(DDL 注释 / Java 枚举 / VO getStatusName)。
- 沉淀为 dim 维度表设计(可选)或语义层参考文档,供 dbt dim_* 与 NL2SQL 同义词引用。

## 影响范围

- `assets/` 枚举字典文档;语义包同义词;dbt dim(可选)

## 验证

- [ ] 字典覆盖全部勘探发现的多义枚举,标明出处行号
- [ ] 与 T01 guardrail 交叉引用一致

## 完成标准

- [ ] biz_type 三套枚举对照字典成文,纳入语义层可被引用
