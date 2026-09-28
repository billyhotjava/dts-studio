#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

SPRINT_README="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/README.md"
F1_README="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/features/F1-权威基准注册与明细级一致性/README.md"
F1_T01="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/features/F1-权威基准注册与明细级一致性/T01-财务权威基准注册表.md"
F1_T02="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/features/F1-权威基准注册与明细级一致性/T02-明细级对账harness.md"
F2_README="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/features/F2-复式凭证tie-out与汇总双路对账/README.md"
F2_T01="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/features/F2-复式凭证tie-out与汇总双路对账/T01-凭证账本接入.md"
F2_T02="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/features/F2-复式凭证tie-out与汇总双路对账/T02-汇总双路计算对账.md"
F4_T02="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/features/F4-差分抽样与持续对账记分卡/T02-持续对账记分卡.md"
IT_README="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/README.md"
F1_ROUTE_PREFLIGHT="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_f1_authority_route_preflight.sh"
F4_SCORECARD_PREFLIGHT="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_f4_scorecard_live_preflight.sh"
F4_SCORECARD_PREFLIGHT_EVIDENCE="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/evidence/20260607-local/f4-scorecard-live-preflight.md"
F1_AUTHORITY_REGISTRY_EVIDENCE="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/evidence/20260605-local/finance-authority-registry-test.log"
F1_AUTHORITY_AUTH_EVIDENCE="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/evidence/20260605-local/f1-live-authority-auth-precheck.md"
F1_AUTHORITY_ADMIN_ROUTE_EVIDENCE="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/evidence/20260605-local/f1-live-authority-admin-session-route-precheck.md"
F2_APPLICATION_MYSQL_AUTHORITY_EVIDENCE="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/evidence/20260606-local/f2-application-mysql-authority-proof.md"
COMPLETION_READINESS="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_sprint33_completion_readiness.sh"
LOCAL_CONTRACTS="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_sprint33_local_contracts.sh"
AUTHORITY_NAMING="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_sprint33_authority_registry_naming.sh"
FINANCE_AUTHORITY_REGISTRY_GATE="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_sprint33_finance_authority_registry.sh"
APPLICATION_MYSQL_AUTHORITY_PROOF_GATE="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_proof.sh"
DOCKER_COMPOSE="$REPO_ROOT/docker-compose.yml"

require_fixed() {
  local file="$1"
  local text="$2"
  local message="$3"

  if ! grep -Fq -- "$text" "$file"; then
    echo "$message" >&2
    echo "missing text: $text" >&2
    echo "file: $file" >&2
    exit 1
  fi
}

require_fixed "$F1_README" "| T01 | 财务权威基准注册表（报表↔权威端点/账本） | P0 | DONE | - |" \
  "F1-T01 is not recorded as DONE in the feature task list"
require_fixed "$F1_T01" "- [x] 权威基准注册表成文，作为 T02/F2/F4 对账的唯一基准来源" \
  "F1-T01 completion criteria no longer prove the authority registry is documented"
if [ ! -f "$FINANCE_AUTHORITY_REGISTRY_GATE" ]; then
  echo "Sprint-33 must expose a canonical finance authority registry gate" >&2
  echo "missing file: $FINANCE_AUTHORITY_REGISTRY_GATE" >&2
  exit 1
fi
if [ ! -f "$F1_AUTHORITY_REGISTRY_EVIDENCE" ]; then
  echo "Sprint-33 must store current F1 registry evidence under the canonical authority filename" >&2
  echo "missing file: $F1_AUTHORITY_REGISTRY_EVIDENCE" >&2
  exit 1
fi
require_fixed "$IT_README" "| IT-01 | 财务权威基准注册表 | F1-T01 | \`it/test_sprint33_finance_authority_registry.sh\` + \`it/test_sprint33_authority_registry_naming.sh\` | PASS" \
  "IT-01 no longer records the authority registry gate as PASS"

require_fixed "$SPRINT_README" "- [x] 财务权威基准注册表成文：每张核心报表↔权威端点/账本明确绑定" \
  "Sprint-33 top-level completion criteria must mark the documented authority registry as complete once F1-T01 and IT-01 are DONE/PASS"

