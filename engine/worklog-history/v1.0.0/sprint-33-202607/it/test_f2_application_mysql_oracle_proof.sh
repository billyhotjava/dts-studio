#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

cd "$REPO_ROOT"
if rg -n "IFS=\\$'\\\\t' read -r project_id" \
  worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_live_local.sh >/dev/null; then
  echo "live local proof must not parse projectId rows with whitespace TSV; empty projectId groups are valid" >&2
  exit 4
fi
if rg -n -F '[ -z "$project_id" ]' \
  worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_live_local.sh >/dev/null; then
  echo "live local proof must not drop empty projectId groups from ADS fixtures" >&2
  exit 4
fi
if ! rg -n 'CREATE TABLE f_voucher .* code ' \
  worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_live_local.sh >/dev/null; then
  echo "live local voucher fixture must include f_voucher.code because the application oracle excludes unnumbered vouchers" >&2
  exit 4
fi
bash worklog/prs/v1/tests/test_xycyl_finance_dbt_zip_contract.sh
mvn -q -pl dts-copilot-ai \
  -Dtest=FinanceApplicationMysqlOracleProofServiceTest,FinanceApplicationMysqlOracleProofRunnerTest,FinanceApplicationMysqlOracleProofResourceTest,FinanceApplicationMysqlOracleJdbcQueryExecutorTest,FinanceApplicationMysqlOracleJdbcConfigurationTest,AssetBackedPlannerPolicyTest#executableFinanceAdsTemplateOutranksL0BusinessObjectProfileIndicator \
  test
