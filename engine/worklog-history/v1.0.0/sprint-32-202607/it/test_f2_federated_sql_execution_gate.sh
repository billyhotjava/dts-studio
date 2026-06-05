#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
MAVEN_REPO_LOCAL="${MAVEN_REPO_LOCAL:-/opt/prod/prs/source/.m2}"

cd "$ROOT_DIR"

mvn -q -Dmaven.repo.local="$MAVEN_REPO_LOCAL" \
  -pl dts-copilot-analytics \
  -Dtest=DatasetQueryServiceTest,DefaultFederatedNativeSqlQualifierTest,QueryExecutionFacadeTest,FederatedQueryGuardrailTest \
  test
