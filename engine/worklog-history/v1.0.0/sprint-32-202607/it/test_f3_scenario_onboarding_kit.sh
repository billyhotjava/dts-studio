#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../../../.."

SCRIPT="scripts/scaffold-scenario-kit.mjs"
KIT_DIR="worklog/v1.0.0/sprint-32-202607/assets/scenario-onboarding-kit"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

test -f "$SCRIPT"
test -f "$KIT_DIR/README.md"
test -f "$KIT_DIR/examples/flowerbiz.md"
test -f "$KIT_DIR/examples/finance.md"

for required in \
  "catalog-domain.json.tmpl" \
  "semantic-packs/{{sceneCode}}.json.tmpl" \
  "trino-catalog/{{sceneCode}}.properties.tmpl" \
  "glossary/{{sceneCode}}-glossary.yml.tmpl" \
  "routing/{{sceneCode}}-route-map.md.tmpl" \
  "dbt/models/stg/{{sceneCode}}_stg_placeholder.sql.tmpl" \
  "dbt/models/dwd/{{sceneCode}}_dwd_placeholder.sql.tmpl" \
  "dbt/models/dws/{{sceneCode}}_dws_monthly.sql.tmpl" \
  "dbt/models/ads/{{sceneCode}}_ads_overview.sql.tmpl" \
  "dbt/models/schema.yml.tmpl"; do
  test -f "$KIT_DIR/templates/$required"
done

node "$SCRIPT" \
  --scene-code inventory \
  --domain-name "库存" \
  --owner warehouse-team \
  --dry-run | grep -q "would write:"

node "$SCRIPT" \
  --scene-code inventory \
  --domain-name "库存" \
  --owner warehouse-team \
  --output-dir "$TMP_DIR" \
  --force >/dev/null

jq -e '.domain == "inventory" and .domainName == "库存"' \
  "$TMP_DIR/inventory/catalog-domain.json" >/dev/null
jq -e '.domain == "inventory" and (.guardrails | length >= 3)' \
  "$TMP_DIR/inventory/semantic-packs/inventory.json" >/dev/null

test -f "$TMP_DIR/inventory/dbt/models/stg/inventory_stg_placeholder.sql"
test -f "$TMP_DIR/inventory/dbt/models/dwd/inventory_dwd_placeholder.sql"
test -f "$TMP_DIR/inventory/dbt/models/dws/inventory_dws_monthly.sql"
test -f "$TMP_DIR/inventory/dbt/models/ads/inventory_ads_overview.sql"
test -f "$TMP_DIR/inventory/trino-catalog/inventory.properties"

grep -q "ref('inventory_stg_placeholder')" \
  "$TMP_DIR/inventory/dbt/models/dwd/inventory_dwd_placeholder.sql"
grep -q "mysql.rs_cloud_flower" \
  "$TMP_DIR/inventory/routing/inventory-route-map.md"

echo "[F3] scenario onboarding kit scaffold verified"
