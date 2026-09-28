# T02: 确立治理层为 SoT 并定义口径主数据模型

**优先级**: P0
**状态**: READY
**依赖**: T01

## 目标

裁定 dts-platform 治理层（`modeling` + glossary）为口径单一事实源，并定义一套能容纳 9 条口径铁律的口径主数据模型，使 pack 与 dbt 都能从它派生。

## 技术设计

口径主数据模型（落在治理层，最小四类）：
1. **业务对象**：表/视图 + 业务语义（如 月对账 a_month_accounting）。
2. **属性/列口径**：三级金额列语义（名义/应收折前/折后实收/已回款）、varchar 须 CAST 标记。
3. **口径规则**：枚举归属（biz_type 属于哪张表的哪套枚举）、结算链归属（bizType→a_month_accounting vs a_sale_account）、双重计数去重（source_type=8）、JSON 关联不可等值 JOIN 等——表达为"约束 + 适用对象 + 违反示例"。
4. **指标定义**：可发布指标的口径公式与依赖对象（接 sprint-29 指标联邦）。

落点决策（T04 ADR 锁定）：对象/属性/指标定义 → dts-platform `modeling`；业务术语/同义词 → OpenMetadata Glossary。复用 `CatalogDomain`（自由参数化数据行）作为域归属轴。

## 影响范围

- dts-platform `modeling` 可能需扩字段/表承载"口径规则"（如无现成承载体）
- 产出 `assets/caliber-master-data-model.md`（schema + 字段说明 + 与 9 铁律映射）
- ⚠️ 改 dts-platform `modeling` symbol 前，按其 CLAUDE.md 先跑 `gitnexus_impact`

## 验证

- [ ] 9 条铁律每条都能在主数据模型里找到承载位置
- [ ] finance/procurement 两域现有口径可无损表达进该模型（纸面演练）

## 完成标准

- [ ] 主数据模型成文并评审通过，作为 T03 编码与 F2 导出的目标 schema
