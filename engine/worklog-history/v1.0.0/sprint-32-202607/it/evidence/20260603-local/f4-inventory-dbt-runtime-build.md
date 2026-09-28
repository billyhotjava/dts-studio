# F4/T01 库存域 runtime dbt build 证据

**日期**: 2026-06-03
**环境**: local dts-stack / dts-copilot

## ODS 准备

当前 dbt 镜像只有 `postgres` adapter，不能直接用 dbt-trino 从 `mysql.rs_cloud_flower.s_stock_info` 读取。因此本轮按 ODS 导入链路先把库存余额表落到 Postgres：

```text
public.ods_ptr_mysql_s_stock_info
```

导入字段覆盖库存模型需要的 16 个字段，MySQL 客户端使用 `--default-character-set=utf8mb4` 避免中文字段编码错误。

导入结果：

```text
COPY 11661
stock_rows=11661
active_rows=11661
```

价格主数据使用已有 ODS：

```text
public.ods_ptr_mysql_b_goods_price
```

## dbt build

使用临时 dbt project 加载库存生成套件里的模型：

```bash
docker run --rm --network dts-core \
  -v "$runtime_dir:/opt/dbt" \
  -v "$runtime_dir/profiles:/root/.dbt" \
  -v /opt/dts/logs/dbt:/opt/dbt-logs \
  -e DBT_USE_EXPERIMENTAL_PARSER=false \
  -e DBT_LOG_PATH=/opt/dbt-logs \
  dts-dbt:1.10.0 build \
    --project-dir /opt/dbt \
    --profiles-dir /root/.dbt \
    --log-path /opt/dbt-logs \
    --target dev \
    --threads 1 \
    --vars '{inventory_stock_info_relation: public.ods_ptr_mysql_s_stock_info, inventory_goods_price_relation: public.ods_ptr_mysql_b_goods_price}' \
    --select +inventory_ads_overview +inventory_ads_low_stock_alert
```

结果：

```text
Found 6 models, 1 operation, 7 data tests
inventory_stg_goods_price ........ SELECT 6517
inventory_stg_stock_info ......... SELECT 11661
inventory_dwd_stock_balance ...... SELECT 11661
inventory_dws_stock_monthly ...... SELECT 11660
inventory_ads_overview ........... SELECT 11660
inventory_ads_low_stock_alert .... SELECT 9971
Done. PASS=12 WARN=2 ERROR=0 SKIP=0 NO-OP=0 TOTAL=14
```

## 数据质量提示

两个 warn 均来自同一类质量问题：

```text
not_null_inventory_stg_stock_info_good_price_id => WARN 1
not_null_inventory_dwd_stock_balance_good_price_id => WARN 1
```

说明源库存余额中存在 1 条缺 `good_price_id` 的记录。该问题不阻塞建模，但后续报表需要保留“缺价格主数据记录数”质量提示。

## 结论

库存域 STG/DWD/DWS/ADS 已在运行态成功 build 到 `biadmin.public`，满足 Sprint-32 F4 的 runtime build 证据要求。
