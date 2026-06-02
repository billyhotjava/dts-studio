# F6 联邦查询 Catalog 约定

## 入口

- dts-copilot 数据库名:`联邦查询入口`
- engine:`trino`
- 逻辑别名:
  - `prs.flowerbiz.federated`
  - `prs.flowerbiz.trino`

## Trino Catalog

| Catalog | 数据源 | Schema | 说明 |
|---------|--------|--------|------|
| `postgres` | dts-stack PG 数仓 | `public` | dbt/ADS/DWD 结构化仓库 |
| `mysql` | 旧业务 MySQL | `rs_cloud_flower` | adminapi 业务源库 |

## 运行配置

- `TRINO_PG_USER` / `TRINO_PG_PASSWORD`:默认复用 `PG_USER_BIADMIN` / `PG_PWD_BIADMIN`。
- `TRINO_MYSQL_USER` / `TRINO_MYSQL_PASSWORD`:用于连接 `docker_huahui` 网络里的旧 MySQL;不得写入 catalog 文件。
- 当前验证通过的 `dts-trino` 容器已在启动命令中临时注入 `TRINO_MYSQL_PASSWORD`;若后续重新 `up --force-recreate dts-trino`,需要由运维环境或 `.env` 提供该变量。

## SQL 约定

- 表必须写全:`catalog.schema.table`。
- 跨 catalog Join 必须带 `WHERE` 或 `LIMIT`。
- 默认示例只读,用于验证真实跨库 Join:
  - `postgres.public.xycyl_dwd_flowerbiz_main`
  - `mysql.rs_cloud_flower.t_flower_biz_info`
