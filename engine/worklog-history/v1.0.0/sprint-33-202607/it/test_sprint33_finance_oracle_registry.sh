#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

bash "$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_sprint33_finance_authority_registry.sh" "$@"
