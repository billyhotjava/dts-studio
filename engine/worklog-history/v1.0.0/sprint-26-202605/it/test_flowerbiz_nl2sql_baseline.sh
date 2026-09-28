#!/usr/bin/env bash
# Sprint-26 F0/T03 报花域 NL2SQL 路由基线校验
# 默认（静态）：校验对照集结构 + 期望 ADS 视图与 flowerbiz.json objects 一致。
# RUN_TEST=1：额外离线运行 JUnit 路由基线测试 FlowerbizNl2SqlBaselineTest。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
TSV="$ROOT_DIR/worklog/v1.0.0/sprint-26-202605/it/sql/flowerbiz_baseline_questions.tsv"
PACK="$ROOT_DIR/dts-copilot-ai/src/main/resources/semantic-packs/flowerbiz.json"

test -f "$TSV"
test -f "$PACK"

# 1) 对照集至少覆盖 8 条 fewShot + 2 条护栏 = 10 行
rows=$(grep -vc '^#' "$TSV")
if [[ "$rows" -lt 10 ]]; then
  echo "baseline rows < 10 (got $rows)" >&2
  exit 1
fi

# 2) 每个期望的 ADS 视图必须存在于 flowerbiz.json，防止基线漂移到不存在的 mart
missing=0
while IFS=$'\t' read -r id question domain mode kind view note; do
  [[ "$id" =~ ^#.*$ || -z "${id:-}" ]] && continue
  case "$view" in
    public.xycyl_ads_flowerbiz_*)
      bare="${view#public.}"
      if ! grep -Fq "$bare" "$PACK"; then
        echo "expected view not found in flowerbiz.json: $view (row $id)" >&2
        missing=1
      fi
      ;;
    business-object:*|-) : ;;  # 护栏行：L0 业务对象 / 口径歧义，无 ADS 视图
    *) echo "unexpected view token: $view (row $id)" >&2; missing=1 ;;
  esac
done < "$TSV"
[[ "$missing" -eq 0 ]]

# 3) 护栏行存在性：口径歧义 + 单据状态画像
grep -Fq "BUSINESS_CLARIFICATION" "$TSV"
grep -Fq "business-object:prs.flowerbiz.biz_order" "$TSV"

echo "[static] flowerbiz NL2SQL baseline consistency OK ($rows rows)"

# 4) 可选：离线运行 JUnit 路由基线测试
if [[ "${RUN_TEST:-0}" == "1" ]]; then
  cd "$ROOT_DIR"
  mvn -o -q -pl dts-copilot-ai test \
    -Dtest=FlowerbizNl2SqlBaselineTest -DfailIfNoTests=false
  echo "[junit] FlowerbizNl2SqlBaselineTest passed"
fi
