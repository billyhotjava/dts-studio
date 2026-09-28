# 空白域建模 Onboarding Checklist

本清单把 Sprint-30 财务域切片抽象成后续库存、督导、薪资域可复用流程。每个空白域必须走完"事实源 → ODS → dbt → 语义包 → 对账 → IT 证据"闭环后才能标 DONE。

| 阶段 | 必做项 | 财务域样例 | 库存域纸面演练 |
|------|--------|------------|----------------|
| 1. 源表盘点 | 列出主表、明细表、状态/枚举、金额列、JSON 列、跨域软外键 | `a_month_accounting`、`a_green_accounting`、`a_invoice_*`、`a_collection_*` | `s_stock_info`、`s_stock_item`、`t_warehousing_*`、`t_ex_warehouse_*`、`b_goods_price` |
| 2. 口径陷阱 | 对照业务地图 §3，识别同名字段多义、金额层级、重复计数、历史覆盖 | 两条结算链、月对账三级金额、`source_type=8` | 加权平均成本非 FIFO、`good_price_id` 不是 `good_name`、出入库类型独立枚举 |
| 3. ODS 补全 | Addax JSON 覆盖所有源表，多 tenant，密码外置，入湖列保持原始 | F2 生成 14 张缺失源表 × 5 tenant | 库存域先确认 `s_stock_info`、`s_stock_item`、`t_warehousing_*`、`t_ex_warehouse_*` 均有 ODS |
| 4. STG | 只做类型转换、占位符归 NULL、字段改名，不做业务派生 | `xycyl_stg_finance_*` | `xycyl_stg_inventory_stock` / `xycyl_stg_inventory_io_*` |
| 5. DWD | 展开枚举、统一业务键、落实口径护栏 | 月对账三级金额、开票/收款业务类型、`source_type=8` 标记 | 库存成本口径、入/出库类型、SKU 软外键质量 |
| 6. DWS/ADS | 发布业务可读宽表，字段使用客户/Excel 语言 | 月对账、开票进度、收款明细三张 ADS | 库存余额、出入库流水、库存周转/异常 |
| 7. 语义包 | 对象、synonyms、fewShots、guardrails 指向权威 ADS | `finance.json` 三个对象 | 新建/扩展 `inventory` semantic-pack |
| 8. 回归测试 | 为高危问句建 TSV/JUnit/脚本断言，不允许静默混口径 | `caliber-regression-questions.tsv` | "库存成本按物品名称汇总" 必须转为 SKU/good_price_id |
| 9. 对账 | 与 adminweb 内建报表或业务库 SQL 对账，登记误差和差异原因 | `f4_finance_adminweb_reconciliation.sql` | 库存台账/出入库列表与 ADS 对账 |
| 10. 证据包 | `it/README.md` 矩阵全绿，证据可重跑，脚本可独立执行 | F1-F5 evidence | 库存域复制本 sprint IT 目录结构 |

## 交付门槛

- `assets/*-source-catalog.md` 或等价源表盘点成文。
- 所有 ODS Addax JSON 无明文凭据，且覆盖清单缺口为 0。
- 至少一条高危业务口径进入 semantic-pack guardrails，并有回归问题。
- ADS 字段使用业务可读中文，不暴露 SQL 内部命名给默认问答。
- 对账 SQL 可重跑；如当前环境无法产数，必须保留可执行 SQL 与阻断原因。

## 库存域纸面结论

库存域下一步可从 `s_stock_info` + `s_stock_item` + `t_warehousing_info/item` + `t_ex_warehouse_info/item` + `b_goods_price` 开始。核心护栏是 `CAL-INVENTORY-COST`: `s_stock_info.out_cost` 为加权平均成本，出入库和库存必须按 `good_price_id`/SKU 关联，不能按 `good_name` 汇总。
