#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

cd "$REPO_ROOT"
bash worklog/prs/v1/tests/test_xycyl_finance_dbt_zip_contract.sh
mvn -q -pl dts-copilot-ai \
  -Dtest=FinanceApplicationMysqlOracleProofServiceTest,FinanceApplicationMysqlOracleJdbcQueryExecutorTest,FinanceApplicationMysqlOracleJdbcConfigurationTest,AssetBackedPlannerPolicyTest#executableFinanceAdsTemplateOutranksL0BusinessObjectProfileIndicator \
  test
