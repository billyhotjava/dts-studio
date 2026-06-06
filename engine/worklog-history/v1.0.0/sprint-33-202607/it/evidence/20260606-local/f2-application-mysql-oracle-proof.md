# F2 Application MySQL Oracle Proof Evidence

**Date**: 2026-06-06 / last rerun 2026-06-07 01:20 Asia/Shanghai
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
- Conditional proof JDBC configuration does not publish extra Spring `DataSource` beans, so enabling proof JDBC does not steal the primary `dataSource` from Spring Boot/JPA.
- Runtime container smoke verifies API-key protection and endpoint availability: unauthenticated `/cases` returns 401; authenticated `/cases` returns the three proof cases; authenticated `/prove` currently returns `DISABLED` because this local container has no direct application MySQL JDBC configured.
- Local live proof script creates a temporary MySQL container in `dts-core`, seeds application MySQL tables from current PG ADS aggregates, restarts `dts-copilot-ai` with direct JDBC proof enabled, requires `/prove` to return `PASSED`, then restores the default runtime. It supports `COPILOT_FINANCE_PROOF_CASE_IDS=all` or a comma-separated case list for month settlement, sale account, and voucher proof cases.
- Local live proof script preflights required ADS table existence and non-empty 2026 proof rows before restarting `dts-copilot-ai`, so missing dbt builds fail clearly instead of producing `/prove` 500 or false-positive proof runs.
- Registered datasource proof script can read an active application MySQL datasource from `copilot_ai.data_source`, combine it with the current ADS Postgres runtime env, run the same in-app `/prove` endpoint, and restore the default container afterward.
- Runtime proof runner converts JDBC/query exceptions into structured `FAILED` results instead of surfacing HTTP 500, so broken credentials and source mismatches stay auditable.
- `docker-compose.yml` exposes the `COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_*` environment variables needed to enable direct application MySQL and copilot ADS JDBC executors without editing model artifacts.
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
mvn -pl dts-copilot-ai -DskipTests package
docker compose build copilot-ai
docker compose up -d copilot-ai
docker compose config
worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_http.sh
REQUIRE_LIVE_APPLICATION_MYSQL_PROOF=true worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_http.sh
worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_live_local.sh
COPILOT_FINANCE_PROOF_CASE_IDS=all worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_live_local.sh
worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_registered_datasource.sh
COPILOT_FINANCE_PROOF_APP_DATASOURCE='园林业务库' worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_registered_datasource.sh
git diff --check
```

## Results

- New F2 application MySQL oracle proof IT: PASS, including three application MySQL oracle cases, in-app runner/resource tests, JDBC executor/config tests, and planner guard against `*.profile` route capture.
- Runtime container smoke: PASS for service startup, API-key protection, `/cases` availability, and explicit `DISABLED` proof status when direct application MySQL/ADS executors are not configured. The same script can enforce real direct MySQL proof by setting `REQUIRE_LIVE_APPLICATION_MYSQL_PROOF=true`.
- Runtime live-required guard: PASS; with `REQUIRE_LIVE_APPLICATION_MYSQL_PROOF=true`, the script fails on local `DISABLED` status instead of treating it as a successful proof.
- Runtime live-local direct JDBC proof: PASS for current built voucher ADS; `health_http_status=200`, unauthenticated `/cases` returns `401`, authenticated `/cases` returns 3 cases, `/prove` returns `200`, `prove_run_status=PASSED`, `prove_report_count=1`, `proved_case_count=1`, `temporary_mysql_voucher_count=663`.
- Runtime all-case preflight: PASS as a guardrail; with `COPILOT_FINANCE_PROOF_CASE_IDS=all`, current local PG fails before runtime mutation with `required ADS table is missing: public.xycyl_ads_finance_month_settlement`, proving that month-settlement/sale-account live proof is waiting for the dbt package build rather than being silently treated as passed.
- Registered datasource preflight: PASS as a guardrail; default `ptr_mysql` is present but its stored password is empty, so the registered-datasource script exits before runtime mutation with `registered MySQL data source has empty password: ptr_mysql`.
- Registered datasource runtime proof: FAIL as useful evidence, not an infrastructure crash. `COPILOT_FINANCE_PROOF_APP_DATASOURCE='园林业务库'` reaches MySQL and `/prove` returns HTTP 200 with `prove_run_status=FAILED`; first diff is `accountPeriod=2026-01`, `copilot=1.00`, `oracle=0.00`, `difference=1.00`, showing that this registered MySQL datasource is not aligned with the currently built ADS voucher source.
- Runtime exception handling regression: PASS; runner failures from JDBC/query execution return structured `FAILED` status instead of HTTP 500.
- Proof JDBC primary DataSource regression: PASS; enabling both application MySQL and copilot ADS proof JDBC leaves the Spring Boot primary `dataSource` as the only `DataSource` bean, preventing the `entityManagerFactory` startup regression.
- Compose rendered-config check: PASS; `copilot-ai` now receives the `COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_*` configuration surface needed to enable direct application MySQL proof in a runtime deployment.
- dbt zip contract: PASS, including sale-account model chain and `models.tsv` manifest.
- Isolated dbt package parse: PASS in `dts-dbt` container; existing `log-path` deprecation and unused legacy config-path warnings only.
- Isolated dbt package compile: PASS, 17 models / 1 operation / 59 data tests / 6 sources found; sale-account ADS compiled.
- ODS DDL execution: PASS; six ODS tables exist in local warehouse (`a_month_accounting`, `a_collection_record`, `a_sale_account`, `t_flower_biz_info`, `f_voucher`, `f_voucher_item` mirrors).
- Existing F2 summary dual reconciliation IT: PASS.
- Planner route regression: 27 tests, 0 failures, 0 errors.
- `FinanceApplicationMysqlOracleProofServiceTest`: 4 tests, 0 failures, 0 errors.
- Combined planner + reconciliation/JDBC/runner/resource test set: PASS.
- Full `dts-copilot-ai` module: 353 tests, 0 failures, 0 errors.
- Whitespace check: PASS.

## Boundaries

- dbt model package artifacts stay under `dts-copilot/worklog/prs/v1`.
- `dts-stack` is not used to store these dbt model files.
- Live L2 endpoint/sign-off SQL wiring remains IN_PROGRESS.
- Local runtime direct application MySQL proof remains `DISABLED` until `copilot.finance.application-mysql-oracle.*` JDBC settings point to a reachable application MySQL and copilot ADS database.
- `test_f2_application_mysql_oracle_runtime_live_local.sh` proves the direct JDBC path with a temporary local MySQL mirror seeded from current PG ADS aggregates. Current local PG has voucher ADS only; after importing/building the dbt package from `dts-copilot/worklog/prs/v1`, rerun with `COPILOT_FINANCE_PROOF_CASE_IDS=all` to prove month-settlement, sale-account, and voucher cases together. This is not a substitute for production application MySQL credentials or finance sign-off.
- Registered application MySQL proof now distinguishes configuration and data problems: `ptr_mysql` needs its stored password repaired, while `园林业务库` is reachable but does not match the current 2026 voucher ADS source.
- No secrets are stored in the dbt package or Sprint-33 proof scripts; runtime credentials must be supplied by environment variables.
