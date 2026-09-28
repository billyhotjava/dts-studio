#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../../../.."

guide="worklog/v1.0.0/sprint-32-202607/assets/multi-scenario-onboarding-guide.md"
kit="worklog/v1.0.0/sprint-32-202607/assets/scenario-onboarding-kit/README.md"
inventory="worklog/v1.0.0/sprint-32-202607/assets/scenario-onboarding-kit/generated/inventory"
evidence_dir="worklog/v1.0.0/sprint-32-202607/it/evidence/20260603-local"

test -f "$guide"
test -f "$kit"
test -f "$inventory/catalog-domain.json"
test -f "$inventory/semantic-packs/inventory.json"
test -f "$inventory/routing/inventory-route-map.md"

grep -q "0. 先选隔离方式" "$guide"
grep -q "六个必交付物" "$guide"
grep -q "Agent 五层路由接线" "$guide"
grep -q "deploy-per-scenario" "$guide"
grep -q "低库存预警已从不存在的固定资产路径修正" "$guide"
grep -q "库存 STG/DWD/DWS/ADS runtime build 已通过" "$guide"

for evidence in \
  f1-route-trace.md \
  f1-route-telemetry.md \
  f1-tier5-fallback-contract.md \
  f2-federated-sql-execution-gate.md \
  f2-trino-dbt-sales-root-cause.md \
  f2-trino-mysql-access-policy.md \
  f3-scenario-onboarding-kit.md \
  f4-inventory-dbt-ads-models.md \
  f4-inventory-dbt-runtime-build.md \
  f4-inventory-ads-reconciliation.md \
  f4-multi-scenario-isolation.md \
  f5-multi-scenario-guide.md \
  sprint32-completion-gate.md; do
  test -f "$evidence_dir/$evidence"
done

grep -q "test_sprint32_completion_gate.sh" "worklog/v1.0.0/sprint-32-202607/it/README.md"
grep -q "IT-09" "worklog/v1.0.0/sprint-32-202607/it/README.md"
grep -q "PASS" "worklog/v1.0.0/sprint-32-202607/it/README.md"

echo "[f5] multi-scenario guide and evidence index PASS"
