#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
EXECUTOR_FILE="$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/OntologyActionExecutor.java"
CLIENT_FILE="$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/AdminApiActionClient.java"
TEST_FILE="$ROOT_DIR/dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/OntologyActionExecutorTest.java"

grep -q "postDraft" "$EXECUTOR_FILE"
grep -q "endpoint().draft()" "$EXECUTOR_FILE"
grep -q "postCommit" "$CLIENT_FILE"
grep -q "never()).postCommit" "$TEST_FILE"
grep -q "Missing required action param" "$TEST_FILE"
grep -q "参数错误" "$TEST_FILE"

if grep -q "postCommit" "$EXECUTOR_FILE"; then
  echo "[static] executor must not call postCommit" >&2
  exit 1
fi

echo "[static] action executor is locked to draft endpoint"

if [[ "${RUN_TEST:-0}" == "1" ]]; then
  (cd "$ROOT_DIR" && mvn -pl dts-copilot-ai -Dtest=OntologyActionExecutorTest,SemanticPackOntologySchemaTest test)
  echo "[junit] action executor safety tests passed"
fi
