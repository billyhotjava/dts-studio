# Worklog

这个目录是 `/opt/prod/dts/rdc` 的统一工作记录入口。

## 组织规则
- `dts-stack/`：平台内核相关任务和文档
- `app-stack/`：行业应用包相关任务
- Sprint 命名：`sprint-<序号>-<简短目标描述>/`
- 每个 Sprint 内 `tasks/` 目录包含具体任务文件

## dts-stack v3.0 — AI Decision OS (12 Sprints, 35 Tasks)

| Sprint | Weeks | Theme | Tasks |
|--------|-------|-------|-------|
| sprint-01-foundation | 1-2 | 三语言 Monorepo + Proto + 开发环境 | 6 |
| sprint-02-core-platform | 3-4 | dts-gateway + dts-platform | 2 |
| sprint-03-data-layer | 5-6 | dts-ontology-store + dts-query-service | 2 |
| sprint-04-security-audit | 7-8 | dts-data-security + dts-audit-log + dts-workflow | 3 |
| sprint-05-governance-assets | 9-10 | dts-governance + dts-asset + dts-data-service | 3 |
| sprint-06-ai-core-1 | 11-12 | dts-intent-engine + dts-agent | 2 |
| sprint-07-ai-core-2 | 13-14 | dts-ontology-engine + dts-query-ai | 2 |
| sprint-08-ai-data | 15-16 | dts-data-connector + dts-data-quality + dts-scheduler | 3 |
| sprint-09-ai-ops | 17-18 | dts-ai-eval + dts-observability | 2 |
| sprint-10-infrastructure | 19-20 | dts-operator + dts-cli | 2 |
| sprint-11-frontend | 21-22 | dts-ui + admin + cortex + pilot | 4 |
| sprint-12-integration-release | 23-24 | E2E + Helm + 离线包 + v3.0.0 | 4 |

## 文档
- `dts-stack/docs/dts-agent-protocol.md` — DAP 协议规范
- `dts-stack/docs/knowledge-strategy.md` — 知识战略框架
- `dts-stack/docs/strategic-discussion-summary.md` — 讨论整理
- `dts-stack/docs/plans/2026-03-11-ai-decision-os-design.md` — 架构设计 (APPROVED)
- `dts-stack/docs/plans/2026-03-11-dts-v3-implementation-plan.md` — 实施计划总览

## 历史任务
- `app-stack/metro-stack/sprint-1-submodule-migration/`
- `app-stack/prs-stack/sprint-1-submodule-normalization/`
