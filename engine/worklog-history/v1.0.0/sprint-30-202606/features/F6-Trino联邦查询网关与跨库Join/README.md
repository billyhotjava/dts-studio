# F6 Trino 联邦查询网关与跨库 Join

## 目标

按方案 B 恢复真实跨库 Join:用 dts-stack 的 `dts-trino` 同时连接结构化数仓 PG 与业务 MySQL,让 dts-copilot 的 `prs.flowerbiz.federated` 指向 Trino 联邦入口,而不是在应用内拼接多库结果。

## 范围

- dts-stack 恢复 `dts-trino` 单节点 coordinator。
- Trino 挂载 `postgres` 与 `mysql` catalog,凭据只从环境变量读取。
- dts-copilot analytics 增加 `联邦查询入口` 数据库登记。
- Dataset native SQL 增加联邦查询护栏与来源元数据。
- IT 脚本覆盖静态配置、Java 测试和可选 live 跨库 Join。

## 任务

| Task | 标题 | 状态 |
|------|------|------|
| T01 | 恢复 dts-trino 服务与 catalog | DONE |
| T02 | 注册 copilot 联邦查询数据源 | DONE |
| T03 | 跨库 Join SQL 护栏 | DONE |
| T04 | 示例 SQL 与语义入口 | DONE |
| T05 | IT 证据与运行态验证 | DONE |

## 完成标准

- `dts-trino` 服务可启动并通过 `/v1/info` 探活。
- `SHOW CATALOGS` 能看到 `postgres`、`mysql`。
- F6 示例 SQL 能在 Trino 执行真实跨 catalog Join。
- `prs.flowerbiz.federated` 优先解析到 `联邦查询入口`。
- `test_f6_trino_federated_join.sh` 可重跑并生成 evidence。
