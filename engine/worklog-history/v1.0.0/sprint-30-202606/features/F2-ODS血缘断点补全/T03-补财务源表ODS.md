# T03: 补财务垂直切片源表 ODS

**优先级**: P0
**状态**: DONE
**依赖**: F1

## 目标

把财务回款/开票链(F4)所需源表全部纳入 ODS 同步,作为财务垂直切片建模的数据底座。

## 技术设计

源表清单(多 tenant 同步):
- `a_month_accounting` / `a_green_accounting`(租摆链月对账主表+明细,含三级金额列)
- `a_flower_biz_accounting`(报花业务对账汇总)
- `a_sale_account` / `a_sale_account_rent_item`(售/赠/坏链,注意 rent 为 varchar)
- `a_invoice_info` / `a_invoice_item` / `a_invoice_record`(开票)
- `a_collection_record` / `a_collection_item`(收款)

- ODS 建表对齐源 DDL,完整保留金额列(不在 ODS 做口径换算,换算留给 dbt)。
- 注意 `biz_ids_json` 等 JSON 文本列原样落地,展开留给 dwd。

## 影响范围

- `dts-stack/services/dts-airflow/dags/`;ODS 建表脚本

## 验证

- [x] 上述表全部生成 5 tenant Addax JSON 并纳入覆盖矩阵
- [x] varchar/JSON 列按 ODS 文本落地约定保留,精度换算留给 STG/dbt

## 完成标准

- [x] 财务回款/开票链源表全部进 ODS,F4 可开工
