#!/usr/bin/env bash
# Compare legacy and seed-free startup in separate disposable PostgreSQL instances.
set -euo pipefail
if [[ $# -ne 2 || ! -f "$1" ]]; then
  echo 'Usage: verify-pack-bootstrap.sh /absolute/prs-flower.dtspack /artifact/directory' >&2
  exit 2
fi
pack_archive="$(realpath -- "$1")"
mkdir -p -- "$2"
artifact_parent="$(realpath -- "$2")"
artifact_dir="$(mktemp -d "$artifact_parent/pack-bootstrap.XXXXXX")"
studio_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
export STUDIO_TEST_POSTGRES_IMAGE="${STUDIO_TEST_POSTGRES_IMAGE:-pgvector/pgvector:pg17}"
cd "$studio_dir"
echo "Bootstrap verification artifacts: $artifact_dir"
./engine/scripts/with-test-postgres.sh mvn -B -ntp -f engine/pom.xml -pl engine-ai \
  -Dtest=PackRuntimeSmokeIT "-Dstudio.pack.archive=$pack_archive" \
  "-Dstudio.pack.golden-output=$artifact_dir/legacy-golden.json" test > "$artifact_dir/legacy.log" 2>&1
cp engine/engine-ai/target/surefire-reports/TEST-com.yuzhi.dts.copilot.ai.service.pack.PackRuntimeSmokeIT.xml \
  "$artifact_dir/legacy.junit.xml"
./engine/scripts/with-test-postgres.sh mvn -B -ntp -f engine/pom.xml -pl engine-ai \
  -Dtest=PackFreshBootstrapSmokeIT "-Dstudio.pack.archive=$pack_archive" \
  "-Dstudio.pack.golden-input=$artifact_dir/legacy-golden.json" test > "$artifact_dir/fresh.log" 2>&1
cp engine/engine-ai/target/surefire-reports/TEST-com.yuzhi.dts.copilot.ai.service.pack.PackFreshBootstrapSmokeIT.xml \
  "$artifact_dir/fresh.junit.xml"
echo 'PASS: legacy and Pack-only HTTP/SSE, template baseline, restart and mode-switch protection'
