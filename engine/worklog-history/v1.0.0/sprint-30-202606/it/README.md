# Sprint-30 集成测试（IT）

本目录存放"数据面补全地基"的验收证据。**所有证据必须真实可重跑,不接受空占位**(沿用 sprint-25/26/29 风格)。

## 证据结构

```
it/
  README.md                  # 本文件,证据索引
  sql/                       # 口径陷阱问句集、对账 SQL、覆盖校验脚本
  evidence/<日期>-local/     # 每次本地验证的结果快照
```

## 证据矩阵

| Feature | 验收点 | 证据位置 | 状态 |
|---------|--------|---------|------|
| F1 | Airflow DAG 无明文密码 | `evidence/20260601-local/f1-airflow-credentials-summary.md` | DONE |
| F1 | 凭据扫描基线 | `evidence/20260601-local/f1-credential-scan.txt` | DONE |
| F2 | t_change_info ODS 同步 | `assets/ods-coverage-matrix.tsv` | DONE |
| F2 | 仓储出入库 ODS | `assets/ods-coverage-matrix.tsv` | DONE |
| F2 | 财务源表 ODS | `assets/ods-coverage-matrix.tsv` | DONE |
| F2 | ODS 覆盖对照表缺口为零 | `evidence/20260601-local/f2-ods-coverage-summary.md` | DONE |
| F3 | §3 九条 guardrail 入 pack | `evidence/20260601-local/f3-caliber-guardrails-summary.md` + SemanticPack schema 测试绿 | DONE |
| F3 | 口径回归网 | `sql/caliber-regression-questions.tsv` + `test_f3_caliber_guardrails.sh` | DONE |
| F3 | biz_type 枚举字典 | `assets/biz-type-enum-dictionary.md` | DONE |
| F4 | 月对账应收/折后/回款 ads | `evidence/20260601-local/f4-finance-vertical-slice-summary.md` + `f4-dbt-compile-selected.txt` | DONE |
| F4 | 开票进度 ads | `evidence/20260601-local/f4-finance-vertical-slice-summary.md` | DONE |
| F4 | 收款明细 ads | `evidence/20260601-local/f4-finance-vertical-slice-summary.md` | DONE |
| F4 | 财务语义对象 NL2SQL 命中 | `SemanticPackCaliberGuardrailTest` | DONE |
| F4 | 与 adminweb 内建报表对账 | `sql/f4_finance_adminweb_reconciliation.sql` | DONE |
| F5 | 空白域 onboarding checklist | `assets/blank-domain-onboarding-checklist.md` | DONE |
| F5 | Sprint-30 IT 证据包 | `evidence/20260601-local/f5-sprint30-evidence-pack.md` | DONE |
| F6 | dts-trino 服务与 catalog | `test_f6_trino_federated_join.sh` | DONE |
| F6 | PG/MySQL 真实跨库 Join | `sql/f6_trino_federated_join.sql` + `evidence/20260601-local/f6-trino-federated-join-summary.md` | DONE |

## 重跑约定

每个 `test_*.sh` 可独立执行,结果写入 `evidence/<日期>-local/`。标 DONE 前本矩阵所有计划证据必须替换为真实证据链接。

F6 live 验证需要先启动 dts-stack 的 `dts-trino`,然后执行:

```bash
RUN_LIVE=1 bash worklog/v1.0.0/sprint-30-202606/it/test_f6_trino_federated_join.sh
```
