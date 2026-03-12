# Task 1.4: Go Module — Infrastructure Skeleton

**Sprint:** 1 — Foundation
**Points:** 2
**Status:** TODO

## Files

- Create: `dts-stack/infra/go.mod`
- Create: `dts-stack/infra/cmd/operator/main.go`
- Create: `dts-stack/infra/cmd/cli/main.go`
- Create: `dts-stack/infra/internal/common/version.go`
- Test: `dts-stack/infra/internal/common/version_test.go`

## Step 1: Initialize Go module

```bash
cd dts-stack/infra
go mod init github.com/billyhotjava/dts/infra
```

## Step 2: Create entry points

```go
// cmd/operator/main.go
package main

import "fmt"

func main() {
    fmt.Println("dts-operator v3.0.0")
}
```

```go
// cmd/cli/main.go
package main

import "fmt"

func main() {
    fmt.Println("dts-cli v3.0.0")
}
```

```go
// internal/common/version.go
package common

var (
    Version   = "3.0.0"
    GitCommit = "unknown"
    BuildDate = "unknown"
)
```

## Step 3: Verify

Run: `cd dts-stack/infra && go build ./...`
Expected: No errors

Run: `go test ./...`
Expected: PASS

## Step 4: Commit

```bash
git add dts-stack/infra/
git commit -m "feat(s1): initialize Go infrastructure module"
```
