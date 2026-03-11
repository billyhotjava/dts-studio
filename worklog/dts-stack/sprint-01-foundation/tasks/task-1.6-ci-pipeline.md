# Task 1.6: CI Pipeline — GitHub Actions

**Sprint:** 1 — Foundation
**Points:** 3
**Status:** TODO

## Files

- Create: `dts-stack/.github/workflows/ci.yml`

## Step 1: Create CI workflow

```yaml
name: DTS CI
on:
  pull_request:
    branches: [main, develop]
  push:
    branches: [develop]

jobs:
  java:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: cd source && mvn clean verify -T1C

  python:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: astral-sh/setup-uv@v3
      - run: cd ai && uv sync && uv run ruff check . && uv run pytest

  go:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-go@v5
        with: { go-version: '1.22' }
      - run: cd infra && go build ./... && go test ./...

  proto:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: bufbuild/buf-action@v1
        with:
          input: proto
```

## Step 2: Commit

```bash
git add dts-stack/.github/
git commit -m "feat(s1): add CI pipeline for Java/Python/Go/Proto"
```
