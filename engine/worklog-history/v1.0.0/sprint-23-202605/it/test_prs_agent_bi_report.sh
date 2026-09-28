#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../" && pwd)"
EVIDENCE_DIR="${DTS_AGENT_BI_EVIDENCE_DIR:-${ROOT_DIR}/worklog/v1.0.0/sprint-23-202605/it/evidence/$(date +%Y%m%d-local)}"

mkdir -p "${EVIDENCE_DIR}"
cd "${ROOT_DIR}"

log() {
  printf '[agent-bi-it] %s\n' "$*" | tee -a "${EVIDENCE_DIR}/run.log"
}

run_step() {
  local name="$1"
  shift
  log "START ${name}: $*"
  "$@" 2>&1 | tee "${EVIDENCE_DIR}/${name}.log"
  log "PASS ${name}"
}

run_step ai-tests \
  mvn -pl dts-copilot-ai \
  -Dtest=AgentExecutionServiceTest,AgentChatServiceTest,AssetBackedPlannerPolicyTest,AgentBiReportCatalogServiceTest \
  test

run_step analytics-tests \
  mvn -pl dts-copilot-analytics \
  -Dtest=AnalyticsAnalysisDraftMappingTest,AnalysisDraftSchemaVerificationTest,AnalysisDraftResourceTest \
  test

run_step webapp-copilot-tests \
  pnpm --dir dts-copilot-webapp test \
  src/components/copilot/copilotAnalysisDraft.test.ts \
  src/components/copilot/copilotAnalysisDraftLinks.test.ts \
  src/components/copilot/copilotGeneratedReportMessage.test.ts

if [[ -n "${DTS_COPILOT_BASE_URL:-}" && -n "${DTS_COPILOT_COOKIE:-}" ]]; then
  log "START api-smoke"
  API_OUT="${EVIDENCE_DIR}/api-smoke.jsonl"
  : > "${API_OUT}"

  while IFS= read -r question; do
    [[ -z "${question}" || "${question}" == \#* ]] && continue
    payload=$(jq -nc --arg q "${question}" '{message:$q}')
    response=$(curl -sS -H "Content-Type: application/json" -H "Cookie: ${DTS_COPILOT_COOKIE}" \
      -X POST "${DTS_COPILOT_BASE_URL%/}/api/agent/chat" \
      --data "${payload}")
    printf '%s\n' "${response}" | jq -c --arg q "${question}" '{question:$q,responseKind:.responseKind,dataSurface:.dataSurface,qualityLevel:.qualityLevel,reportCode:.reportCode,raw:.}' >> "${API_OUT}"
  done <<'QUESTIONS'
打开 PRS 租赁经营总览大屏
从 2025 年 5 月到现在，租赁收入按月趋势怎么样
本月待审批报花单按类型统计
这个项目有哪些待确认账单
帮我发起催收任务
QUESTIONS

  log "PASS api-smoke"
else
  log "SKIP api-smoke: set DTS_COPILOT_BASE_URL and DTS_COPILOT_COOKIE to run live API checks"
fi

log "Evidence written to ${EVIDENCE_DIR}"
