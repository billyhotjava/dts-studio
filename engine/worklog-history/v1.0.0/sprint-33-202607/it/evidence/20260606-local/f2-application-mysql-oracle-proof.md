# F2 Application MySQL Oracle Proof Evidence

**Date**: 2026-06-06 / last rerun 2026-06-07 00:22 Asia/Shanghai
**Scope**: Sprint-33 F2, application MySQL oracle proof for month settlement, sale account, and voucher statistics
**Status**: PASS

## What Was Verified

- `finance-application-mysql-oracle-sql.v1.json` loads three cases: `month-settlement-discounted-receivable`, `sale-account-receivable`, and `voucher-year-2026-count`.
- Copilot/NL2SQL side is bound to ADS tables: `public.xycyl_ads_finance_month_settlement`, `public.xycyl_ads_sale_account_summary`, and `public.xycyl_ads_finance_voucher_monthly`.
- Oracle side is direct application MySQL SQL against `a_month_accounting`, `a_sale_account` + `t_flower_biz_info`, and `f_voucher` + `f_voucher_item`.
- `dts-copilot/worklog/prs/v1/xycyl-finance-dbt-models-v1.zip` contains the supporting dbt package: month settlement ADS exposes `projectId`; sale account ODS/STG/DWD/DWS/ADS and `models.tsv` manifest are present.
- Oracle SQL guard rejects ODS tables, Trino `mysql.rs_cloud_flower.*`, JDBC URLs, password text, wrong database names, and write SQL.
- JDBC executor can query application-style tables and normalize `accountPeriod`, `metricId`, and `amount` for reconciliation.
- Application runtime proof runner returns `DISABLED` when either ADS JDBC or application MySQL JDBC executor is missing, preventing false-positive proof runs.
- REST entrypoints are exposed for in-app proof checks: `GET /api/ai/finance/application-mysql-oracle/cases` and `POST /api/ai/finance/application-mysql-oracle/prove`.
- Conditional configuration creates the application MySQL executor only when `copilot.finance.application-mysql-oracle.enabled=true`, and creates the copilot ADS executor only when `copilot-jdbc-url` is also configured.
- Planner routes "2026年凭证的数据统计下" to TPL-57 ADS SQL even when platform has a high-confidence `prs.finance.voucher.profile` candidate.

## Commands

```bash
worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_proof.sh
worklog/v1.0.0/sprint-33-202607/it/test_f2_summary_dual_reconciliation.sh
worklog/prs/v1/tests/test_xycyl_finance_dbt_zip_contract.sh
docker exec dts-dbt sh -lc 'dbt parse --profiles-dir /opt/dbt/profiles --project-dir /tmp/xycyl-finance-dbt-v1/services/dts-dbt'
docker exec dts-dbt sh -lc 'dbt compile --profiles-dir /opt/dbt/profiles --project-dir /tmp/xycyl-finance-dbt-v1/services/dts-dbt --select +xycyl_ads_finance_month_settlement +xycyl_ads_finance_collection +xycyl_ads_sale_account_summary +xycyl_ads_finance_voucher_monthly'
docker exec -i dts-stack-dts-pg-1 psql -U biadmin -d biadmin -v ON_ERROR_STOP=1 < worklog/prs/v1/ods-finance-ddl.sql
mvn -pl dts-copilot-ai -Dtest=AssetBackedPlannerPolicyTest test
mvn -pl dts-copilot-ai -Dtest=FinanceApplicationMysqlOracleProofServiceTest,FinanceApplicationMysqlOracleProofRunnerTest,FinanceApplicationMysqlOracleProofResourceTest,FinanceApplicationMysqlOracleJdbcQueryExecutorTest,FinanceApplicationMysqlOracleJdbcConfigurationTest,FinanceSummaryDualReconciliationServiceTest test
mvn -pl dts-copilot-ai test
git diff --check
```

## Results

- New F2 application MySQL oracle proof IT: PASS, including three application MySQL oracle cases, in-app runner/resource tests, JDBC executor/config tests, and planner guard against `*.profile` route capture.
- dbt zip contract: PASS, including sale-account model chain and `models.tsv` manifest.
- Isolated dbt package parse: PASS in `dts-dbt` container; existing `log-path` deprecation and unused legacy config-path warnings only.
- Isolated dbt package compile: PASS, 17 models / 1 operation / 59 data tests / 6 sources found; sale-account ADS compiled.
- ODS DDL execution: PASS; six ODS tables exist in local warehouse (`a_month_accounting`, `a_collection_record`, `a_sale_account`, `t_flower_biz_info`, `f_voucher`, `f_voucher_item` mirrors).
- Existing F2 summary dual reconciliation IT: PASS.
- Planner route regression: 27 tests, 0 failures, 0 errors.
- `FinanceApplicationMysqlOracleProofServiceTest`: 4 tests, 0 failures, 0 errors.
- Combined planner + reconciliation/JDBC/runner/resource test set: PASS.
- Full `dts-copilot-ai` module: 352 tests, 0 failures, 0 errors.
- Whitespace check: PASS.

## Boundaries

- dbt model package artifacts stay under `dts-copilot/worklog/prs/v1`.
- `dts-stack` is not used to store these dbt model files.
- Live L2 endpoint/sign-off SQL wiring remains IN_PROGRESS.
