#!/usr/bin/env bash
# Sprint-26 F0/T01-T02 semantic-pack schema and OntologyService load checks.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
PACK="$ROOT_DIR/dts-copilot-ai/src/main/resources/semantic-packs/flowerbiz.json"

test -f "$PACK"

grep -Fq '"links": []' "$PACK"
grep -Fq '"metrics": []' "$PACK"
grep -Fq '"signals": []' "$PACK"
grep -Fq '"actions": []' "$PACK"

echo "[static] flowerbiz ontology sections are explicitly present and empty"

if [[ "${RUN_TEST:-0}" == "1" ]]; then
  cd "$ROOT_DIR"
  mvn -pl dts-copilot-ai test \
    -Dtest=SemanticPackOntologySchemaTest,OntologyServiceTest,SemanticPackServiceTest \
    -DfailIfNoTests=false
  echo "[junit] ontology schema/load tests passed"
fi
