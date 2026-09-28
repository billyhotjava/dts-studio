#!/usr/bin/env bash
# Build the imported backend without loading deployment credentials or starting services.
set -euo pipefail
engine_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
action="${1:-verify}"
if [[ $# -gt 0 ]]; then shift; fi

case "$action" in
  verify|package)
    exec "$engine_dir/scripts/with-test-postgres.sh" \
      "${MVN:-mvn}" -B -ntp -f "$engine_dir/pom.xml" clean "$action" "$@"
    ;;
  --help|-h|help)
    echo 'Usage: ./build.sh [verify|package] [Maven arguments...]'
    echo 'Default: clean verify, including backend tests with an isolated disposable PostgreSQL.'
    echo 'Requires a local postgres:18.4 image (or STUDIO_TEST_POSTGRES_IMAGE). Does not deploy.'
    ;;
  *)
    echo "Unsupported build action: $action" >&2
    exit 2
    ;;
esac