require_fixed "$F2_README" "| T01 | 凭证账本接入（借=贷 复式作为汇总锚） | P0 | DONE | F1-T01 |" \
  "F2-T01 is not recorded as DONE in the feature task list"
require_fixed "$F2_T01" "- [x] 凭证账本可作为 T03 tie-out 锚，映射成文" \
  "F2-T01 completion criteria no longer prove the voucher ledger anchor is documented"
require_fixed "$IT_README" "| IT-04 | 复式凭证 tie-out（借=贷） | F2-T01/T03 | \`assets/voucher-tieout-mapping.md\` + \`it/test_sprint33_voucher_tieout_mapping.sh\` + \`it/test_f2_voucher_subject_tieout.sh\` | IN_PROGRESS（T01 mapping/self-check PASS" \
  "IT-04 must keep the voucher tie-out lane open while recording F2-T01 mapping/self-check as PASS"
require_fixed "$F2_README" "- [x] 凭证账本（debit/credit + subjectId）可作为对账锚接入" \
  "F2 feature completion criteria must mark the voucher ledger anchor complete once F2-T01 and IT-04 mapping/self-check are DONE/PASS"

require_fixed "$F1_T01" "- [ ] 注册的端点实际可调通并返回与 adminweb 一致的数" \
  "The worklog must keep live endpoint callability open until real L2/adminapi evidence exists"
require_fixed "$SPRINT_README" "- [ ] 明细级对账 harness：真实业务单 copilot 结果与 L2 报表端点逐额（到分）相等，三级金额列各自对齐" \
  "The sprint must not mark live L2 detail reconciliation complete before real endpoint evidence exists"
require_fixed "$F1_T02" 'it/test_f1_authority_route_preflight.sh' \
  "F1-T02 must document the live authority route preflight command"
require_fixed "$IT_README" "it/test_f1_authority_route_preflight.sh" \
  "IT-02 must include the live authority route preflight command"
if [ ! -f "$F1_AUTHORITY_AUTH_EVIDENCE" ]; then
  echo "Sprint-33 must store current F1 auth precheck evidence under the canonical authority filename" >&2
  echo "missing file: $F1_AUTHORITY_AUTH_EVIDENCE" >&2
  exit 1
fi
if [ ! -f "$F1_AUTHORITY_ADMIN_ROUTE_EVIDENCE" ]; then
  echo "Sprint-33 must store current F1 admin-session route precheck evidence under the canonical authority filename" >&2
  echo "missing file: $F1_AUTHORITY_ADMIN_ROUTE_EVIDENCE" >&2
  exit 1
fi
require_fixed "$F1_T02" "it/evidence/20260605-local/f1-live-authority-auth-precheck.md" \
  "F1-T02 must reference the canonical authority auth precheck evidence"
require_fixed "$F1_T02" "it/evidence/20260605-local/f1-live-authority-admin-session-route-precheck.md" \
  "F1-T02 must reference the canonical authority admin-session route precheck evidence"
require_fixed "$IT_README" "it/evidence/20260605-local/f1-live-authority-auth-precheck.md" \
  "IT-02 must reference the canonical authority auth precheck evidence"
require_fixed "$IT_README" "it/evidence/20260605-local/f1-live-authority-admin-session-route-precheck.md" \
  "IT-02 must reference the canonical authority admin-session route precheck evidence"
require_fixed "$F1_ROUTE_PREFLIGHT" "REQUIRE_LIVE_F1_AUTHORITY_ROUTE" \
  "F1 authority route preflight must support an explicit live-required gate"
require_fixed "$IT_README" "it/test_f1_authority_route_preflight_contract.sh" \
  "IT-02 must include the F1 authority route preflight diagnostics contract command"
require_fixed "$IT_README" "it/test_sprint33_completion_readiness.sh" \
  "Sprint-33 IT README must expose the completion readiness gate"
require_fixed "$COMPLETION_READINESS" "REQUIRE_SPRINT33_DONE" \
  "Sprint-33 completion readiness must support an explicit DONE-required gate"
