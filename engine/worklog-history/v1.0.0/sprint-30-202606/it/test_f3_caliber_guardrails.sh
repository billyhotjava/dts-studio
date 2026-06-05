#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SPRINT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"
AI_DIR="$ROOT_DIR/dts-copilot/dts-copilot-ai"
ASSET_DIR="$SPRINT_DIR/assets"
EVIDENCE_DIR="$SCRIPT_DIR/evidence/20260601-local"

FLOWERBIZ_PACK="$AI_DIR/src/main/resources/semantic-packs/flowerbiz.json"
FINANCE_PACK="$AI_DIR/src/main/resources/semantic-packs/finance.json"
QUESTIONS_TSV="$SCRIPT_DIR/sql/caliber-regression-questions.tsv"
ENUM_DICT="$ASSET_DIR/biz-type-enum-dictionary.md"
SUMMARY="$EVIDENCE_DIR/f3-caliber-guardrails-summary.md"

RULE_IDS=(
  CAL-BIZTYPE-SCOPE
  CAL-SETTLEMENT-CHAIN
  CAL-MONTH-AMOUNT-TIER
  CAL-SALE-IN-RENT
  CAL-RENT-HISTORY
  CAL-INVENTORY-COST
  CAL-JSON-EXPAND
  CAL-VARCHAR-AMOUNT-CAST
  CAL-EXTRA-COST-VS-EXPENSE
)

mkdir -p "$EVIDENCE_DIR"

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "missing required file: $path" >&2
    return 1
  fi
}

require_pack_rules() {
  local pack="$1"
  local domain="$2"
  require_file "$pack"
  for rule_id in "${RULE_IDS[@]}"; do
    jq -e --arg rule_id "[$rule_id]" '.guardrails[] | select(contains($rule_id))' "$pack" >/dev/null
  done
  local count
  count="$(jq '.guardrails | length' "$pack")"
  if [[ "$count" -lt "${#RULE_IDS[@]}" ]]; then
    echo "$domain guardrail count $count is below ${#RULE_IDS[@]}" >&2
    return 1
  fi
}

require_questions() {
  require_file "$QUESTIONS_TSV"
  for rule_id in "${RULE_IDS[@]}"; do
    awk -F '\t' -v rule_id="$rule_id" 'NR > 1 && $3 ~ rule_id { found = 1 } END { exit found ? 0 : 1 }' "$QUESTIONS_TSV"
  done
}

require_enum_dictionary() {
  require_file "$ENUM_DICT"
  local needles=(
    "t_flower_biz_info.biz_type"
    "t_flower_biz_item.biz_type"
    "FlowerBizInfoServiceImpl.getNewFlowerBizCode"
    "a_invoice_item.biz_type"
    "a_collection_item.biz_type"
    "t_warehousing_info.warehousing_type"
    "t_ex_warehouse_info.out_house_type"
    "a_green_accounting.source_type"
  )
  for needle in "${needles[@]}"; do
    grep -F "$needle" "$ENUM_DICT" >/dev/null
  done
}

require_pack_rules "$FLOWERBIZ_PACK" flowerbiz
require_pack_rules "$FINANCE_PACK" finance
require_questions
require_enum_dictionary

{
  echo "# F3 Caliber Guardrails Evidence"
  echo
  echo "- flowerbiz guardrails: $(jq '.guardrails | length' "$FLOWERBIZ_PACK")"
  echo "- finance guardrails: $(jq '.guardrails | length' "$FINANCE_PACK")"
  echo "- regression questions: $(awk 'END { print NR > 0 ? NR - 1 : 0 }' "$QUESTIONS_TSV")"
  echo "- enum dictionary: $ENUM_DICT"
  echo
  echo "## Rule IDs"
  printf -- "- %s\n" "${RULE_IDS[@]}"
} > "$SUMMARY"

echo "F3 caliber guardrails passed: $SUMMARY"
