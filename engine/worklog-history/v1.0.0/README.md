# v1.0.0 — dts-copilot 独立 AI 助手 + BI 分析服务

日期: 2026-03-14
状态: active-planning

## 版本目标

从 dts-stack v2.4.1 中独立抽取 AI Copilot 引擎和 BI 分析模块，构建可独立运行的增值服务组件。
目标：通过 API Key 认证，少量改动即可集成到任意业务系统（首个集成目标：馨懿诚园林管理平台）。

### 架构概览

```
业务系统（园林平台 / 其他）
    ↓ API Key + X-DTS-User-* 头
┌─ dts-copilot ─────────────────────────────────────────┐
│  dts-copilot-ai (8091)       — AI 引擎服务            │
│  dts-copilot-analytics (8092) — BI 分析服务            │
│  dts-copilot-webapp (3003)   — React 前端              │
│  Traefik (HTTPS TLS 终止 + 路由)                       │
│  PostgreSQL (copilot_ai + copilot_analytics schema)   │
│  Ollama (本地 LLM)                                     │
└───────────────────────────────────────────────────────┘
```

### 双服务职责

| 服务 | 来源 | 职责 |
|------|------|------|
| dts-copilot-ai | dts-platform AI 部分 | LLM Gateway、ReAct Agent、Tool 系统、NL2SQL、RAG、语义层、安全防护、API Key 管理 |
| dts-copilot-analytics | dts-analytics | SQL Workbench、仪表盘/报表/屏幕设计器、AI 屏幕生成、嵌入式分析、公开链接 |
| dts-copilot-webapp | dts-analytics-webapp + AI 聊天组件 | BI 界面 + Copilot 聊天面板 |

### 关键设计决策

- 认证：API Key 为主，请求头传递用户身份
- 数据库：共用 PostgreSQL，两个 schema（copilot_ai / copilot_analytics）
- LLM：OpenAI 兼容接口，默认 Ollama 本地部署
- HTTPS：Traefik TLS 终止，gen-certs.sh 自动生成自签名证书（生产换真证书）
- 部署：Docker Compose（Traefik + 后端 + 前端 + PostgreSQL + Ollama）
- 包名：com.yuzhi.dts.copilot.ai / com.yuzhi.dts.copilot.analytics

## Sprint 总览

| Sprint | 主题 | 前缀 | 任务数 | 状态 |
|--------|------|------|--------|------|
| sprint-1 | 项目脚手架与基础设施（含 HTTPS/证书） | SC | 8 | READY |
| sprint-2 | AI 引擎核心抽取 | AE | 7 | READY |
| sprint-3 | AI 高级能力抽取（RAG/Agent/Tool） | AA | 7 | READY |
| sprint-4 | API Key 认证与安全体系 | AK | 5 | READY |
| sprint-5 | BI 分析引擎抽取 | BA | 7 | READY |
| sprint-6 | 前端 Webapp 抽取与整合 | FE | 6 | READY |
| sprint-7 | 园林平台集成与端到端验证 | IN | 5 | READY |
| sprint-8 | NL2SQL 聊天到可视化闭环 | NV | 8 | READY |
| sprint-9 | Copilot 系统配置中心 | CS | 9 | IN_PROGRESS |

## 依赖关系

```
sprint-1 (脚手架)
    ↓
sprint-2 (AI 核心) ──→ sprint-3 (AI 高级)
    ↓                        ↓
sprint-4 (认证安全) ←────────┘
    ↓
sprint-5 (BI 引擎) ──→ sprint-6 (前端)
                            ↓
                      sprint-7 (集成验证)
                            ↓
                      sprint-8 (NL2SQL 闭环)
                            ↓
                      sprint-9 (系统配置中心)
```

## 设计文档

- 架构设计: 待生成（docs/superpowers/specs/）
- 源代码抽取映射: 见各 sprint task 中的影响文件

## 来源代码映射

| dts-copilot 模块 | 来源模块 | 关键改造 |
|------------------|---------|---------|
| copilot-ai: LLM Gateway | dts-platform/service/ai/gateway/ | 独立配置管理，去掉对 dts-ingestion 的依赖 |
| copilot-ai: ReAct Engine | dts-platform/service/ai/engine/ | 保持不变 |
| copilot-ai: Tool System | dts-platform/service/ai/tool/ | 裁剪数据治理专用 Tool，预留园林业务扩展点 |
| copilot-ai: NL2SQL | dts-platform/service/ai/ | 语义增强保留，去掉治理专用上下文 |
| copilot-ai: RAG | dts-platform/service/ai/rag/ | 完整保留 pgvector + 混合检索 |
| copilot-ai: Safety | dts-platform/service/ai/safety/ | 完整保留 |
| copilot-ai: 认证 | 新增 | API Key 管理 + 用户身份传递 |
| copilot-ai: 数据源管理 | dts-platform/service/infra/ | 精简抽取 InfraDataSource |
| copilot-analytics | dts-analytics 整体 | 将对 dts-platform 的调用改为对 copilot-ai |
| copilot-webapp | dts-analytics-webapp + dts-platform-webapp AI组件 | 合并 AI 聊天面板 |
