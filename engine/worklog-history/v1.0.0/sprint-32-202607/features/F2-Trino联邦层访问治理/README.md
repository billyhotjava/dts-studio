# F2: Trino 联邦层访问治理

**优先级**: P0
**状态**: DONE

## 目标

给 Trino 联邦层（Sprint-30 F6）补上访问治理，避免"联邦在 lake 规模上重演 tech-debt 里的重查询打业务库"。当前本地 dts-stack 没有 Ranger runtime，本 sprint 以 Trino file access-control 完成可运行治理闭环：受限账号、catalog/table 只读、系统 catalog 拒绝、资源/审计护栏；Ranger 可在企业部署中替换为策略源。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | Trino→biz MySQL 访问策略（读副本/限流） | P0 | DONE | - |
| T02 | Ranger/access-control 策略接入联邦路径 | P0 | DONE | T01 |
| T03 | 联邦查询资源/超时/审计护栏 | P1 | DONE | S30-F6 |

## Task 明细

### T01 访问策略（读副本/限流）
- **目标**：联邦读 biz MySQL 不直打生产主库、不无限并发。
- **设计**：Trino mysql catalog 指向受限账号；连接池上限 + 并发/队列限制；明确"联邦只读"。配置在 dts-stack Trino catalog + dts-copilot 连接定义。`dts-trino` 默认优先使用 `TRINO_MYSQL_READONLY_*`，不再默认回退 root，catalog 继续通过 env 注入连接信息；Trino 全局 resource group 限制联邦入口并发与队列。运行态已切到数据源 15 的受限账号并重建 `dts-trino`。
- **影响**：dts-stack `dts-trino` catalog 配置；analytics 联邦数据源定义；docker-compose/env。
- **验证**：配置门禁可重跑 `../../it/test_f2_trino_mysql_access_policy.sh`，证据见 `../../it/evidence/20260603-local/f2-trino-mysql-access-policy.md`；运行时 Trino CLI 已证明 postgres/mysql SELECT 成功、system catalog 和 MySQL 写表被 access-control 拒绝。

### T02 Ranger/access-control 策略
- **目标**：联邦查询遵守受控 catalog/table 权限，避免 agent 访问系统 catalog 或写业务库。
- **设计**：本地 dts-stack 没有 Ranger runtime，因此接入 Trino file access-control：`postgres/mysql/jmx` 只允许 SELECT，`system` 与其他 catalog 拒绝。企业部署如启用 Ranger，可迁移同一策略语义到 Ranger 插件。
- **影响**：dts-stack Trino access-control 配置；联邦路径鉴权边界。
- **验证**：受控用户可读 mysql/postgres 业务表；访问 `system.runtime.nodes` 被拒绝；`CREATE TABLE mysql...` 被拒绝。

### T03 资源/超时/审计护栏
- **目标**：扩展 `FederatedQueryGuardrail`，加运行时资源与审计边界。
- **设计**：查询级超时、结果行数上限、扫描量/内存上限（Trino resource group）；联邦查询审计日志（谁、何 catalog、何表、耗时）。在 guardrail 静态校验之外补运行时护栏。第一步已落地统一 native SQL 执行 gate：`/api/dataset` 主路径与固定报表/大屏预热等直接 `runNative` 路径都会在运行前把 dbt 资产表名 `public.xycyl_*` 补全为联邦执行名 `postgres.public.xycyl_*`，避免绕过主路径导致两段式表名错误。第二步已定位并修复 dbt numeric 指标列在 Trino Postgres catalog 默认 `IGNORE` 下不可见的问题：catalog 配置新增 `unsupported-type-handling=CONVERT_TO_VARCHAR`，销售模板改为 Trino/Postgres 双兼容的显式 `CAST(... AS DECIMAL)` 聚合。第三步已补历史 screen/固定资产旧 SQL 的执行兜底：native SQL 自动重试会把 Trino 下失败的 `?::date`、`SUM(x)::numeric`、裸 `SUM/AVG(varchar)`、`to_char(...,'YYYY-MM[-DD]')` 改写为 Trino 兼容表达式，避免存量资产批量 500。第四步已补 Trino 配置级资源护栏：`query.max-run-time=5m`、`query.max-execution-time=3m`、`query.max-planning-time=1m`、`query.max-scan-physical-bytes=2GB`、`query.max-length=100000`，并通过 `resource-groups.properties/json` 限制全局 `hardConcurrencyLimit=4`、`maxQueued=20`。
- **影响**：`FederatedQueryGuardrail` + `QueryExecutionFacade`（analytics）；Trino resource group 配置。
- **验证**：超时/超量查询被拦并有审计；正常查询不受影响；analytics 既有联邦测试不回归。当前可重跑验证见 `../../it/test_f2_federated_sql_execution_gate.sh`，销售模板与旧 screen 资产根因证据见 `../../it/evidence/20260603-local/f2-trino-dbt-sales-root-cause.md`。

## 完成标准

- [x] 联邦读走受限账号 + 连接限流，运行态已验证 MySQL/Postgres 只读
- [x] 联邦路径接 Trino access-control；Ranger 作为企业部署策略源保留
- [x] 资源/超时/审计护栏生效，guardrail 静态+配置级护栏已完成，运行态探针通过
