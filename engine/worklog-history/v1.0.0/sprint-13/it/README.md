# Sprint-13 IT 资产

本目录现在提供两类可执行核验：

1. `test_elt_guardrails.sh`
   - 运行 `ER-03~ER-07` 的关键 Maven 测试
   - 可选尝试 live ELT monitor smoke
   - 默认不强依赖本地 `8092` 已开启 `dts.elt.enabled=true`

2. `verify_warehouse_layers.sh`
   - 直接连接 `copilot_analytics` 库
   - 核验当前物理落表是否仍为 `mart/fact + watermark`
   - 核验 `business_view_registry` 中的视图层元数据仍存在
   - 明确当前未进入 `dwd/dws/ads` 分层

## 运行方式

### 1. Guardrail 回归

```bash
bash worklog/v1.0.0/sprint-13/it/test_elt_guardrails.sh
```

可选开启 live ELT monitor smoke：

```bash
ELT_HTTP_SMOKE=on \
COPILOT_ADMIN_SECRET=change-me-in-production \
bash worklog/v1.0.0/sprint-13/it/test_elt_guardrails.sh
```

说明：

- `ELT_HTTP_SMOKE=auto` 时，脚本会尝试访问 `/api/analytics/elt/status`
- 如果本地 `analytics` 没有以 `dts.elt.enabled=true` 重启，脚本会明确输出 `SKIP`
- live smoke 默认使用 `copilot-ai` 管理口临时生成的 API key

### 2. 数仓分层核验

```bash
PG_PORT=15432 \
bash worklog/v1.0.0/sprint-13/it/verify_warehouse_layers.sh
```

可覆盖的环境变量：

- `PG_HOST`
- `PG_PORT`
- `PG_DB`
- `PG_USER`
- `PG_PASSWORD`
- `PG_SCHEMA`
- `POSTGRES_JDBC_JAR`

## 当前结论

- 物理层已存在：
  - `mart_project_fulfillment_daily`
  - `fact_field_operation_event`
  - `elt_sync_watermark`
- 逻辑视图层仍通过 `business_view_registry` 维护
- 当前没有 `dwd_*`、`dws_*`、`ads_*`
- Sprint-13 的策略是继续维持 `view registry + mart/fact` 轻量双层模型，而不是立刻升级到标准数仓四层
