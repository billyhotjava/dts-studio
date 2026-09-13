#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

cd "$REPO_ROOT"
if rg -n "IFS=\\$'\\\\t' read -r project_id" \
  worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_runtime_live_local.sh >/dev/null; then
  echo "live local proof must not parse projectId rows with whitespace TSV; empty projectId groups are valid" >&2
  exit 4
fi
if rg -n -F '[ -z "$project_id" ]' \
  worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_runtime_live_local.sh >/dev/null; then
  echo "live local proof must not drop empty projectId groups from ADS fixtures" >&2
  exit 4
fi
if ! rg -n 'CREATE TABLE f_voucher .* code ' \
  worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_runtime_live_local.sh >/dev/null; then
  echo "live local voucher fixture must include f_voucher.code because the application MySQL authority baseline excludes unnumbered vouchers" >&2
  exit 4
fi
if ! rg -n -F 'COPILOT_FINANCE_APPLICATION_MYSQL_AUTHORITY_ENABLED: ${COPILOT_FINANCE_APPLICATION_MYSQL_AUTHORITY_ENABLED:-${COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_ENABLED:-false}}' \
  docker-compose.yml >/dev/null; then
  echo "docker compose must expose authority-named application MySQL proof env vars with legacy oracle env fallback" >&2
  exit 4
fi
if ! rg -n -F 'COPILOT_FINANCE_APPLICATION_MYSQL_AUTHORITY_COPILOT_JDBC_URL: ${COPILOT_FINANCE_APPLICATION_MYSQL_AUTHORITY_COPILOT_JDBC_URL:-${COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_COPILOT_JDBC_URL:-}}' \
  docker-compose.yml >/dev/null; then
  echo "docker compose must expose authority-named copilot ADS proof env vars with legacy oracle env fallback" >&2
  exit 4
fi
if ! rg -n -F 'it/test_f2_finance_ingestion_mapping_preflight.sh' \
  worklog/v1.0.0/sprint-33-202607/it/README.md \
  worklog/v1.0.0/sprint-33-202607/features/F2-复式凭证tie-out与汇总双路对账/T02-汇总双路计算对账.md >/dev/null; then
  echo "F2 proof evidence must document the finance ingestion mapping preflight so ADS proof cannot silently run without required ODS mappings" >&2
  exit 4
fi
bash worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_runtime_case_ids_contract.sh
bash worklog/prs/v1/tests/test_xycyl_finance_dbt_zip_contract.sh
mvn -q -pl dts-copilot-ai \
  -Dtest=FinanceApplicationMysqlAuthorityRegistryTest,FinanceApplicationMysqlAuthorityProofServiceTest,FinanceApplicationMysqlAuthorityProofRunnerTest,FinanceApplicationMysqlAuthorityScorecardCheckServiceTest,FinanceApplicationMysqlAuthorityProofResourceTest,FinanceApplicationMysqlOracleJdbcQueryExecutorTest,FinanceApplicationMysqlAuthorityJdbcConfigurationTest,AssetBackedPlannerPolicyTest#executableFinanceAdsTemplateOutranksL0BusinessObjectProfileIndicator \
  test
