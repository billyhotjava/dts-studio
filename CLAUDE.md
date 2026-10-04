# DTS — AI Decision Operating System

> Decision Twins System: Ontology + Intent + Agent
> 把专家脑中的隐性经验变成系统里能自动运转的能力包

## Five Iron Laws (五条铁律 — 最高优先级)

1. **人工随时可接管** — 关掉 AI 系统照样能用。AI 引擎可降级，业务 App 与数据平台独立运行。
2. **核心安全不可绕过** — 所有请求必经 dts-gateway 认证，无旁路直连，AppPack 无特权。
3. **数据安全是基因** — 所有数据出口必经受控数据边界（含 AI RAG 检索），AI 只看授权数据；实现归属以当前 ADR-009 为准。
4. **全操作可追溯** — 人和 AI 的操作 → Kafka → dts-audit-log，append-only 不可篡改。
5. **能力先于界面** — API-first，每个服务先有完整 API + 测试，再做前端。

## Architecture

- **Authority**: `dts-rdc/worklog/v1.0.0/sprint-5-202610/`, F1 and its linked ADR/task status.
- **Studio**: Java AI brain imported under `engine/engine-ai`; preserve existing packages and APIs during F1.
- **Analytics**: moved to the independent `dts-stack/analytics` project; not built by Studio.
- **Common**: pinned Pack contract library/CLI; no shared business entities or service framework.
- **Stack**: sibling lakehouse/data platform, canonical metrics and governed data execution.
- **App Stack**: sibling business applications and industry assets, consumed through AppPack contracts.
- **Console**: new UI/BFF under F6/BL-C; the legacy Copilot webapp is not imported.
- **Deployment**: K8s/offline delivery belongs to Infra F7; unusable imported legacy deployment was removed after backup.
- **Verification**: root `build.sh verify` resolves Common and tests the current AI checkout with disposable PostgreSQL; no runtime acceptance is implied.

The inherited March rules describe a historical target. Their Python/25-service
decomposition does not override the current Java migration or current Feature boundaries.
Do not introduce parallel data access or metric definitions while resolving the
existing imported code's boundary debt. No new libraries are required for F1.

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
