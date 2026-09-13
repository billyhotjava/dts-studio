#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

AUTHORITY_REGISTRY_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceApplicationMysqlAuthorityRegistry.java"
LEGACY_ORACLE_REGISTRY_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceApplicationMysqlOracleRegistry.java"
AUTHORITY_JDBC_CONFIG_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceApplicationMysqlAuthorityJdbcConfiguration.java"
LEGACY_ORACLE_JDBC_CONFIG_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceApplicationMysqlOracleJdbcConfiguration.java"
AUTHORITY_PROOF_RESOURCE_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/FinanceApplicationMysqlAuthorityProofResource.java"
LEGACY_PROOF_RESOURCE_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/FinanceApplicationMysqlOracleProofResource.java"
AUTHORITY_PROOF_RESOURCE_TEST_JAVA="$REPO_ROOT/dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/web/rest/FinanceApplicationMysqlAuthorityProofResourceTest.java"
AUTHORITY_SQL_JSON="$REPO_ROOT/dts-copilot-ai/src/main/resources/governance/finance-application-mysql-authority-sql.v1.json"
WORKLOG_AUTHORITY_SQL_JSON="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/assets/finance-application-mysql-authority-sql.v1.json"
AUTHORITY_PROOF_SERVICE_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceApplicationMysqlAuthorityProofService.java"
LEGACY_ORACLE_PROOF_SERVICE_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceApplicationMysqlOracleProofService.java"
AUTHORITY_PROOF_SERVICE_TEST_JAVA="$REPO_ROOT/dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceApplicationMysqlAuthorityProofServiceTest.java"
AUTHORITY_PROOF_RUNNER_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceApplicationMysqlAuthorityProofRunner.java"
LEGACY_ORACLE_PROOF_RUNNER_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceApplicationMysqlOracleProofRunner.java"
AUTHORITY_PROOF_RUNNER_TEST_JAVA="$REPO_ROOT/dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceApplicationMysqlAuthorityProofRunnerTest.java"
AUTHORITY_SCORECARD_CHECK_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceApplicationMysqlAuthorityScorecardCheckService.java"
LEGACY_ORACLE_SCORECARD_CHECK_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceApplicationMysqlOracleScorecardCheckService.java"
AUTHORITY_SCORECARD_CHECK_TEST_JAVA="$REPO_ROOT/dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceApplicationMysqlAuthorityScorecardCheckServiceTest.java"
AUTHORITY_JDBC_CONFIG_TEST_JAVA="$REPO_ROOT/dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceApplicationMysqlAuthorityJdbcConfigurationTest.java"
DIFFERENTIAL_ROW_PROVIDER_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceDifferentialGridSummarySqlRowProvider.java"
F2_PROOF_IT_SCRIPT="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_proof.sh"
AUTHORITY_RUNTIME_HTTP_SCRIPT="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_runtime_http.sh"
AUTHORITY_RUNTIME_LIVE_LOCAL_SCRIPT="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_runtime_live_local.sh"
AUTHORITY_RUNTIME_REGISTERED_DATASOURCE_SCRIPT="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_runtime_registered_datasource.sh"
AUTHORITY_RUNTIME_CASE_IDS_CONTRACT="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_runtime_case_ids_contract.sh"
LEGACY_ORACLE_RUNTIME_HTTP_SCRIPT="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_http.sh"
LEGACY_ORACLE_RUNTIME_LIVE_LOCAL_SCRIPT="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_live_local.sh"
LEGACY_ORACLE_RUNTIME_REGISTERED_DATASOURCE_SCRIPT="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_registered_datasource.sh"
LEGACY_ORACLE_RUNTIME_CASE_IDS_CONTRACT="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_case_ids_contract.sh"
SCORECARD_IT_SCRIPT="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_sprint33_reconciliation_scorecard.sh"

if [[ ! -f "$AUTHORITY_REGISTRY_JAVA" ]]; then
  echo "Missing canonical FinanceApplicationMysqlAuthorityRegistry.java" >&2
  exit 1
fi

if [[ ! -f "$AUTHORITY_JDBC_CONFIG_JAVA" ]]; then
  echo "Missing canonical FinanceApplicationMysqlAuthorityJdbcConfiguration.java" >&2
  exit 1
fi

if [[ ! -f "$AUTHORITY_PROOF_RESOURCE_JAVA" ]]; then
  echo "Missing canonical FinanceApplicationMysqlAuthorityProofResource.java" >&2
  exit 1
fi

if [[ ! -f "$AUTHORITY_PROOF_SERVICE_JAVA" ]]; then
  echo "Missing canonical FinanceApplicationMysqlAuthorityProofService.java" >&2
  exit 1
fi

