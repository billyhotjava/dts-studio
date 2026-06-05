# T02 注册 copilot 联邦查询数据源

## 状态

DONE

## 实施要点

- analytics Liquibase 增加 `0066_trino_federated_query_database.xml`。
- 新增数据库名为 `联邦查询入口`,engine 为 `trino`,role 为 `BUSINESS_PRIMARY`。
- `details_json` 挂载 `prs.flowerbiz.federated` 与 `prs.flowerbiz.trino` 别名。

## 验收

- `master.xml` 在 0065 后 include 0066。
- seed 中没有明文密码。
- `AnalyticsDatabaseAliasResolver` 将 `prs.flowerbiz.federated` 优先解析到 Trino 联邦入口。
