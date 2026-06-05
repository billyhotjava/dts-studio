#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../../../.."

mvn -q \
  -Dmaven.repo.local=/opt/prod/prs/source/.m2 \
  -pl dts-copilot-ai \
  -Dtest=AssetBackedPlannerPolicyTest,CopilotChatContractTest \
  test
