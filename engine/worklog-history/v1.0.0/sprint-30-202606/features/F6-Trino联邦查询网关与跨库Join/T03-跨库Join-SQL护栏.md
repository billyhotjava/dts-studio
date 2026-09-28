# T03 跨库 Join SQL 护栏

## 状态

DONE

## 实施要点

- 仅对 `engine=trino` 或 `details_json.federatedQuery=true` 的库启用。
- native SQL 必须使用 `catalog.schema.table`。
- 出现跨 catalog Join 时必须带 `WHERE` 或 `LIMIT`。
- 返回来源元数据,便于前端和审计看到 `engine=trino` 与参与 catalogs。

## 验收

- 合规 SQL 通过。
- 未加 catalog 的表名被拒绝。
- 无过滤/limit 的跨 catalog Join 被拒绝。
