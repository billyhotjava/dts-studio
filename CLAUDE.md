# DTS — AI Decision Operating System

> Decision Twins System: Ontology + Intent + Agent
> 把专家脑中的隐性经验变成系统里能自动运转的能力包

## Five Iron Laws (五条铁律 — 最高优先级)

1. **人工随时可接管** — 关掉 AI 系统照样能用。Python 服务全部可降级，Java 层独立运行。
2. **核心安全不可绕过** — 所有请求必经 dts-gateway 认证，无旁路直连，AppPack 无特权。
3. **数据安全是基因** — 所有数据出口必经 dts-data-security（含 AI RAG 检索），AI 只看授权数据。
4. **全操作可追溯** — 人和 AI 的操作 → Kafka → dts-audit-log，append-only 不可篡改。
5. **能力先于界面** — API-first，每个服务先有完整 API + 测试，再做前端。

## Architecture

- **Python** (AI Core, 10 services): dts-agent, dts-intent-engine, dts-ontology-engine, dts-data-connector, dts-data-quality, dts-query-ai, dts-ai-eval, dts-observability, dts-scheduler, dts-perception(P2)
- **Java** (Business Platform, 10 services): dts-platform, dts-gateway, dts-ontology-store, dts-query-service, dts-governance, dts-asset, dts-data-security, dts-workflow, dts-audit-log, dts-data-service
- **Go** (Infrastructure, 2 services): dts-operator, dts-cli
- **Frontend** (3 apps): dts-admin-webapp, dts-cortex-webapp, dts-pilot-webapp
- **Communication**: gRPC (control plane) + Kafka KRaft (data plane)
- **Storage**: PostgreSQL + ClickHouse + Neo4j CE + pgvector + MinIO
- **Auth**: Keycloak + JWT propagation
- **Observability**: OpenTelemetry + Grafana (Tempo/Prometheus/Loki)
- **Deployment**: All-in-K8s, online/offline unified architecture

## Rules & Skills

详细规则和技能定义在以下目录中，开发前必须阅读对应层级的规则：

### .rules/ — 工作守则
- `00-foundation/` — 五条铁律 + 产品理念 (**HIGHEST priority**)
- `10-architecture/` — 架构原则 + 服务边界 + AppPack 协议 + **Infra 铁律应用**
- `20-development/` — 编码规范 + Git 工作流 + API 设计 + 依赖策略
- `30-testing/` — 测试策略 + 质量门禁 (PR/Nightly/Release)
- `40-deployment/` — K8s 部署运维 + 发布流程
- `50-appstack/` — Pack 开发指南 + 行业规则 (energy/research/manufacturing)
- `60-skills/` — AI 技能设计规范 + 分类体系 + 生命周期
- `90-process/` — worklog 组织规范

### .skills/ — 技能清单 (57 skills, placeholder)
- `00-platform/` — 17 个平台内置技能 (data/ontology/query/governance/report/action)
- `10-data/` — 6 个数据层技能 (connector/quality)
- `20-ai/` — 9 个 AI 核心技能 (agent/eval)
- `30-industry/` — 18 个行业技能 (energy/research/manufacturing)
- `40-devops/` — 7 个运维技能

## Project Structure

```
/opt/prod/dts/dts-rdc/dts-studio/    # dts-rdc 的 submodule
├── .rules/              # 工作守则 (23 rule files)
├── .skills/             # 技能清单 (15 skill files, 57 skills)
├── .memory/             # 领域知识 (ontology/conversations/decisions)
├── CLAUDE.md            # 本文件 — dts-studio 入口
└── .mcp.json            # MCP server config (Engram)
```

## Key References

- Parent repo: `/opt/prod/dts/dts-rdc/` (RDC — Research Development Center)
- Architecture design: `/opt/prod/dts/dts-rdc/worklog/v1.0.0/docs/plans/2026-03-11-ai-decision-os-design.md`
- Infra design: `/opt/prod/dts/dts-rdc/worklog/v1.0.0/docs/plans/2026-03-26-dts-infra-design.md`
- Product docs: `~/Documents/dts/` (商业计划书, 产品介绍, Palantir 分析)
- Memory: `~/.claude/projects/-opt-prod-dts-dts-rdc/memory/`

## Working Language

- 与用户交流使用中文
- 代码注释和文档使用英文
- 规则文件使用英文（技术标准化）
