# Sprint-32 集成验证（IT）

**状态**: DONE

汇总 Sprint-32 路由阶梯、联邦治理、多场景接入的可重跑验证证据。各 Feature 完成时回填，**禁止空占位**（沿用 sprint-30 `it/` 标准）。

## 证据清单

| 编号 | 验证项 | 来源 | 重跑方式 | 状态 |
|------|--------|------|----------|------|
| IT-01 | 路由 5 层命中分布 + routeTrace/telemetry 样例 | F1-T01/T02/T03 | `bash worklog/v1.0.0/sprint-32-202607/it/test_f1_route_trace.sh`；`bash worklog/v1.0.0/sprint-32-202607/it/test_f1_route_telemetry.sh`；证据见 `it/evidence/20260603-local/f1-route-responsibility-chain.md`、`it/evidence/20260603-local/f1-route-trace.md`、`it/evidence/20260603-local/f1-route-telemetry.md`、`it/evidence/20260603-local/f1-tier5-fallback-contract.md`、`it/evidence/20260603-local/template-route-precedence-root-cause.md` | PASS |
| IT-02 | 联邦读副本/连接限流验证 | F2-T01 | `bash worklog/v1.0.0/sprint-32-202607/it/test_f2_trino_mysql_access_policy.sh`；证据见 `it/evidence/20260603-local/f2-trino-mysql-access-policy.md` | PASS |
| IT-03 | Ranger/access-control 受限策略 | F2-T02 | Trino CLI 探针；证据见 `it/evidence/20260603-local/f2-trino-mysql-access-policy.md` | PASS |
| IT-04 | 联邦资源/超时/审计护栏 + 存量 SQL 方言兜底 | F2-T03 | `bash worklog/v1.0.0/sprint-32-202607/it/test_f2_federated_sql_execution_gate.sh`；证据见 `it/evidence/20260603-local/f2-federated-sql-execution-gate.md`、`it/evidence/20260603-local/f2-trino-dbt-sales-root-cause.md` | PASS |
| IT-05 | 场景接入套件脚手架与模板验证 | F3-T01/T02/T03 | `bash worklog/v1.0.0/sprint-32-202607/it/test_f3_scenario_onboarding_kit.sh`；证据见 `it/evidence/20260603-local/f3-scenario-onboarding-kit.md` | PASS |
| IT-06 | 新场景端到端跑通（domain→dbt→pack→路由→联邦） | F4-T01 | `bash worklog/v1.0.0/sprint-32-202607/it/test_f4_inventory_source_discovery.sh`；`bash worklog/v1.0.0/sprint-32-202607/it/test_f4_inventory_dbt_ads_models.sh`；证据见 `it/evidence/20260603-local/f4-inventory-source-discovery.md`、`it/evidence/20260603-local/f4-inventory-dbt-ads-models.md`、`it/evidence/20260603-local/f4-inventory-dbt-runtime-build.md`、`it/evidence/20260603-local/f4-inventory-ads-reconciliation.md`、`it/evidence/20260603-local/f4-multi-scenario-isolation.md` | PASS |
| IT-07 | 多场景共存隔离回归 | F4-T02 | `bash worklog/v1.0.0/sprint-32-202607/it/test_f4_multi_scenario_isolation.sh`；证据见 `it/evidence/20260603-local/f4-multi-scenario-isolation.md` | PASS |
| IT-08 | 多场景接入手册与证据索引 | F5-T01/T02 | `bash worklog/v1.0.0/sprint-32-202607/it/test_f5_multi_scenario_guide.sh`；产物见 `assets/multi-scenario-onboarding-guide.md`；证据见 `it/evidence/20260603-local/f5-multi-scenario-guide.md` | PASS |
| IT-09 | Sprint-32 完成门禁 | F5-T02 | `bash worklog/v1.0.0/sprint-32-202607/it/test_sprint32_completion_gate.sh`；证据见 `it/evidence/20260603-local/sprint32-completion-gate.md`、`it/evidence/20260605-local/sprint32-completion-gate-rerun.md` | PASS |

## 目录约定

```
it/
  README.md
  evidence/{YYYYMMDD-env}/
  test_*.sh
```
