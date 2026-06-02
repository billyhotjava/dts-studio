# T04 示例 SQL 与语义入口

## 状态

DONE

## 实施要点

- 提供 `it/sql/f6_trino_federated_join.sql` 作为真实跨库 Join 样例。
- 在资产目录登记 Trino 联邦入口与 catalog 命名约定。

## 验收

- 示例 SQL 同时引用 `postgres.public` 与 `mysql.rs_cloud_flower`。
- SQL 使用 `JOIN` 并带 `LIMIT`。
