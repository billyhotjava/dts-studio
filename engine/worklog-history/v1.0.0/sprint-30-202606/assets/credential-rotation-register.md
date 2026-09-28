# F1 凭据轮换登记

**状态**: CODE_MITIGATED_ROTATION_REQUIRED
**登记日期**: 2026-06-01

## 背景

Sprint-30 F1 已把 5 个 tenant 的 Airflow Addax JSON 密码字段改为运行时变量占位符,并补上可重跑扫描脚本。由于历史明文已经进入版本库与本地运行目录,生产侧仍必须轮换受影响账号,不能把"代码已移除"视为"凭据仍安全"。

## 受影响凭据

| 凭据 | 历史使用位置 | 运行时变量/Connection | Owner | 状态 |
|------|--------------|------------------------|-------|------|
| PRS MySQL 源库读取账号 | `ptr_mysql_flow-*_ods_*.json` reader | `DTS_ADDAX_READER_PASSWORD` / `DTS_SOURCE_DB_PASSWORD` / `DTS_PTR_MYSQL_PASSWORD` | 平台运维 + DBA | 待生产轮换 |
| DTS PostgreSQL 目标库写入账号 | `ptr_mysql_flow-*_ods_*.json` writer | `DTS_TARGET_DB_PASSWORD` / `DTS_ADDAX_WRITER_PASSWORD` | 平台运维 + DBA | 待生产轮换 |

## 轮换步骤

1. DBA 在生产侧创建或轮换最小权限读取/写入账号。
2. 平台运维把新值写入 Airflow Variables 或部署环境变量,不再写入 DAG JSON。
3. 重启 Airflow scheduler/webserver/triggerer,触发一个低风险 ODS 任务验证连接。
4. 复跑 `worklog/v1.0.0/sprint-30-202606/it/test_f1_airflow_credentials.sh`,归档 evidence。

## 当前代码侧证据

- `dts-stack/services/dts-airflow/dags/ptr_mysql_flow-*_ods_*.json`: 55 个 ODS JSON 已改为 `${DTS_ADDAX_READER_PASSWORD}` / `${DTS_TARGET_DB_PASSWORD}`。
- `dts-stack/services/dts-airflow/dags/addax-env-runner.sh`: Addax 容器内运行时渲染占位符。
- `dts-stack/scripts/security/sprint30_credential_scan.sh`: F1 凭据扫描基线。
- `it/evidence/20260601-local/f1-airflow-credentials-summary.md`: 本地验收摘要。