require_fixed "$IT_README" "it/test_sprint33_local_contracts.sh" \
  "Sprint-33 IT README must expose the local contract aggregation gate"
require_fixed "$IT_README" "it/test_sprint33_authority_registry_naming.sh" \
  "Sprint-33 IT README must expose the authority registry naming gate"
require_fixed "$IT_README" "it/test_sprint33_application_mysql_authority_naming.sh" \
  "Sprint-33 IT README must expose the application MySQL authority naming gate"
if [ ! -f "$APPLICATION_MYSQL_AUTHORITY_PROOF_GATE" ]; then
  echo "Sprint-33 must expose a canonical application MySQL authority proof gate" >&2
  echo "missing file: $APPLICATION_MYSQL_AUTHORITY_PROOF_GATE" >&2
  exit 1
fi
require_fixed "$IT_README" "it/test_f2_application_mysql_authority_proof.sh" \
  "Sprint-33 IT README must expose the canonical application MySQL authority proof gate"
if [ ! -f "$F2_APPLICATION_MYSQL_AUTHORITY_EVIDENCE" ]; then
  echo "Sprint-33 must store current F2 proof evidence under the canonical authority filename" >&2
  echo "missing file: $F2_APPLICATION_MYSQL_AUTHORITY_EVIDENCE" >&2
  exit 1
fi
require_fixed "$IT_README" "it/evidence/20260606-local/f2-application-mysql-authority-proof.md" \
  "Sprint-33 IT README must reference the canonical application MySQL authority proof evidence"
require_fixed "$LOCAL_CONTRACTS" "test_sprint33_finance_authority_registry.sh" \
  "Sprint-33 local contract aggregation gate must run the canonical finance authority registry gate"
require_fixed "$LOCAL_CONTRACTS" "test_f2_application_mysql_authority_proof.sh" \
  "Sprint-33 local contract aggregation gate must run the canonical application MySQL authority proof gate"
require_fixed "$LOCAL_CONTRACTS" "test_sprint33_authority_registry_naming.sh" \
  "Sprint-33 local contract aggregation gate must include the authority registry naming gate"
require_fixed "$LOCAL_CONTRACTS" "test_sprint33_application_mysql_authority_naming.sh" \
  "Sprint-33 local contract aggregation gate must include the application MySQL authority naming gate"
require_fixed "$F1_T01" "finance-authority-registry.v1.json" \
  "F1-T01 must document the canonical authority registry governance asset"
require_fixed "$F2_T02" "finance-application-mysql-authority-sql.v1.json" \
  "F2 worklog must document the canonical application MySQL authority SQL governance asset"
require_fixed "$F2_T02" "FinanceApplicationMysqlAuthorityJdbcConfiguration" \
  "F2 worklog must document the canonical application MySQL authority JDBC configuration"
require_fixed "$F2_T02" "FinanceApplicationMysqlAuthorityProofResource" \
  "F2 worklog must document the canonical application MySQL authority proof REST resource"
require_fixed "$AUTHORITY_NAMING" "FinanceAuthorityRegistry.java" \
  "Authority registry naming gate must lock the canonical Java registry"
require_fixed "$LOCAL_CONTRACTS" "test_f1_authority_route_preflight_contract.sh" \
  "Sprint-33 local contract aggregation gate must include the F1 authority route preflight diagnostics contract"
require_fixed "$IT_README" "it/test_f4_scorecard_live_preflight_contract.sh" \
  "IT-08 must include the F4 scorecard live preflight diagnostics contract command"
require_fixed "$IT_README" "it/test_f4_scorecard_live_preflight.sh" \
  "IT-08 must include the F4 scorecard live preflight command"
require_fixed "$LOCAL_CONTRACTS" "test_f4_scorecard_live_preflight_contract.sh" \
  "Sprint-33 local contract aggregation gate must include the F4 scorecard live preflight diagnostics contract"
require_fixed "$LOCAL_CONTRACTS" "test_f4_scorecard_live_preflight.sh" \
  "Sprint-33 local contract aggregation gate must include the F4 scorecard live preflight"
