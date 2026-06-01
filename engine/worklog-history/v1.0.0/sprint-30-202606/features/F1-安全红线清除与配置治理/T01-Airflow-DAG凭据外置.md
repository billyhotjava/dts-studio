# T01: Airflow DAG 凭据外置（移除明文密码）

**优先级**: P0
**状态**: READY
**依赖**: 无

## 目标

把 `dts-stack/services/dts-airflow/dags/ptr_mysql_flow-*_ods_*.json`(5 个 tenant)中明文硬编码的生产 MySQL/PG 密码移出代码,改为 Airflow Connections/Variables 引用。

## 技术设计

- 盘点所有含明文密码的 DAG 配置文件(grep `Devops123@` 及 PG 密码模式)。
- 在 Airflow 侧建立 Connection(如 `prs_mysql_src`、`dts_pg_target`),DAG 配置改为按 conn_id 引用,不再内联密码。
- 若 DAG 由生成器/模板产出,改模板源头,避免再生成时回写明文。
- 提供 `.env.example` / Connection 配置说明文档,真实值走部署环境注入。

## 影响范围

- `dts-stack/services/dts-airflow/dags/ptr_mysql_flow-*`(5 tenant × 多表)
- Airflow Connections 配置、DAG 生成模板(若有)
- 部署文档/compose env

## 验证

- [ ] `grep -rn 'Devops123@' dts-stack/` 无命中
- [ ] DAG 仍能正常解析(airflow dags list / 本地校验)
- [ ] ODS 同步任务用 Connection 跑通(或干跑校验配置可解析)

## 完成标准

- [ ] 5 个 tenant DAG 全部去明文,改 Connection/Variable 引用
- [ ] 模板源头同步修正,不会再生成明文
