# F5 多场景接入手册与证据索引

**日期**: 2026-06-03
**范围**: Sprint-32 F5-T01/T02

## 产物

- `assets/multi-scenario-onboarding-guide.md`
- `it/test_f5_multi_scenario_guide.sh`

## 覆盖内容

- 软隔离 vs `deploy-per-scenario` 隔离选型。
- 新场景六个必交付物：CatalogDomain、dbt namespace、semantic pack、Trino catalog、glossary、routing map。
- Agent 五层路由接线：T1 指标、T2 mart/template、T3 本体对象图、T4 guardrail 联邦、T5 业务只读明细。
- Trino SQL 约束：`catalog.schema.table`、只读、授权 catalog、历史 PostgreSQL 方言只作运行时兜底。
- 库存域实战边界：已完成 ODS 导入、runtime dbt build 和 ADS 对账；后续产品化应把临时 runtime build 纳入正式 dbt 发布包。

## 验证

```bash
bash worklog/v1.0.0/sprint-32-202607/it/test_f5_multi_scenario_guide.sh
```

结果：

```text
[f5] multi-scenario guide and evidence index PASS
```

## 完成结论

- F2 已完成本地 Trino access-control 受限策略验证。
- F4 已完成库存 runtime build 和 ADS 对账。
- Sprint-32 completion gate 已作为最终门禁纳入 `it/README.md`。
