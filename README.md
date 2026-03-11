# DTS Studio

> **Decision Twins System** — AI 驱动的企业决策操作系统

DTS 通过 Studio 完成企业决策与运营的操作系统。Studio 是设计中心，dts-stack 是 Agent 能力中心，app-stack 是面向客户的 Agent 中心。三者通过 **DTS Agent Protocol** 交互，以 Ontology 映射客户的物理世界，以 Intent 理解决策目标，以 Agent 执行智能操作。

## Architecture

```
                         dts-studio
                      Design & Governance
                    ┌─────────────────────┐
                    │  .rules/   约束标准   │
                    │  .skills/  能力定义   │
                    │  .memory/  领域知识   │
                    │  worklog/  规划日志   │
                    └──────┬──────┬───────┘
                           │      │
            DTS Agent Protocol (DAP)
                           │      │
              ┌────────────┘      └────────────┐
              ▼                                 ▼
    ┌──────────────────┐            ┌──────────────────┐
    │    dts-stack      │            │    app-stack      │
    │  Agent 能力中心    │  ◄─ DAP ─► │  客户 Agent 中心   │
    │                   │            │                   │
    │  25 个微服务       │            │  行业 Pack         │
    │  Python/Java/Go   │            │  Ontology + Skill  │
    │  通用平台能力       │            │  行业 Know-how     │
    └──────────────────┘            └──────────────────┘
              │                                 │
              └────────────┬────────────────────┘
                           ▼
                    Customer's World
                  设备 · 产线 · 电网 · 实验室
                    Ontology 映射物理世界
```

## Three Pillars

| 仓库 | 定位 | 内容 |
|------|------|------|
| **dts-studio** (本仓库) | 设计中心 / 指挥部 | 规则、技能定义、Ontology 模式、领域知识、设计文档 |
| **dts-stack** | Agent 能力中心 | 25 个微服务（Python AI + Java 业务 + Go 基础设施 + 3 前端） |
| **app-stack** | 客户 Agent 中心 | 行业能力包（电力、科研、制造…），每个 Pack 封装领域 Ontology + AI Skill |

## DTS Agent Protocol (DAP)

三者交互的标准协议，也是产品的核心护城河。详见 [docs/dts-agent-protocol.md](docs/dts-agent-protocol.md)。

- **Ontology Protocol** — 如何定义和注册业务对象、关系、指标
- **Skill Protocol** — 如何定义、注册、调用 AI 技能
- **Intent Protocol** — 如何将自然语言转化为结构化意图
- **Security Protocol** — 如何在 Agent 间传递身份和权限
- **Audit Protocol** — 如何记录每一步 AI 决策过程

## Core Concepts

**Ontology** — 物理世界的数字映射。不是知识图谱，而是可操作的业务对象模型。每个行业的设备、工单、指标、动作都是 Ontology 的实例。

**Intent** — 决策目标的结构化表达。用户说"帮我优化产线排程"，系统理解为 `OptimizeSchedule { target: throughput, constraint: machine_capacity }`。

**Agent** — 智能执行引擎。读取 Ontology 数据，理解 Intent 目标，调用 Skill 执行多步决策链。

**Skill** — AI 能力的原子单元。专家的隐性经验通过 AI 提取，转化为可执行、可复用、可交易的能力包。

## Five Iron Laws

1. **人工随时可接管** — 关掉 AI，系统照样能用
2. **核心安全不可绕过** — 权限、审批、审计由平台统一管控
3. **数据安全是基因** — AI 不会看到超出权限的数据
4. **全操作可追溯** — 人和 AI 的操作都有完整日志，不可篡改
5. **能力先于界面** — 先做扎实的底层能力，不做跑不通的演示品

## Directory Structure

```
dts-studio/
├── CLAUDE.md            # AI 助手项目入口
├── .mcp.json            # MCP 配置 (Engram 记忆集成)
├── .rules/              # 工作守则 (22 files, 7 layers)
│   ├── 00-foundation/   #   五条铁律 + 产品理念
│   ├── 10-architecture/ #   架构 + 服务边界 + Pack 协议
│   ├── 20-development/  #   编码 + Git + API + 依赖
│   ├── 30-testing/      #   测试策略 + 质量门禁
│   ├── 40-deployment/   #   K8s 部署 + 发布流程
│   ├── 50-appstack/     #   Pack 开发 + 行业规则
│   ├── 60-skills/       #   技能设计 + 分类 + 生命周期
│   └── 90-process/      #   流程规范
├── .skills/             # 技能清单 (57 skills, 5 categories)
├── .memory/             # 知识记忆系统
│   ├── conversations/   #   Engram 对话记忆
│   ├── ontology/        #   领域知识 (类型/关系/模式/行业)
│   └── decisions/       #   设计决策日志
├── docs/                # 协议和架构文档
└── worklog/             # 工作日志
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| AI Core | Python 3.12 + LangGraph |
| Business Platform | Java 21 + Spring Boot 3 |
| Infrastructure | Go 1.22 + K8s Operator |
| Frontend | React 19 + Vite + shadcn/ui + Tailwind |
| Communication | gRPC (control) + Kafka KRaft (data) |
| Storage | PostgreSQL + ClickHouse + Neo4j + pgvector + MinIO |
| Auth | Keycloak + JWT |
| Observability | OpenTelemetry + Grafana Stack |
| Deployment | All-in-K8s (online/offline unified) |

## License

Proprietary. All rights reserved.