if [[ ! -f "$AUTHORITY_PROOF_RUNNER_JAVA" ]]; then
  echo "Missing canonical FinanceApplicationMysqlAuthorityProofRunner.java" >&2
  exit 1
fi

if [[ ! -f "$AUTHORITY_SCORECARD_CHECK_JAVA" ]]; then
  echo "Missing canonical FinanceApplicationMysqlAuthorityScorecardCheckService.java" >&2
  exit 1
fi

if [[ ! -f "$AUTHORITY_SQL_JSON" ]]; then
  echo "Missing canonical finance-application-mysql-authority-sql.v1.json governance asset" >&2
  exit 1
fi

if [[ ! -f "$WORKLOG_AUTHORITY_SQL_JSON" ]]; then
  echo "Missing Sprint-33 asset copy for finance-application-mysql-authority-sql.v1.json" >&2
  exit 1
fi

if ! cmp -s "$AUTHORITY_SQL_JSON" "$WORKLOG_AUTHORITY_SQL_JSON"; then
  echo "Sprint-33 application MySQL authority SQL asset copy must match the runtime governance asset" >&2
  exit 1
fi

if grep -Fq "application-mysql-oracle-sql" "$AUTHORITY_SQL_JSON"; then
  echo "Canonical application MySQL authority asset must not identify itself as application-mysql-oracle" >&2
  exit 1
fi

if ! grep -Fq 'governance/finance-application-mysql-authority-sql.v1.json' "$AUTHORITY_REGISTRY_JAVA"; then
  echo "FinanceApplicationMysqlAuthorityRegistry must load the canonical authority SQL governance asset" >&2
  exit 1
fi

if ! grep -Fq "AuthoritySqlCase" "$AUTHORITY_REGISTRY_JAVA"; then
  echo "FinanceApplicationMysqlAuthorityRegistry must expose canonical AuthoritySqlCase records" >&2
  exit 1
fi

if grep -Fq "OracleSqlCase" "$AUTHORITY_REGISTRY_JAVA" || grep -Fq "OracleSqlCase" "$AUTHORITY_PROOF_SERVICE_JAVA"; then
  echo "Canonical application MySQL authority code must not keep OracleSqlCase internal types" >&2
  exit 1
fi

if ! grep -Eq '^[[:space:]]*@Service' "$AUTHORITY_REGISTRY_JAVA"; then
  echo "FinanceApplicationMysqlAuthorityRegistry must be the Spring service bean" >&2
  exit 1
fi

if [[ -f "$LEGACY_ORACLE_REGISTRY_JAVA" ]]; then
  if grep -Eq '^[[:space:]]*@Service' "$LEGACY_ORACLE_REGISTRY_JAVA"; then
    echo "Legacy FinanceApplicationMysqlOracleRegistry must not be the Spring service bean" >&2
    exit 1
  fi
  if ! grep -Fq "@Deprecated" "$LEGACY_ORACLE_REGISTRY_JAVA"; then
    echo "Legacy FinanceApplicationMysqlOracleRegistry must be explicitly marked deprecated" >&2
    exit 1
  fi
fi

if ! grep -Eq '^[[:space:]]*@Configuration' "$AUTHORITY_JDBC_CONFIG_JAVA"; then
  echo "FinanceApplicationMysqlAuthorityJdbcConfiguration must be the Spring configuration" >&2
  exit 1
fi

if ! grep -Fq 'FinanceApplicationMysqlAuthorityJdbcProperties.class' "$AUTHORITY_JDBC_CONFIG_JAVA"; then
  echo "FinanceApplicationMysqlAuthorityJdbcConfiguration must enable authority JDBC properties" >&2
  exit 1
fi

if ! grep -Fq 'financeApplicationMysqlAuthorityJdbcQueryExecutor' "$AUTHORITY_JDBC_CONFIG_JAVA"; then
  echo "FinanceApplicationMysqlAuthorityJdbcConfiguration must expose the canonical authority executor bean" >&2
  exit 1
fi

if ! grep -Fq 'financeApplicationMysqlOracleJdbcQueryExecutor' "$AUTHORITY_JDBC_CONFIG_JAVA"; then
  echo "FinanceApplicationMysqlAuthorityJdbcConfiguration must keep the legacy executor bean alias" >&2
  exit 1
fi

if [[ -f "$LEGACY_ORACLE_JDBC_CONFIG_JAVA" ]]; then
  if grep -Eq '^[[:space:]]*@Configuration' "$LEGACY_ORACLE_JDBC_CONFIG_JAVA"; then
    echo "Legacy FinanceApplicationMysqlOracleJdbcConfiguration must not be the Spring configuration" >&2
    exit 1
  fi
  if ! grep -Fq "@Deprecated" "$LEGACY_ORACLE_JDBC_CONFIG_JAVA"; then
    echo "Legacy FinanceApplicationMysqlOracleJdbcConfiguration must be explicitly marked deprecated" >&2
    exit 1
  fi
