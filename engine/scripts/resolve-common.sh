#!/usr/bin/env bash
set -euo pipefail
repo="${DTS_MAVEN_REPOSITORY:-$HOME/.m2/repository}"
version=1.0.0
artifact_dir="$repo/com/yuzhi/dts/dts-common-pack/$version"
if [[ -f "$artifact_dir/dts-common-pack-$version.jar" && -f "$artifact_dir/dts-common-pack-$version-cli.jar" && -f "$artifact_dir/dts-common-pack-$version.pom" ]]; then
  echo "Using installed dts-common-pack $version"
  exit 0
fi
if [[ -z "${DTS_MAVEN_REPOSITORY_URL:-}" ]]; then
  echo 'dts-common-pack 1.0.0 is required. Install its release locally or set DTS_MAVEN_REPOSITORY_URL to the Maven registry containing it.' >&2
  exit 1
fi
for coordinate in "com.yuzhi.dts:dts-common-pack:$version" "com.yuzhi.dts:dts-common-pack:$version:jar:cli"; do
  "${MVN:-mvn}" -B -ntp "-Dmaven.repo.local=$repo" \
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get \
    "-Dartifact=$coordinate" "-DremoteRepositories=dts-releases::default::$DTS_MAVEN_REPOSITORY_URL" \
    -Dtransitive=false
 done
