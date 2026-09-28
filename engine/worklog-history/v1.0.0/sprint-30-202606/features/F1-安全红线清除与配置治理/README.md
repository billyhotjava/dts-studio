# F1: 安全红线清除与配置治理

**优先级**: P0
**状态**: DONE

## 目标

清除 dts-stack Airflow DAG 中明文硬编码的生产数据库密码,建立全仓凭据扫描基线,堵住"任何 agent 接触 ELT 配置即触达生产密码"的红线。**本 sprint 一切其它工作的前置。**

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | Airflow DAG 凭据外置(移除明文密码) | P0 | DONE | - |
| T02 | 全仓凭据扫描与示例化基线 | P0 | DONE | - |
| T03 | 暴露密码轮换跟进项登记 | P0 | DONE | T01 |

## 完成标准

- [x] 5 个 tenant 的 DAG JSON 不再含明文密码,改用 Airflow Variables/环境变量引用
- [x] 建立凭据扫描脚本与基线,覆盖 dts-ingestion 生成器与 Airflow DAG 目标范围
- [x] 暴露密码登记为需轮换项,纳入风险台账
