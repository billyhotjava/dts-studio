# F2/T01 Trino MySQL 访问策略配置门禁

**日期**: 2026-06-03
**范围**: dts-stack `docker-compose-app.yml` + Trino `mysql` catalog

## 背景

Sprint-32 F2/T01 要求联邦读业务 MySQL 不默认使用高权限账号，并优先走读副本或受限账号。当前 Trino `mysql` catalog 通过环境变量注入 JDBC URL、用户名和密码；因此需要先把 compose 默认值改为只读账号优先，再要求部署环境显式配置真实只读账号。

## 本次改动

`dts-stack/docker-compose-app.yml` 的 `dts-trino` 环境变量默认值调整为：

- 保留 `TRINO_MYSQL_*` 作为显式覆盖，兼容既有部署。
- 默认优先读取 `TRINO_MYSQL_READONLY_*`。
- 如果没有设置，只回退到 `trino_readonly`，不再回退到 `root`。
- 挂载 `resource-groups.properties` 与 `resource-groups.json`，使 Trino 查询进入统一资源组。
- `dts-stack/init.sh` 的 `.env` 生成源头也同步改为只读默认：先生成 `TRINO_MYSQL_READONLY_*`，再让 `TRINO_MYSQL_*` 默认继承只读变量，不再生成 `TRINO_MYSQL_USER=root`。

Trino catalog 仍保持：

```properties
connection-url=${ENV:TRINO_MYSQL_JDBC_URL}
connection-user=${ENV:TRINO_MYSQL_USER}
connection-password=${ENV:TRINO_MYSQL_PASSWORD}
```

这样 catalog 不保存明文账号，部署侧用 env 注入。

Trino 资源护栏默认值：

```properties
query.max-run-time=5m
query.max-execution-time=3m
query.max-planning-time=1m
query.max-scan-physical-bytes=2GB
query.max-length=100000
```

Resource group 默认限制：

- `hardConcurrencyLimit=4`
- `maxQueued=20`
- `softMemoryLimit=80%`

## 验证

```bash
bash worklog/v1.0.0/sprint-32-202607/it/test_f2_trino_mysql_access_policy.sh
```

结果：

```text
[f2] trino mysql access policy config gate PASS
```

脚本验证：

- compose 文件包含 `TRINO_MYSQL_READONLY_JDBC_URL`、`TRINO_MYSQL_READONLY_USER`、`TRINO_MYSQL_READONLY_PASSWORD`。
- compose 默认用户不再回退到 `root`。
- `init.sh` 默认生成 `TRINO_MYSQL_READONLY_USER=trino_readonly`，且不再包含 `TRINO_MYSQL_USER:=root`。
- Trino `mysql.properties` 仍通过 env 注入连接信息。
- `config.properties` 包含查询时长、执行时长、规划时长、扫描字节和 SQL 长度限制。
- `resource-groups.properties/json` 存在，并已挂载到 `/etc/trino/`。

额外运行态观察：

- `docker compose -f docker-compose-app.yml config` 可以正常解析新增挂载。
- 当前本地 `.env` 仍显式覆盖了 Trino MySQL 连接账号为高权限账号；默认只读门禁不会覆盖显式配置。因此运行态仍需切换只读账号后重建 `dts-trino`。

## 运行态验证

本轮已把本地 `dts-stack/.env` 的 `TRINO_MYSQL_READONLY_*` 切到 Copilot 数据源 15（`园林业务库`）对应的受限业务账号，并移除 schema 路径后重建 `dts-trino`。敏感值不落文档。

重启后 Trino 加载信息：

```text
dts-trino Up (healthy)
/v1/info => "starting": false
mysql catalog connection-url => jdbc:mysql://<readonly-host>:<port>
mysql catalog connection-user => <readonly-user>
```

Trino CLI 探针：

```text
[postgres] OK 1580
[mysql] OK 11661
[system] ERROR Access Denied: Cannot access catalog system
[write] ERROR Access Denied: Cannot create table mysql.rs_cloud_flower.__copilot_acl_probe
```

该探针证明：

- `postgres.public.xycyl_ads_flowerbiz_sale_summary` 可读。
- `mysql.rs_cloud_flower.s_stock_info` 可读。
- `system` catalog 被拒绝。
- MySQL catalog 写表被拒绝，联邦入口保持只读。

## 当前边界

当前 dts-stack 运行面没有独立 Ranger 服务，因此本地 Sprint-32 以 Trino file access-control 作为联邦治理策略源：限制可访问 catalog，并把 mysql/postgres/jmx 限定为只读 SELECT。企业部署如启用 Ranger，可把同一策略迁移到 Ranger 插件，保留本证据中的只读和 catalog 拒绝语义。

F2/T01 与本地 access-control 接线当前可标记 DONE；Ranger 行列脱敏的企业版策略仍应在有 Ranger runtime 的部署中继续验证。
