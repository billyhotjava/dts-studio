# T01: 补 t_change_info ODS 同步

**优先级**: P0
**状态**: BLOCKED
**依赖**: F1

## 目标

为 `t_change_info`(报花变更单)新增 Airflow ODS 同步任务,使 sprint-22 设计的 `xycyl_ads_flowerbiz_change_log` 对象有源可建(当前 DAG 目录无 `ods_ptr_mysql_t_change_info`)。

## 技术设计

- 复用现有 `ptr_mysql_flow-*` DAG 模板(去明文后),新增 `t_change_info`(及必要的 `t_flower_rent_time_log`)的 ODS 同步流。
- 全量/增量策略对齐既有 `t_flower_biz_*` 同步(多 tenant prefix)。
- ODS 目标表 `ods_ptr_mysql_t_change_info`,列与源对齐,补建表 DDL。

## 影响范围

- `dts-stack/services/dts-airflow/dags/`(新增 DAG)
- ODS 建表脚本(`worklog/prs/v1/ods_create_tables.sql` 同风格)

## 验证

- [ ] DAG 可解析、可触发,ODS 表产数行数与源一致(抽样)
- [ ] `xycyl_dwd_flowerbiz_change` 来源路径打通

## 完成标准

- [ ] t_change_info 进入 ODS,change_log mart 血缘可达
