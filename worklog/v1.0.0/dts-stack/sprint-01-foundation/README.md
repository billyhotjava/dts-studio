# Sprint 1: Foundation & Skeleton

**Weeks:** 1-2
**Theme:** Monorepo setup, proto definitions, shared libs, dev environment
**Story Points:** 21

## Goals
- Initialize three-language monorepo (Java / Python / Go)
- Define DAP v1 gRPC proto contracts
- Set up dev environment (Docker Compose middleware)
- Configure CI pipeline

## Tasks

| # | Task | Points | Status |
|---|------|--------|--------|
| 1.1 | Java monorepo — Parent POM & dts-common | 5 | DONE |
| 1.2 | Proto definitions — DAP gRPC contracts | 5 | DONE |
| 1.3 | Python monorepo — workspace & dts-common-py | 3 | DONE |
| 1.4 | Go module — infrastructure skeleton | 2 | DONE |
| 1.5 | Dev environment — Docker Compose middleware | 3 | DONE |
| 1.6 | CI pipeline — GitHub Actions | 3 | DONE |

## Dependencies
- None (first sprint)

## Deliverables
- [x] `dts-stack/source/pom.xml` + `dts-common` module compiles
- [x] `dts-stack/proto/dap/v1/*.proto` — 7 proto files created
- [x] `dts-stack/ai/` workspace with `uv sync` working
- [x] `dts-stack/infra/` with `go build ./...` passing
- [ ] `docker compose up -d` starts all middleware
- [ ] CI green on GitHub Actions
