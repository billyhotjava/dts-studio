# T05 IT 证据与运行态验证

## 状态

DONE

## 实施要点

- 新增 `test_f6_trino_federated_join.sh`。
- 静态检查 compose、catalog、analytics 依赖、Liquibase、护栏测试和示例 SQL。
- `RUN_LIVE=1` 时执行 Trino `/v1/info`、`SHOW CATALOGS` 和跨库 Join。

## 验收

- 生成 `evidence/20260601-local/f6-trino-federated-join-summary.md`。
- live 模式能证明 Join 由 Trino 执行。