fi

if ! grep -Eq '^[[:space:]]*@Service' "$AUTHORITY_PROOF_SERVICE_JAVA"; then
  echo "FinanceApplicationMysqlAuthorityProofService must be the Spring service bean" >&2
  exit 1
fi

if [[ -f "$LEGACY_ORACLE_PROOF_SERVICE_JAVA" ]]; then
  if grep -Eq '^[[:space:]]*@Service' "$LEGACY_ORACLE_PROOF_SERVICE_JAVA"; then
    echo "Legacy FinanceApplicationMysqlOracleProofService must not be the Spring service bean" >&2
    exit 1
  fi
  if ! grep -Fq "@Deprecated" "$LEGACY_ORACLE_PROOF_SERVICE_JAVA"; then
    echo "Legacy FinanceApplicationMysqlOracleProofService must be explicitly marked deprecated" >&2
    exit 1
  fi
fi

if ! grep -Eq '^[[:space:]]*@Service' "$AUTHORITY_PROOF_RUNNER_JAVA"; then
  echo "FinanceApplicationMysqlAuthorityProofRunner must be the Spring service bean" >&2
  exit 1
fi

if ! grep -Fq "FinanceApplicationMysqlAuthorityRegistry" "$AUTHORITY_PROOF_RUNNER_JAVA"; then
  echo "Application MySQL proof runner must depend on the canonical authority registry" >&2
  exit 1
fi

if grep -Fq '@Qualifier("financeApplicationMysqlOracle' "$AUTHORITY_PROOF_RUNNER_JAVA"; then
  echo "Application MySQL proof runner must inject canonical authority executor bean names, not legacy Oracle aliases" >&2
  exit 1
fi

if [[ -f "$LEGACY_ORACLE_PROOF_RUNNER_JAVA" ]]; then
  if grep -Eq '^[[:space:]]*@Service' "$LEGACY_ORACLE_PROOF_RUNNER_JAVA"; then
    echo "Legacy FinanceApplicationMysqlOracleProofRunner must not be the Spring service bean" >&2
    exit 1
  fi
  if ! grep -Fq "@Deprecated" "$LEGACY_ORACLE_PROOF_RUNNER_JAVA"; then
    echo "Legacy FinanceApplicationMysqlOracleProofRunner must be explicitly marked deprecated" >&2
    exit 1
  fi
fi

if ! grep -Eq '^[[:space:]]*@Service' "$AUTHORITY_SCORECARD_CHECK_JAVA"; then
  echo "FinanceApplicationMysqlAuthorityScorecardCheckService must be the Spring service bean" >&2
  exit 1
fi

if [[ -f "$LEGACY_ORACLE_SCORECARD_CHECK_JAVA" ]]; then
  if grep -Eq '^[[:space:]]*@Service' "$LEGACY_ORACLE_SCORECARD_CHECK_JAVA"; then
    echo "Legacy FinanceApplicationMysqlOracleScorecardCheckService must not be the Spring service bean" >&2
    exit 1
  fi
  if ! grep -Fq "@Deprecated" "$LEGACY_ORACLE_SCORECARD_CHECK_JAVA"; then
    echo "Legacy FinanceApplicationMysqlOracleScorecardCheckService must be explicitly marked deprecated" >&2
    exit 1
  fi
fi

if grep -Fq '@Qualifier("financeApplicationMysqlOracle' "$DIFFERENTIAL_ROW_PROVIDER_JAVA"; then
  echo "Differential grid summary SQL row provider must inject canonical authority executor bean names, not legacy Oracle aliases" >&2
  exit 1
fi

if ! grep -Fq "FinanceApplicationMysqlAuthorityRegistry" "$AUTHORITY_PROOF_RESOURCE_JAVA"; then
  echo "Application MySQL proof resource must depend on the canonical authority registry" >&2
  exit 1
fi

if ! grep -Fq '"/api/ai/finance/application-mysql-authority"' "$AUTHORITY_PROOF_RESOURCE_JAVA"; then
  echo "Canonical application MySQL proof resource must publish the authority REST path" >&2
  exit 1
fi

if [[ -f "$LEGACY_PROOF_RESOURCE_JAVA" ]]; then
  if grep -Eq '^[[:space:]]*@RestController' "$LEGACY_PROOF_RESOURCE_JAVA"; then
    echo "Legacy FinanceApplicationMysqlOracleProofResource must not be the REST controller" >&2
    exit 1
  fi
  if ! grep -Fq "@Deprecated" "$LEGACY_PROOF_RESOURCE_JAVA"; then
    echo "Legacy FinanceApplicationMysqlOracleProofResource must be explicitly marked deprecated" >&2
    exit 1
  fi
