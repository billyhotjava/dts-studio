#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
SPRINT_DIR="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607"
REQUIRE_DONE="${REQUIRE_SPRINT33_DONE:-false}"

pending_count=0

pending() {
  local id="$1"
  local reason="$2"
  pending_count=$((pending_count + 1))
  echo "pending.${pending_count}.id=${id}"
  echo "pending.${pending_count}.reason=${reason}"
}

require_file() {
  local file="$1"
  if [ ! -f "$file" ]; then
    echo "missing required file: ${file#$REPO_ROOT/}" >&2
    exit 2
  fi
}

require_file "$SPRINT_DIR/README.md"
require_file "$SPRINT_DIR/it/README.md"
require_file "$SPRINT_DIR/it/test_sprint33_worklog_consistency.sh"
require_file "$SPRINT_DIR/it/test_f1_authority_route_preflight.sh"
require_file "$SPRINT_DIR/features/F1-权威基准注册与明细级一致性/T02-明细级对账harness.md"
require_file "$SPRINT_DIR/features/F2-复式凭证tie-out与汇总双路对账/T02-汇总双路计算对账.md"
require_file "$SPRINT_DIR/features/F2-复式凭证tie-out与汇总双路对账/T03-收入应收回款锚到凭证科目.md"
require_file "$SPRINT_DIR/features/F4-差分抽样与持续对账记分卡/README.md"
require_file "$SPRINT_DIR/features/F5-可审计溯源与财务签字基线/README.md"
require_file "$SPRINT_DIR/assets/finance-signoff-baseline.md"

bash "$SPRINT_DIR/it/test_sprint33_worklog_consistency.sh" >/tmp/sprint33-worklog-consistency.out

if ! grep -Fq "**状态**: DONE" "$SPRINT_DIR/README.md"; then
  pending "sprint-status" "Sprint-33 README is not DONE"
fi

unchecked_count="$(grep -c '^- \[ \]' "$SPRINT_DIR/README.md" || true)"
if [ "$unchecked_count" -gt 0 ]; then
  pending "sprint-completion-criteria" "Sprint-33 README still has ${unchecked_count} unchecked completion criteria"
fi

if grep -Fq "| IN_PROGRESS" "$SPRINT_DIR/it/README.md"; then
  pending "it-in-progress" "Sprint-33 IT evidence matrix still has IN_PROGRESS rows"
fi

route_output="$(bash "$SPRINT_DIR/it/test_f1_authority_route_preflight.sh")"
route_status="$(printf '%s\n' "$route_output" | awk -F= '$1 == "status" {print $2; exit}')"
if [ "$route_status" != "ROUTE_READY" ]; then
  route_reason="$(printf '%s\n' "$route_output" | awk -F= '$1 == "reason" {print $2; exit}')"
  pending "f1-authority-route" "F1 live authority route is ${route_status:-UNKNOWN}${route_reason:+ (${route_reason})}"
fi

if grep -Fq -- "- [ ] live：真实 L2/签字 SQL vs copilot dataset 双路取数逐格差异为 0（或登记）" \
  "$SPRINT_DIR/features/F2-复式凭证tie-out与汇总双路对账/T02-汇总双路计算对账.md"; then
  pending "f2-summary-live-tieout" "F2 live L2/signoff SQL vs copilot dataset tie-out is still open"
fi

if grep -Fq -- "- [ ] live：抽样账期收入/应收/回款汇总与真实凭证科目合计相等，差异为 0 或分类登记" \
  "$SPRINT_DIR/features/F2-复式凭证tie-out与汇总双路对账/T03-收入应收回款锚到凭证科目.md"; then
  pending "f2-voucher-subject-live-tieout" "F2 live voucher subject tree/signoff tie-out is still open"
fi

if grep -Fq -- "- [ ] 代表性过滤网格（项目×月份×日期区间）copilot vs 权威基准端点 live 全绿（到分）" \
  "$SPRINT_DIR/features/F4-差分抽样与持续对账记分卡/README.md"; then
  pending "f4-live-differential-grid" "F4 live differential grid is still open"
fi

if grep -Fq -- "- [ ] 持续对账记分卡每日 live 可跑，输出通过率/漂移/逐项差异，差异越限告警" \
  "$SPRINT_DIR/features/F4-差分抽样与持续对账记分卡/README.md"; then
  pending "f4-live-scorecard" "F4 live scorecard publishing is still open"
fi

if ! grep -Fq "**签字状态**: SIGNED" "$SPRINT_DIR/assets/finance-signoff-baseline.md"; then
  pending "f5-signoff" "Finance/audit signoff baseline is not SIGNED"
fi

if grep -Fq -- "- [ ] 财务对一次完整对账基线（明细+汇总+凭证 tie-out）签字确认" \
  "$SPRINT_DIR/features/F5-可审计溯源与财务签字基线/README.md"; then
  pending "f5-business-acceptance" "F5 finance baseline business acceptance is still open"
fi

if [ "$pending_count" -eq 0 ]; then
  echo "status=READY_TO_MARK_DONE"
else
  echo "status=IN_PROGRESS"
fi
echo "pending_count=${pending_count}"

if [ "$REQUIRE_DONE" = "true" ] && [ "$pending_count" -gt 0 ]; then
  exit 10
fi
