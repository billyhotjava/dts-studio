#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

AUTHORITY_REGISTRY_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceAuthorityRegistry.java"
LEGACY_ORACLE_REGISTRY_JAVA="$REPO_ROOT/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/FinanceOracleRegistry.java"
AUTHORITY_REGISTRY_JSON="$REPO_ROOT/dts-copilot-ai/src/main/resources/governance/finance-authority-registry.v1.json"
AUTHORITY_REGISTRY_MD="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/assets/finance-authority-registry.md"
INVARIANTS_JSON="$REPO_ROOT/dts-copilot-ai/src/main/resources/governance/finance-invariants.v1.json"

if [[ ! -f "$AUTHORITY_REGISTRY_JAVA" ]]; then
  echo "Missing canonical FinanceAuthorityRegistry.java; authority registry must not stay named as Oracle" >&2
  exit 1
fi

if [[ ! -f "$AUTHORITY_REGISTRY_JSON" ]]; then
  echo "Missing canonical finance-authority-registry.v1.json governance asset" >&2
  exit 1
fi

if grep -Fq "finance-oracle-registry" "$AUTHORITY_REGISTRY_JSON"; then
  echo "Canonical authority registry asset must not identify itself as finance-oracle-registry" >&2
  exit 1
fi

if [[ ! -f "$AUTHORITY_REGISTRY_MD" ]]; then
  echo "Missing canonical Sprint-33 finance-authority-registry.md asset" >&2
  exit 1
fi

if grep -Fq 'finance-oracle-registry.md' "$AUTHORITY_REGISTRY_MD"; then
  echo "Canonical Sprint-33 authority registry markdown must not identify itself as finance-oracle-registry.md" >&2
  exit 1
fi

if ! grep -Fq 'governance/finance-authority-registry.v1.json' "$AUTHORITY_REGISTRY_JAVA"; then
  echo "FinanceAuthorityRegistry must load the canonical authority governance asset" >&2
  exit 1
fi

if [[ -f "$LEGACY_ORACLE_REGISTRY_JAVA" ]]; then
  if grep -Eq '^[[:space:]]*@Service' "$LEGACY_ORACLE_REGISTRY_JAVA"; then
    echo "Legacy FinanceOracleRegistry must not be the Spring service bean" >&2
    exit 1
  fi
  if ! grep -Fq "@Deprecated" "$LEGACY_ORACLE_REGISTRY_JAVA"; then
    echo "Legacy FinanceOracleRegistry must be explicitly marked deprecated" >&2
    exit 1
  fi
fi

if grep -Fq 'finance-oracle-registry.v1.json' "$INVARIANTS_JSON"; then
  echo "Finance invariants must reference the authority registry governance asset, not the legacy oracle filename" >&2
  exit 1
fi

if grep -R -n -F "FinanceOracleRegistryTest" "$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it" \
  --include 'test_*.sh' \
  --exclude "$(basename "$0")"; then
  echo "Current Sprint-33 IT scripts must run FinanceAuthorityRegistryTest instead of the legacy Oracle-named test" >&2
  exit 1
fi

echo "[sprint33-authority-registry-naming] authority registry naming contract ok"