fi

if grep -R -n -F "FinanceApplicationMysqlOracleRegistryTest" "$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it" \
  --include 'test_*.sh' \
  --exclude "$(basename "$0")"; then
  echo "Current Sprint-33 IT scripts must run FinanceApplicationMysqlAuthorityRegistryTest instead of the legacy Oracle-named registry test" >&2
  exit 1
fi

if [[ ! -f "$AUTHORITY_SCORECARD_CHECK_TEST_JAVA" ]]; then
  echo "Missing canonical FinanceApplicationMysqlAuthorityScorecardCheckServiceTest.java" >&2
  exit 1
fi

if ! grep -Fq "FinanceApplicationMysqlAuthorityScorecardCheckServiceTest" "$SCORECARD_IT_SCRIPT"; then
  echo "Sprint-33 scorecard IT must run the canonical authority scorecard check test" >&2
  exit 1
fi

if grep -Fq "FinanceApplicationMysqlOracleScorecardCheckServiceTest" "$SCORECARD_IT_SCRIPT"; then
  echo "Sprint-33 scorecard IT must not run the legacy Oracle-named scorecard check test" >&2
  exit 1
fi

for required_test in \
  "$AUTHORITY_PROOF_SERVICE_TEST_JAVA" \
  "$AUTHORITY_PROOF_RUNNER_TEST_JAVA" \
  "$AUTHORITY_PROOF_RESOURCE_TEST_JAVA" \
  "$AUTHORITY_JDBC_CONFIG_TEST_JAVA"; do
  if [[ ! -f "$required_test" ]]; then
    echo "Missing canonical $(basename "$required_test")" >&2
    exit 1
  fi
done

for required_test_name in \
  FinanceApplicationMysqlAuthorityProofServiceTest \
  FinanceApplicationMysqlAuthorityProofRunnerTest \
  FinanceApplicationMysqlAuthorityScorecardCheckServiceTest \
  FinanceApplicationMysqlAuthorityProofResourceTest \
  FinanceApplicationMysqlAuthorityJdbcConfigurationTest; do
  if ! grep -Fq "$required_test_name" "$F2_PROOF_IT_SCRIPT"; then
    echo "Sprint-33 F2 proof IT must run $required_test_name" >&2
    exit 1
  fi
done

if grep -Eq "FinanceApplicationMysqlOracle(ProofService|ProofRunner|ProofResource|ScorecardCheckService|JdbcConfiguration)Test" "$F2_PROOF_IT_SCRIPT"; then
  echo "Sprint-33 F2 proof IT must not run legacy Oracle-named proof/config/scorecard tests" >&2
  exit 1
fi

for authority_runtime_script in \
  "$AUTHORITY_RUNTIME_HTTP_SCRIPT" \
  "$AUTHORITY_RUNTIME_LIVE_LOCAL_SCRIPT" \
  "$AUTHORITY_RUNTIME_REGISTERED_DATASOURCE_SCRIPT" \
  "$AUTHORITY_RUNTIME_CASE_IDS_CONTRACT"; do
  if grep -Fq "test_f2_application_mysql_oracle_runtime" "$authority_runtime_script"; then
    echo "Canonical application MySQL authority runtime scripts must own the implementation, not delegate to legacy oracle scripts" >&2
    echo "file: $authority_runtime_script" >&2
    exit 1
  fi
done

if ! grep -Fq "test_f2_application_mysql_authority_runtime_http.sh" "$LEGACY_ORACLE_RUNTIME_HTTP_SCRIPT"; then
  echo "Legacy oracle runtime HTTP script must delegate to the canonical authority runtime script" >&2
  exit 1
fi
if ! grep -Fq "test_f2_application_mysql_authority_runtime_live_local.sh" "$LEGACY_ORACLE_RUNTIME_LIVE_LOCAL_SCRIPT"; then
  echo "Legacy oracle runtime live-local script must delegate to the canonical authority runtime script" >&2
  exit 1
fi
if ! grep -Fq "test_f2_application_mysql_authority_runtime_registered_datasource.sh" "$LEGACY_ORACLE_RUNTIME_REGISTERED_DATASOURCE_SCRIPT"; then
  echo "Legacy oracle runtime registered-datasource script must delegate to the canonical authority runtime script" >&2
  exit 1
fi
if ! grep -Fq "test_f2_application_mysql_authority_runtime_case_ids_contract.sh" "$LEGACY_ORACLE_RUNTIME_CASE_IDS_CONTRACT"; then
  echo "Legacy oracle runtime case-id contract must delegate to the canonical authority runtime contract" >&2
  exit 1
fi

echo "[sprint33-application-mysql-authority-naming] authority naming contract ok"