require_fixed "$LOCAL_CONTRACTS" "test_f5_finance_signoff_baseline.sh" \
  "Sprint-33 local contract aggregation gate must include the F5 signoff baseline contract"
require_fixed "$F4_SCORECARD_PREFLIGHT" "COPILOT_ADMIN_SECRET" \
  "F4 scorecard live preflight must support admin-secret temporary API key creation"
require_fixed "$DOCKER_COMPOSE" 'FINANCE_RECONCILIATION_AUTHORITY_BASE_URL: ${FINANCE_RECONCILIATION_AUTHORITY_BASE_URL:-${FINANCE_RECONCILIATION_ORACLE_BASE_URL:-}}' \
  "docker-compose must expose the canonical authority base URL and keep legacy oracle env only as fallback"
require_fixed "$DOCKER_COMPOSE" 'COPILOT_FINANCE_RECONCILIATION_LOCAL_EVIDENCE_ENABLED: ${COPILOT_FINANCE_RECONCILIATION_LOCAL_EVIDENCE_ENABLED:-true}' \
  "docker-compose must enable the honest local scorecard evidence provider by default"
require_fixed "$F4_T02" "COPILOT_ADMIN_SECRET" \
  "F4-T02 must document COPILOT_ADMIN_SECRET as a live publish option"
require_fixed "$IT_README" "COPILOT_ADMIN_SECRET" \
  "Sprint-33 IT README must document COPILOT_ADMIN_SECRET as a live scorecard publish option"
if [ ! -f "$F4_SCORECARD_PREFLIGHT_EVIDENCE" ]; then
  echo "F4 scorecard live preflight evidence must be recorded" >&2
  echo "missing file: $F4_SCORECARD_PREFLIGHT_EVIDENCE" >&2
  exit 1
fi
require_fixed "$F4_SCORECARD_PREFLIGHT_EVIDENCE" "publish_http_status=200" \
  "F4 scorecard live preflight evidence must record the publish endpoint HTTP status"
require_fixed "$F4_SCORECARD_PREFLIGHT_EVIDENCE" "diagnostic.publish_status=PENDING_LIVE_EVIDENCE" \
  "F4 scorecard live preflight evidence must record the scheduled publisher status"
require_fixed "$F4_SCORECARD_PREFLIGHT_EVIDENCE" "diagnostic.skipped_reason_code=PENDING_LIVE_EVIDENCE" \
  "F4 scorecard live preflight evidence must record the scheduled publisher skip reason code"
require_fixed "$F4_SCORECARD_PREFLIGHT_EVIDENCE" "reason=scorecard_publish_pending_live_evidence" \
  "F4 scorecard live preflight evidence must keep the live scorecard blocker explicit"

if grep -R -n -E "## Oracle 入口|oracle 与 copilot" \
  "$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/assets" \
  "$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/features" \
  "$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/README.md"; then
  echo "Current Sprint-33 worklog assets must use authority wording for user-visible proof sources" >&2
  exit 1
fi

if grep -R -n -E "test_sprint33_finance_oracle_registry|test_f2_application_mysql_oracle_(proof|runtime)" \
  "$IT_README" \
  "$F2_T02" \
  "$LOCAL_CONTRACTS"; then
  echo "Current Sprint-33 recommended gates must use canonical authority script names, not legacy oracle script names" >&2
  exit 1
fi

if grep -R -n -F "f2-application-mysql-oracle-proof.md" \
  "$IT_README" \
  "$F2_T02" \
  "$SPRINT_README"; then
  echo "Current Sprint-33 recommended evidence must use the canonical authority proof filename" >&2
  exit 1
fi

if grep -R -n -E "f1-live-oracle-(auth|admin-session-route)-precheck\\.md" \
  "$IT_README" \
  "$F1_T02" \
  "$SPRINT_README"; then
  echo "Current Sprint-33 recommended F1 evidence must use canonical authority filenames" >&2
  exit 1
fi

if [ -f "$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/evidence/20260605-local/finance-oracle-registry-test.log" ]; then
  echo "Current Sprint-33 registry evidence must use the canonical authority filename" >&2
  exit 1
fi

echo "[sprint33-worklog] consistency ok"
