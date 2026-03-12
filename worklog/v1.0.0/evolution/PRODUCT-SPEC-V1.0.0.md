# DTS v3.0 产品功能说明书 — AI Decision Operating System

> **版本**: v3.0.0
> **日期**: 2026-03-12
> **状态**: DRAFT
> **读者**: 产品经理、技术决策者、架构师

---

## 1. 产品概述

### 1.1 产品定位

DTS (Decision Twins System) 是一套 **AI 驱动的企业决策操作系统**。它不是传统意义上的数据中台（不卖存储/计算）、不是 BI 工具（不只看数据）、不是 SaaS（不按人头卖工具），而是将企业数据资产、业务规则、行业隐性知识编织成一个能替决策者"想"的系统。

用户不再需要问"有多少数据资产"，而是说"统计半年华东区销售情况，给报表"，系统端到端完成：理解意图 -> 查数据 -> 分析 -> 图表 -> 报告 -> 建议 -> 执行。

**核心价值主张**: 把专家脑中的隐性经验变成系统里能自动运转的能力包。

```
1 位行业专家的创造力
  x AI 提取能力（把隐性知识变成显性规则）
  x 系统化规模（内建到平台，全员可用）
  = 500 倍产出放大
```

### 1.2 核心公式

**Ontology (数据基座) + Intent (理解) + Agent (执行)**

- **Ontology**: 物理世界到数字世界的映射标准，不只是数据字典，而是包含实体、关系、指标、规则、操作的完整认知框架
- **Intent**: 自然语言到结构化决策意图的转化层，支持多目标分解、约束识别、风险评估
- **Agent**: 基于 LangGraph 的多步推理运行时，能自主规划执行路径、调用技能、生成决策解释

三者通过 DAP (DTS Agent Protocol) 协议连接，形成完整的决策闭环。

### 1.3 目标用户角色

| 角色 | 典型岗位 | 核心需求 | 使用端 |
|------|---------|---------|--------|
| **决策者** | 高管、部门负责人 | 用自然语言获取分析结果和行动建议 | Pilot |
| **行业专家** | 数据分析师、工艺工程师、科研人员 | 建模、探索数据、编排技能、管理知识 | Cortex |
| **IT 管理员** | 系统管理员、运维工程师 | 管理用户权限、配置数据源、监控系统 | Admin |
| **开发者** | ISV 开发者、集成商 | 开发 AppPack、调用 API、扩展平台能力 | ADK + API |

### 1.4 产品形态

- **三个前端应用**: Pilot (决策门户) + Cortex (专家工作台) + Admin (管理控制台)
- **开放 API**: REST API (外部) + gRPC API (内部服务间) + SSE (流式推送)
- **AppPack 生态**: 标准化行业能力包，包含 Ontology + Skill + KnowHow + 配置 + UI 的全栈插件

---

## 2. 产品架构

### 2.1 三支柱架构

```
              DTS AI Decision Operating System
    ┌─────────────────────────────────────────────────────┐
    │                                                     │
    │   dts-studio          dts-stack          app-stack  │
    │  (设计中心)          (Agent能力中心)      (客户Agent中心) │
    │                                                     │
    │  - Ontology 设计     - Python AI Core    - 电力 Pack │
    │  - Skill 编排        - Java Business     - 科研 Pack │
    │  - KnowHow 管理      - Go Infrastructure - 制造 Pack │
    │  - Pack 开发 IDE     - 3 Frontend Apps   - 定制 Pack │
    │                                                     │
    └─────────────────────────────────────────────────────┘
```

**dts-studio**: 面向行业专家和开发者的设计中心，提供 Ontology 可视化设计、Skill 编排、KnowHow 管理和 AppPack 开发工具。

**dts-stack**: 平台能力内核，由 Python (10 个 AI 服务) + Java (10 个业务服务) + Go (2 个基础设施服务) + 3 个前端应用组成。Python 层全部可降级，Java 层独立运行。

**app-stack**: 行业应用包集合，每个 Pack 包含该行业的 Ontology 定义、AI 技能、决策规则、UI 组件和数据迁移脚本。

### 2.2 DAP 协议五层概述

DAP (DTS Agent Protocol) 是产品的核心护城河，定义了 AI Agent 如何理解物理世界并执行决策：

```
Layer 5: Audit Protocol        — 决策可追溯性保障 (CloudEvents, append-only)
Layer 4: Security Protocol     — 身份与权限在 Agent 间传递 (JWT + 零信任)
Layer 3: Intent Protocol       — 自然语言 -> 结构化意图 -> 执行计划
Layer 2: Skill Protocol        — AI 技能的定义、注册、调用、编排 (gRPC streaming)
Layer 1: Ontology Protocol     — 物理世界 -> 数字映射的标准格式 (YAML 声明式)
Layer 0: Transport             — gRPC (control) + Kafka (data) + SSE (streaming)
```

每个行业 Pack 遵循 DAP 接入，形成网络效应。客户的 Ontology 和 Skill 资产建立在 DAP 之上，迁移成本等于丢失核心竞争力。DAP 协议版本向后兼容，当前为 dap/v1，预留 v2 (多模态) 和 v3 (跨组织 Agent 协作)。

### 2.3 核心数据流

```
用户输入 (自然语言)
  |
  v
dts-intent-engine: NL -> 结构化 Intent (goals + constraints + risk_level)
  |
  v
dts-agent: Intent -> Plan (多步骤执行 DAG, LangGraph 状态机)
  |
  v
逐步执行 Skills (gRPC streaming)
  |-- 低风险: 自动执行, SSE 推送进度
  |-- 中/高风险: 暂停 -> dts-workflow HITL 审批
  |       |-- 审批通过: 继续执行
  |       |-- 审批拒绝: 终止并记录
  |
  v
结果输出 (富卡片/报告/图表/Action) -> SSE 流式返回前端
  |
  v
全链路审计 -> Kafka -> dts-audit-log (append-only, 不可篡改)
                    -> dts-observability (AI Trace 树, 成本统计)
```

### 2.4 五条铁律在产品中的体现

| 铁律 | 产品功能体现 |
|------|------------|
| **人工随时可接管** | 每个 AI 能力都有等价的人工操作路径。AI 不可用时，用户通过 Cortex 专家模式菜单手动完成所有操作，平台 100% 可用 |
| **核心安全不可绕过** | 所有请求必经 dts-gateway 认证，AppPack 无特权，Skill 调用携带 RequestContext 传递身份和权限 |
| **数据安全是基因** | 每个 ObjectType 属性标记 classification，AI 读取时自动过 dts-data-security，SQL 自动注入行级过滤，RAG 检索安全过滤 |
| **全操作可追溯** | 人和 AI 走同一个 Capability API，审计天然统一。每个 SkillResponse 携带审计条目，Kafka append-only 存储 |
| **能力先于界面** | 每个服务先有完整 REST/gRPC API + 测试，AI Tool 是 API 的适配器，UI 是加速层不是替代层 |

---

## 3. 三个前端应用

技术栈: React 19 + Vite + shadcn/ui + Tailwind CSS。视觉风格: 亮色优先，大圆角卡片 (rounded-2xl ~ rounded-3xl)，Vercel x Grafana x Linear 混合风格。

### 3.1 Pilot — 决策门户

**定位**: 面向管理者和业务人员的自然语言交互式决策入口。

**用户价值**: 让非技术背景的决策者能用一句话完成从数据查询到行动决策的全链路，无需理解 SQL、图表工具或业务系统的操作方式。

**关键特性**:

**自然语言交互界面**
- AI 对话主界面，支持文本输入和语音输入（dts-perception 预留）
- CopilotSidebar 常驻，上下文感知当前页面状态
- 流式响应 (SSE)，实时展示 Agent 多步推理过程
- 对话内嵌富卡片: 图表、表格、结论、操作按钮一体化呈现
- 追问式交互: "按区域拆开"、"改成柱状图"等增量修改无需重新描述

**决策仪表盘**
- 混合首页: 上方 KPI 概览卡片（基于用户角色和数据权限个性化）、中间快捷操作 + 最近访问
- AI 生成的图表可 Pin-to-Canvas，对话与可视化分离
- 支持"保存为报告"（持久化报告页，可分享，可导出 PDF）和"生成看板"
- 角色自适应菜单: 普通用户看到首页/看板/报告中心，分析师增加 Query/Explore/数据目录

**审批工作流 (HITL)**
- 高风险操作推送审批通知，决策者在 Pilot 端直接审批/拒绝
- 审批详情展示 AI 推理链路: 为什么建议这个操作、依据了哪些数据、风险评估结果
- 多级审批流支持，审批历史可查询
- 超时自动处理策略（可配置: 自动拒绝/升级/提醒）

**通知与推送**
- 定时报告推送（日报/周报/月报，由 dts-scheduler 触发）
- 异常告警推送（数据质量异常、指标超阈值、SLA 违规）
- 多渠道投递: 应用内通知 + 邮件 + Webhook

**移动端适配**
- 响应式布局，核心对话和审批功能在移动端完整可用
- 推送通知适配移动设备

### 3.2 Cortex — 专家工作台

**定位**: 面向行业专家（数据分析师、工艺工程师、科研人员）的深度工作环境。

**用户价值**: 为技术型用户提供从数据探索、Ontology 建模、技能编排到知识管理的全套工具，将个人的行业经验转化为可复用的组织资产。

**关键特性**:

**Ontology 可视化浏览和编辑**
- React Flow 驱动的图可视化，展示 ObjectType、Relationship、Metric 的完整拓扑
- 拖拽式 ObjectType 定义: 属性、类型、分类等级、数据来源、更新频率
- Relationship 管理: 可视化创建/编辑实体间关系，支持 many_to_one/many_to_many 等基数
- Metric 定义器: 公式编辑、阈值配置、依赖关系声明
- Action 定义: 风险等级标记，high risk 自动关联 HITL 审批流
- 语义搜索: 自然语言查找 Ontology 中的类型和实例（pgvector 语义匹配）

**KnowHow 管理**
- KnowHow 浏览: 按知识金字塔层级 (L0~L4) 分类展示
- KnowHow 编辑: 专家校正 AI 生成的草稿，标记置信度，关联到 ObjectType
- KnowHow 测试: 将 KnowHow 转化为 Skill 后在沙箱环境试运行
- KnowHow 发布: 经评测套件验证达到 baseline_accuracy 后发布为 active 状态
- 知识碎片管理: 查看/组织 Gen-Ba 碎片，LLM 辅助聚类和去矛盾

**技能编排工作台**
- 可视化 Skill 编排: DAG 式拖拽编排多个 Skill 的执行依赖
- Skill 测试: 输入参数 -> 执行 -> 查看输出 + 审计记录
- Persona 管理: 配置行业人格（Prompt + 关联 Skill 集 + 定时任务）
- 评测管理: 创建评测套件、运行版本对比、分析 Bad Case

**数据探索与分析**
- SQL 编辑器: 语法高亮、自动补全、执行计划分析
- 数据 Profiling: 统计分布、空值率、唯一值、异常值发现
- 数据质量看板: 规则执行结果、漂移检测趋势、修复建议
- Schema 浏览器: 多数据源 schema 树形浏览，Ontology 映射可视化

**AI 对话式查询**
- CopilotSidebar 统一架构，上下文注入当前编辑器状态（datasource/schema/currentSql）
- NL2SQL: 自然语言描述 -> 自动生成 SQL -> 执行 -> 结果解释
- 查询建议: 根据当前数据上下文智能推荐分析角度
- 图表自动生成: 数据 + 意图 -> ECharts 配置自动生成

### 3.3 Admin — 管理控制台

**定位**: 面向 IT 管理员和系统管理员的平台运维和配置中心。

**用户价值**: 提供一站式的系统管理能力，涵盖用户权限、数据源、服务监控、AppPack 生命周期和审计合规的全面管控。

**关键特性**:

**用户/角色/权限管理**
- 用户管理: CRUD + Keycloak 同步 (OIDC/SAML/PKI)
- 角色管理: RBAC 角色定义 + 权限分配，支持功能权限与数据权限分离
- 组织架构: 多租户管理，租户间数据完全隔离
- 菜单管理: 角色自适应菜单配置，权限高不等于看到所有功能

**数据源配置与连接**
- 数据源注册: 支持 JDBC (PostgreSQL/MySQL/Oracle/SQL Server/ClickHouse/Hive)、File (CSV/Excel/JSON/Parquet)、API (REST)、Industrial (OPC-UA/MQTT)
- 连接测试: 一键验证连通性
- 元数据发现: 自动扫描表/列结构
- CDC 配置: 通过 Kafka 实现变更数据捕获

**服务监控 (OpenTelemetry)**
- 服务健康看板: 所有 25 个服务的 Pod 状态、就绪探针、资源使用
- 分布式追踪: Grafana Tempo 集成，traceId 关联基础设施 Trace + AI 执行 Trace
- 指标监控: Prometheus 指标，请求延迟/吞吐/错误率
- 日志查看: Grafana Loki 集成，多维度日志检索

**AppPack 生命周期管理**
- Pack 市场 (Marketplace): 应用市场风格的 Pack 浏览、搜索、安装界面
- 安装/升级/回滚: 管理员审批 -> 内核执行 migrations -> 加载到运行时
- 版本管理: Pack 版本历史、兼容性矩阵、依赖关系图
- 运行状态: Pack 资源用量、调用次数、错误统计

**审计日志查看**
- 全操作日志浏览: 人和 AI 的操作按时间线展示
- 多维度筛选: 按用户/操作类型/数据源/风险等级/时间范围过滤
- 决策链路回溯: 从审计事件关联到完整的 Intent -> Plan -> Skill 执行链
- 合规报告导出: 按时间段生成审计报告

**系统配置**
- 全局参数配置: LLM 端点、Token 限额、超时策略
- 安全策略: 数据分级标准、脱敏规则、水印配置
- 调度管理: 定时任务查看/暂停/重启
- 备份管理: 数据库备份策略配置，备份到 MinIO

---

## 4. 核心功能模块

### 4.1 Ontology 管理

**描述**: Ontology 是 DTS 理解客户物理世界的认知框架。不同于传统数据字典仅记录表结构，DTS 的 Ontology 是一个包含业务对象、关系、指标、规则和可执行操作的完整数字孪生模型。所有 Ontology 资源遵循 DAP v1 协议，采用 YAML 声明式定义。

**用户价值**: 让 AI Agent 真正"理解"企业业务，而非简单的数据库查询。Ontology 是 Agent 规划执行路径、选择合适 Skill、生成决策解释的认知基础。

**关键特性**:

| 特性 | 描述 | 技术实现 |
|------|------|---------|
| ObjectType 定义与管理 | 声明式定义业务对象类型，包含属性、类型、分级、来源标记、更新频率 | dts-ontology-store (Java) CRUD + dts-ontology-engine (Python) 语义推理 |
| Relationship 管理 | 图可视化展示实体间关系（belongs_to/inspects/targets 等），支持基数声明 | Neo4j CE 存储图关系，React Flow 前端渲染 |
| Metric 定义与计算 | 公式编辑器定义复合指标（如 OEE = availability x performance x quality），配置阈值告警 | dts-ontology-store 存储定义，dts-query-service 执行计算 |
| Rule 管理 | 业务规则引擎，声明式定义条件-动作规则，支持 risk_level 分级 | dts-ontology-store 存储，dts-agent 执行时自动校验 |
| Action 定义 | 定义可执行操作（start/stop/schedule_maintenance），标记风险等级，high risk 自动触发 HITL | dts-ontology-store 存储，dts-workflow 审批集成 |
| 语义搜索 | 自然语言查找 Ontology 中的类型和实例，"找所有温度异常的设备" | pgvector 向量索引 + Ontology 元数据过滤 |
| 数据血缘追踪 | 查询字段级数据血缘，从报表追溯到源表 | Neo4j 图遍历，结果按用户权限过滤 |
| 行业命名空间 | manufacturing/energy/research 等命名空间隔离，防止跨 Pack 名称冲突 | DAP v1 metadata.namespace 字段 |

**DAP 设计原则**:
- 声明式而非命令式: 定义"是什么"，Agent 自主决定"怎么做"
- 数据安全内置: 每个属性标记 classification，Agent 读取时自动过 dts-data-security
- 来源标记: 标记 source 和 frequency，Agent 知道去哪拿数据（SCADA 实时 vs ERP 批量）

### 4.2 Intent 引擎

**描述**: Intent 引擎是自然语言到结构化决策意图的转化层，是 DAP Layer 3 的核心实现。它将用户的一句话分解为可执行的多目标意图结构，包含查询目标、条件动作、约束条件和风险评估。

**用户价值**: 用户无需学习查询语言或理解系统结构，一句话即可表达复杂的多步决策需求。例如"帮我看看A3产线今天的OEE，如果低于80%就安排维保"被自动分解为查询 + 条件判断 + 动作触发三个步骤。

**关键特性**:

| 特性 | 描述 |
|------|------|
| NL -> 结构化 Intent | 将自然语言解析为 DAP Intent 结构: goals (目标数组) + constraints (约束) + plan (执行计划) |
| 多目标意图分解 | 一句话中识别多个目标（query + conditional_action），自动建立依赖关系 |
| 约束条件识别 | 从用户输入中提取约束: max_steps、timeout、require_approval_above 等 |
| 风险等级评估 | 每个步骤自动评估风险 (low/medium/high)，medium 以上自动触发 HITL 审批 |
| Intent 路由 | 根据 Intent 目标自动选择合适的 Skill 组合，通过 Describe() 接口获取可用 Skill 元数据 |
| 上下文保持 | 多轮对话中保持 Intent 上下文，支持追问式增量修改 |

**Intent 生命周期**: 用户输入 -> 解析为 Intent -> 生成 Plan (DAG) -> 逐步执行 Skills -> 遇到 high risk 暂停等待 HITL -> 结果流式返回 -> 全链路审计。

### 4.3 Agent 运行时

**描述**: Agent 运行时是 DTS 的 AI 执行核心，基于 LangGraph 状态机实现多步推理和任务编排。它负责将 Intent 引擎输出的结构化意图转化为可执行的 DAG 计划，逐步调用 Skill，处理条件分支和错误恢复，最终组装输出结果。

**用户价值**: 将"说一句话到拿到结果"之间的所有中间步骤自动化，同时保持每一步的透明性和可控性。用户可以随时查看执行进度、理解 AI 的推理逻辑、在任意步骤接管控制权。

**关键特性**:

| 特性 | 描述 |
|------|------|
| 多步骤推理 (LangGraph) | 状态机驱动的 DAG 执行引擎。无依赖的步骤可并行，每步结果存入 TaskContext |
| Persona 管理 | 行业人格配置: system prompt + 关联 Skill 集 + few-shot 示例 + 定时任务。切换 Persona 改变 Agent 的行业上下文 |
| 执行计划生成 | LLM 分析用户意图生成 TaskPlan (DAG): 每步包含 stepId、description、toolId、dependsOn、riskLevel |
| 工具/技能调用编排 | 通过标准 gRPC SkillService 接口调用所有 Skill (内置 + AppPack)，RequestContext 传递身份和权限 |
| 决策解释生成 | 读取 dts-observability 的 AI Trace 树，生成人类可读的推理链路解释 |
| 人工接管机制 | 任意执行步骤可暂停，用户手动在 Cortex 专家模式完成该步骤，结果写回 TaskContext，后续步骤继续 |
| 错误恢复策略 | 步骤失败时 LLM 决定: 重试/跳过/降级/终止。降级策略确保 AI 不可用时不阻塞业务 |
| 条件执行 | 支持 if-then 逻辑，如"OEE < 80% 则安排维保"，一句话完成多步决策链 |

### 4.4 数据连接与质量

**描述**: 数据连接层 (dts-data-connector) 负责多源数据接入，数据质量层 (dts-data-quality) 负责 AI 驱动的质量检测。两者协同实现"数据进来就是干净的，进来之后持续监控"。

**用户价值**: 不搬数据、就地使用。JDBC 直连客户现有数据库，SQL 下推执行，数据不离开原位。同时 AI 自动发现数据质量问题，提供修复建议。

**关键特性**:

| 特性 | 描述 |
|------|------|
| 多源数据接入 | JDBC: PostgreSQL/MySQL/Oracle/SQL Server/ClickHouse/Hive; File: CSV/Excel/JSON/Parquet (MinIO); API: REST; Industrial: OPC-UA (MES/SCADA), MQTT (IoT) |
| 元数据自动发现 | 连接数据源后自动扫描表/列结构，AI 建议 ObjectType 映射 |
| 连接测试 | 一键验证数据源连通性 |
| CDC 变更数据捕获 | 通过 Kafka 实现实时变更同步，支持增量数据接入 |
| Schema 映射 | AI 建议源表字段到 Ontology ObjectType 属性的映射关系 |
| 数据质量检测 | SQL 规则引擎执行质量检查 (完整性/一致性/范围)，报告 pass/fail |
| 异常发现 | 统计分布对比检测数据漂移 + LLM 解释异常原因 |
| 修复建议 | 给定质量问题，LLM 提出纠正 SQL 或处理建议 |
| 自动清洗 | 规则化去重、格式标准化，歧义场景由 LLM 确认 |
| 数据 Profiling | 生成统计摘要: 分布、空值率、唯一值、离群值，LLM 解释异常模式 |

### 4.5 查询与分析

**描述**: 智能查询层将自然语言转化为 SQL 执行，并将查询结果转化为可理解的分析和可视化。由 dts-query-ai (Python, NL2SQL) 和 dts-query-service (Java, 执行引擎) 协同实现。

**用户价值**: 业务人员无需写 SQL，用自然语言描述分析需求即可获得结果。系统不仅返回数据，还自动解释数据模式、发现异常趋势，并推荐合适的可视化图表。

**关键特性**:

| 特性 | 描述 |
|------|------|
| NL2SQL | 自然语言转 SQL，以 Ontology 元数据为上下文准确解析表/列名 |
| SQL 安全沙箱 | 生成的 SQL 在执行前经过安全校验，禁止 DELETE/UPDATE/DROP 等危险操作 |
| 多数据源路由查询 | 根据查询目标自动路由到对应数据源 (PostgreSQL/ClickHouse/Hive)，SQL 下推执行 |
| 结果解释 | LLM 总结数据模式、离群值、趋势变化，生成自然语言摘要 |
| 图表自动生成 | 规则化图表类型选择 + LLM 生成标题/注释 -> ECharts 配置输出 |
| 图表类型建议 | 根据数据维度和分析意图推荐最合适的可视化类型 |
| 查询建议与优化 | 根据当前数据上下文推荐分析角度，SQL 执行计划分析和优化建议 |

### 4.6 数据治理

**描述**: 数据治理模块 (dts-governance) 提供元数据管理、数据分级分类、数据血缘和数据标准管理能力，确保企业数据资产的可发现、可理解、可信任。

**用户价值**: 让数据资产从"无人管理的仓库"变成"有目录、有标准、有血缘的资产中心"，支撑数据安全策略的自动执行和合规审计。

**关键特性**:

| 特性 | 描述 |
|------|------|
| 元数据管理 | 技术元数据 (schema/table/column) + 业务元数据 (描述/owner/分类) 统一管理 |
| 数据分级分类 | AI 辅助自动分级 (public/internal/confidential/top-secret)，分析列名 + 样本值推断敏感级别 |
| 数据血缘 | Neo4j 图查询，字段级血缘追踪，从报表到源表的完整链路 |
| 数据标准管理 | 定义企业数据标准（命名规范、取值范围、格式要求），自动校验合规性 |
| 数据资产目录 | 可搜索的数据资产目录，支持自然语言检索 |
| 异常检测 | 规则引擎统计检查 + LLM 解释异常，自动触发告警 |
| 质量规则建议 | 分析数据分布模式，AI 建议适用的完整性/一致性/范围规则 |

### 4.7 数据安全

**描述**: 数据安全模块 (dts-data-security) 是铁律三"数据安全是基因"的核心实现。所有数据出口（包括 AI RAG 检索）必经此模块，实现零信任的数据访问控制。

**用户价值**: 确保 AI Agent 只能看到当前用户有权看到的数据。数据安全不是事后审计，而是实时拦截。即使 AppPack 的自定义 Skill，也遵循同等安全标准。

**关键特性**:

| 特性 | 描述 |
|------|------|
| 动态脱敏 | 按用户权限对敏感字段实时脱敏（手机号/身份证/金额），支持多种脱敏策略 |
| 行列级权限 | 行级过滤 (SecuritySqlRewriter 自动注入 WHERE 条件) + 列级控制 (allow/mask/deny per column) |
| 数据水印 | 报告/导出 PDF 自动嵌入数据密级水印和用户标识 |
| RAG 检索安全过滤 | AI Agent 的 RAG 检索结果按用户数据权限过滤，防止越权获取信息 |
| 零信任验证 | 每次数据访问都通过 DataSecurityCheck 验证，不缓存权限决策 |
| AppPack 同等安全 | Pack 的 Skill 通过 DataAccessGate 访问数据，不能直接连库，看到的数据等于当前用户有权看到的数据 |

### 4.8 审批与工作流

**描述**: 审批引擎 (dts-workflow) 是 HITL (Human-In-The-Loop) 机制的核心，确保高风险的 AI 决策必须经过人工确认。同时支持传统的业务审批流（数据访问申请、变更审批等）。

**用户价值**: 在享受 AI 自动化效率的同时，对高风险操作保持完全的人工控制。审批详情展示完整的 AI 推理链路，帮助审批者做出知情决策。

**关键特性**:

| 特性 | 描述 |
|------|------|
| HITL 审批 | AI 执行遇到 high/medium risk 步骤自动暂停，创建审批工单，等待人工确认 |
| 多级审批流 | 支持按风险等级配置审批链: medium -> 直接上级, high -> 部门负责人 + 安全审核 |
| 审批详情 | 展示完整 AI 推理过程: 为什么建议这个操作、依据了哪些数据、风险评估结果 |
| 超时自动处理 | 可配置超时策略: 自动拒绝/自动升级/发送提醒 |
| 审批历史查询 | 完整的审批记录，关联到 Intent/Plan/Skill 执行链路 |
| 传统审批 | 数据访问权限申请、AppPack 安装审批、服务扩缩容审批等 |

### 4.9 审计追踪

**描述**: 审计日志模块 (dts-audit-log) 实现铁律四"全操作可追溯"。基于 CloudEvents 规范，通过 Kafka 异步收集所有操作事件，append-only 存储，不可篡改。

**用户价值**: 完整记录企业中人和 AI 的每一个操作，支持合规审计和决策链路回溯。当需要理解"AI 为什么做了这个决策"时，可以追溯完整的推理链。

**关键特性**:

| 特性 | 描述 |
|------|------|
| 全操作日志 | 人 (HUMAN) 和 AI (AI_AGENT) 和定时任务 (SCHEDULED) 的操作统一记录 |
| Append-only 不可篡改 | 审计日志写入后不可删除、不可修改（哈希链），涉密环境加密存储 |
| 决策链路回溯 | 从最终结果反向追溯: 哪个 Intent -> 什么 Plan -> 执行了哪些 Skill -> 访问了什么数据 -> 输出了什么结果 |
| AI 透明 | 记录 Agent 看了什么数据、做了什么推理、为什么选这个 Skill、Token 消耗量 |
| 合规报告生成 | 按时间段/用户/操作类型生成审计报告，支持导出 |
| 成本追踪 | 每次 LLM 调用的 token 成本记录，支持用量计费和成本优化分析 |

**审计事件类型**: dap.intent.parsed / dap.plan.generated / dap.skill.started / dap.skill.executed / dap.skill.failed / dap.approval.requested / dap.approval.granted / dap.approval.rejected / dap.data.accessed / dap.session.completed

### 4.10 调度与自动化

**描述**: 调度引擎 (dts-scheduler) 提供定时任务 DAG 编排能力，支持 Skill 的定时执行、报告自动生成推送、SLA 监控和失败重试。

**用户价值**: 将重复性的分析和报告工作完全自动化。专家配置一次，系统按计划持续执行，异常时自动告警，失败时自动重试或补偿。

**关键特性**:

| 特性 | 描述 |
|------|------|
| 定时任务 DAG 编排 | 可视化配置任务依赖关系，支持串行/并行/条件分支 |
| Scheduled Skill 执行 | 任何 Skill 可配置定时触发（cron 表达式），如每日 OEE 计算、每周质量报告 |
| 定时报告推送 | 注册 cron 任务定期执行 report/generate，结果推送到指定用户/渠道 |
| SLA 监控 | 定义关键任务的 SLA (最大延迟/完成时间)，违规时自动告警 |
| 失败重试与补偿 | 可配置重试策略 (次数/间隔/退避)，支持补偿任务和 backfill |
| 任务监控 | 任务运行历史、成功/失败统计、耗时趋势、资源消耗 |

---

## 5. AI 技能体系（57 个技能）

DTS 的 AI 能力通过标准化的 Skill 体系提供。每个 Skill 遵循 DAP Layer 2 (Skill Protocol)，通过 gRPC SkillService 接口统一调用，具备描述发现 (Describe)、预校验 (Validate)、流式执行 (Execute) 三个标准方法。

Skill 类型分为四种:
- **Rule**: 纯规则驱动，确定性执行
- **LLM**: 需要大语言模型推理
- **Hybrid**: 规则 + LLM 混合
- **Workflow**: 涉及人工审批流

### 5.1 平台内置技能（17 个）

平台内置技能随 dts-stack 交付，是所有行业场景的基础能力。

**数据操作类（6 个）**

| 技能 | 类型 | 描述 | 优先级 |
|------|------|------|--------|
| data/schema-lookup | Rule | 从 Ontology 查询表/列元数据，Agent 用于理解数据结构 | P0 |
| data/execute-query | Rule | 经 dts-data-security 权限过滤后执行 SQL 查询，禁止 DELETE/UPDATE/DROP | P0 |
| data/preview-data | Rule | 预览数据源的前 N 行，快速了解数据内容 | P0 |
| data/profile-data | Hybrid | 生成数据统计摘要 (分布/空值/离群)，LLM 解释异常模式 | P1 |
| data/import-csv | Rule | CSV/Excel 数据导入 Ontology 实例，自动类型推断 | P1 |
| data/export-data | Rule | 查询结果导出为 CSV/Excel/JSON，嵌入数据密级水印 | P1 |

**本体操作类（4 个）**

| 技能 | 类型 | 描述 | 优先级 |
|------|------|------|--------|
| ontology/search | LLM | pgvector 语义匹配 + Ontology 元数据过滤的跨类型跨实例语义搜索 | P0 |
| ontology/suggest-type | LLM | 分析列名/类型/样本值，建议 ObjectType 定义 schema | P1 |
| ontology/map-source | LLM | 建议数据源字段到 ObjectType 属性的映射关系 | P1 |
| ontology/lineage-query | Rule | 通过 Neo4j 图遍历查询数据血缘，结果按用户权限过滤 | P0 |

**智能查询类（4 个）**

| 技能 | 类型 | 描述 | 优先级 |
|------|------|------|--------|
| query/nl2sql | LLM | 以 Ontology 元数据为上下文的自然语言转 SQL，输出经 SQL 安全沙箱校验 | P0 |
| query/explain-result | LLM | 总结数据模式、离群值、趋势变化，生成自然语言分析摘要 | P0 |
| query/suggest-chart | LLM | 根据数据维度和分析意图推荐最合适的可视化类型 | P1 |
| query/generate-chart | Hybrid | 规则化图表类型选择 + LLM 标题/注释生成 -> ECharts 配置 | P0 |

**数据治理类（4 个）**

| 技能 | 类型 | 描述 | 优先级 |
|------|------|------|--------|
| governance/classify-column | LLM | 分析列名 + 样本值，自动建议数据分级 (public/internal/confidential/top-secret) | P1 |
| governance/detect-anomaly | Hybrid | 规则引擎统计检查 + LLM 解释异常原因 | P0 |
| governance/suggest-rule | LLM | 分析数据分布，建议完整性/一致性/范围质量规则 | P1 |
| governance/catalog-search | LLM | 自然语言检索数据资产目录 | P1 |

**报表类（3 个）**

| 技能 | 类型 | 描述 | 优先级 |
|------|------|------|--------|
| report/generate | Hybrid | 模板 + 查询数据 + LLM 洞察/结论 = 完整报告 | P1 |
| report/summarize | LLM | 对查询结果或仪表盘数据生成执行摘要 | P0 |
| report/schedule | Rule | 注册 dts-scheduler cron 任务，定期生成并推送报告 | P1 |

**动作类（3 个）**

| 技能 | 类型 | 描述 | 优先级 |
|------|------|------|--------|
| action/navigate | Rule | 返回导航指令给前端，引导用户跳转到特定页面/视图 | P0 |
| action/create-workflow | Rule | 在 dts-workflow 创建审批工单（HITL 审批流的执行者） | P0 |
| action/notify | Rule | 发布通知事件到 Kafka，支持 email/webhook/in-app 多渠道投递 | P1 |

### 5.2 数据层技能（6 个）

数据层技能由 dts-data-connector 和 dts-data-quality 实现，提供数据接入和质量管控能力。

**连接器类（4 个）**

| 技能 | 类型 | 描述 | 优先级 |
|------|------|------|--------|
| connector/discover-metadata | Rule | 自动扫描数据源的表/列结构 | P0 |
| connector/test-connection | Rule | 验证数据源连通性 | P0 |
| connector/sync-cdc | Rule | 启动 Kafka CDC 同步，实时变更数据捕获 | P1 |
| connector/suggest-mapping | LLM | AI 建议源表字段到 Ontology ObjectType 的映射 | P1 |

**质量类（4 个）**

| 技能 | 类型 | 描述 | 优先级 |
|------|------|------|--------|
| quality/run-rules | Rule | 执行 SQL 质量检查规则，报告每条规则的 pass/fail | P0 |
| quality/detect-drift | Hybrid | 当前 vs 历史数据分布统计对比 + LLM 解释漂移原因 | P1 |
| quality/suggest-fix | LLM | 给定质量问题，建议纠正 SQL 或处理方案 | P1 |
| quality/auto-clean | Hybrid | 规则化去重/格式标准化，歧义场景由 LLM 确认 | P2 |

### 5.3 AI 核心技能（9 个）

AI 核心技能由 dts-agent、dts-intent-engine 和 dts-ai-eval 实现，是 Agent 运行时的核心能力。

**Agent 类（5 个）**

| 技能 | 类型 | 描述 | 优先级 |
|------|------|------|--------|
| agent/route-intent | LLM | 解析自然语言为 Intent 结构，选择目标 Skill 组合（dts-intent-engine 核心） | P0 |
| agent/plan-steps | LLM | LangGraph 状态机生成多步执行计划，每步 = 一次 Skill 调用 | P0 |
| agent/explain-decision | LLM | 读取 AI Trace 树，生成人类可读的推理链路解释 | P0 |
| agent/collect-feedback | Rule | 收集用户反馈 (thumbs up/down/correction)，存入 dts-ai-eval 驱动 Bad Case 飞轮 | P0 |
| agent/switch-persona | Rule | 切换活跃 Persona（行业人格），改变对话的行业上下文 | P1 |

**评测类（4 个）**

| 技能 | 类型 | 描述 | 优先级 |
|------|------|------|--------|
| eval/run-suite | Rule | 批量执行评测数据集，计算准确率/延迟/成本指标 | P0 |
| eval/compare-versions | Rule | 同一评测套件对比两个 Skill 版本，生成对比报告 | P0 |
| eval/generate-cases | LLM | 基于 Ontology schema + 样本数据自动生成多样化测试用例 | P1 |
| eval/analyze-bad-cases | LLM | 聚类分析失败用例，按错误类型归因，建议改进方向 | P1 |

### 5.4 行业技能（18 个）

行业技能由对应的 AppPack 提供，体现了 DTS 的行业深度。这些技能封装了各行业的决策知识精华。

**电力行业（6 个）— energy-pack**

| 技能 | 类型 | 描述 | 约束 |
|------|------|------|------|
| energy/predict-load | LLM | 基于历史数据 + 气象数据预测变压器/线路负荷 | SCADA 数据按区域权限过滤 |
| energy/detect-defect | Hybrid | 从 SCADA 数据中检测设备缺陷模式 | 缺陷分类遵循 DL/T 741 标准 |
| energy/create-workorder | Workflow | 创建维保工单，触发 HITL 审批 | 在运设备操作必须人工审批 |
| energy/assess-risk | LLM | 基于多指标综合评估电网区段风险 | — |
| energy/generate-safety-ticket | Workflow | 生成安全工作票，多级审批 | 强制 HITL |
| energy/report-reliability | Hybrid | 生成供电可靠性 (ASAI) 报告 | — |

**科研院所（6 个）— research-pack**

| 技能 | 类型 | 描述 | 约束 |
|------|------|------|------|
| research/track-milestone | Hybrid | 跟踪项目里程碑，标记高风险项 | — |
| research/analyze-quality | LLM | 分析质量检验数据，识别最差工序 | 质量操作必须 HITL |
| research/initiate-ncr-review | Workflow | 启动 NCR 归零五步法评审流程 | 五步法闭环不可跳步 |
| research/compare-batches | LLM | 跨供应商物料批次质量对比 | — |
| research/generate-test-report | Hybrid | 从实验数据生成测试报告 | 数据保留不少于 10 年 |
| research/bom-impact-analysis | Rule | 分析 BOM 变更对产品的影响链 | — |

**制造业（6 个）— manufacturing-pack**

| 技能 | 类型 | 描述 | 约束 |
|------|------|------|------|
| mfg/calculate-oee | Rule | 基于 availability x performance x quality 计算 OEE | SCADA 高频数据用 ClickHouse |
| mfg/predict-maintenance | LLM | 基于 SCADA 趋势 + 维保历史预测设备故障 | — |
| mfg/optimize-schedule | LLM | 排程优化建议（约束: 设备/人员/物料/交期） | — |
| mfg/detect-quality-drift | Hybrid | 从质检时序数据检测质量漂移趋势 | — |
| mfg/create-hold-order | Workflow | 创建质量扣留单，QA 经理审批 | 质量放行/扣留必须 QA 审批 |
| mfg/root-cause-analysis | LLM | 从多维生产数据分析缺陷根因 | — |

### 5.5 运维技能（7 个）

运维技能由 dts-agent + dts-operator 协同实现，覆盖平台自身的运维管理。

| 技能 | 类型 | 描述 | 优先级 |
|------|------|------|--------|
| ops/check-health | Rule | 查询 K8s API 获取所有 DTS 服务的 Pod 状态和就绪探针 | P0 |
| ops/diagnose-error | LLM | 读取 Loki 日志 + Tempo 追踪，LLM 识别根因并建议修复 | P1 |
| ops/scale-service | Workflow | 服务副本数扩缩容，需管理员审批 | P1 |
| ops/backup-database | Rule | 触发 pg_dump / clickhouse-backup，存储到 MinIO | P0 |
| ops/install-pack | Workflow | 安装 AppPack，管理员审批 -> 签名校验 -> 沙箱测试 -> 执行 migration | P0 |
| ops/upgrade-pack | Workflow | 升级 AppPack，兼容性检查 + 管理员审批 | P1 |
| ops/generate-report | Hybrid | 生成系统健康/使用量/成本报告 | P1 |

---

## 6. AppPack 生态

### 6.1 AppPack 概念

AppPack 是 DTS 的行业能力单元，采用"半成品 + Connector"策略: 不做完整行业应用（如完整 MES），而是封装行业决策层的精华（约 20%），通过 Connector 对接客户已有的业务系统。

**定位**: 你已有的 MES 管流程，DTS 管决策。

**AppPack 内容物**: Ontology 定义 (实体/关系/指标/规则/操作) + AI Skills (gRPC 接口) + Connectors (数据源适配器) + Frontend (UI 组件) + Workflows (审批模板) + Persona (行业人格) + Metrics (指标定义) + Quality Rules (数据质量规则) + Actions (业务操作) + Evaluations (评测数据集) -- 共 10 种能力类型。

**垂直深度光谱**:

| 层级 | 定义 | DTS 定位 |
|------|------|---------|
| Level 0 | 通用数据平台 | 传统大数据平台停在这里 |
| Level 1 | 行业知识插件 | DTS AppPack 的核心层级: Ontology + Skills + Prompts |
| Level 2 | 垂直应用模块 | 未来可选扩展: "在 AppPack 里做决策层应用" |
| Level 3 | 独立垂直 SaaS | 不做: 避免变成应用厂商 |

**核心原则**: AppPack 只装决策层精华，CRUD 和流程留在交付层。做项目来炼认知，不做项目来卖产品。

### 6.2 AppPack 生命周期

```
开发 (ADK)                      测试                        发布
  |                              |                           |
  | Pack Studio (可视化+AI对话)  | dts-pack test (自带用例)  | dts-pack publish
  | Pack CLI (命令行)            | dts-pack validate (完整性) |   -> 签名校验
  | AI 对话提取隐性知识           | 沙箱环境运行              |   -> 静态扫描
  |                              |                           |   -> 权限审核
  v                              v                           |   -> 兼容性检查
                                                             v
安装                          运行                         更新
  |                              |                           |
  | 管理员审批                   | 运行时隔离                | 版本对比
  | 内核执行 migrations          | GraalVM 沙箱 (脚本)      | Migration diff
  | 加载到运行时                 | DataAccessGate (数据)    | 回滚脚本
  |                              | Pipeline Filter (安全)   |
  v                              v                           v
```

**开发模式**:
- Pack Studio: Web IDE，AI 对话为主要交互方式。描述业务场景 -> AI 生成 Pack 骨架 -> 专家验证。面向行业一线从业者（调度员、组长、主任）
- Pack CLI: 命令行工具 (dts-pack init/dev/test/validate/build/publish/upgrade)，面向专业开发者

### 6.3 AppPack 安全

| 安全维度 | 机制 |
|---------|------|
| **运行时隔离** | 脚本 Skill 在 GraalVM Polyglot 沙箱执行，内存/CPU/时间限制 + API 白名单 |
| **权限沙箱** | Pack 的 Skill 通过 DataAccessGate 访问数据，不能直接 JDBC 连库、不能读写文件系统、不能执行系统命令 |
| **数据访问审计** | Pack 的自定义 Skill 经过与内置 Skill 完全相同的 ToolExecutionPipeline 安全链路: Permission -> PolicyGate -> Guardrail -> Confidence -> Approval -> Execution -> ResultValidation -> Fallback -> Audit |
| **发布安全流程** | 签名校验 (publisher 证书) -> 静态扫描 (禁止 API 检查) -> 权限审核 -> 沙箱测试 -> 兼容性检查 |
| **网络隔离** | Pack 只能访问 pack.yaml 白名单中声明的外部域名 |

---

## 7. KnowHow 知识管理

### 7.1 知识金字塔

DTS 基于 Polanyi 缄默知识理论和 LeCun 多模态预训练研究，构建五层知识金字塔:

```
Layer 4: 世界模型 (World Model)
  Agent 能预测操作后果: State + Action -> Next State
  1% 领域数据即可涌现
  目标准确率: TBD (Phase 3 引入)
  ─────────────────────────────────────────────
Layer 3: 隐性模式 (Tacit Patterns)
  专家没说但一直在用的规则
  从多模态行为数据中统计推断
  目标准确率: ~60%, weight=0.25
  ─────────────────────────────────────────────
Layer 2: 潜在知识 (Latent Knowledge)
  专家能说但没人问过的判断逻辑
  Gen-Ba 碎片化提取（决策瞬间自然追问）
  目标准确率: ~75%, weight=0.30
  ─────────────────────────────────────────────
Layer 1: 显性结构 (Explicit Structure)
  数据字典、行业标准、系统表结构
  传统 Ontology 建模
  目标准确率: ~95%, weight=0.25
  ─────────────────────────────────────────────
Layer 0: 多模态感知 (Multimodal Perception)
  视觉 + 声音 + 时序 + 文本
  目标准确率: TBD (Phase 2 引入)
```

**孪生分数** = Sum(layer_weight x layer_accuracy)，残余不确定性由 HITL 覆盖（铁律一: 人工随时可接管）。

**获取机制与存储方式**:

| 层级 | 获取机制 | 存储方式 |
|------|---------|---------|
| L0 | OPC-UA/MQTT 传感器 + 视觉/音频采集 | ClickHouse (时序) + MinIO (文件) |
| L1 | 数据字典导入 + 标准文档解析 | PostgreSQL (Ontology 元数据) |
| L2 | Gen-Ba 碎片策略: 决策瞬间自然追问 | PostgreSQL + pgvector (知识碎片) |
| L3 | 长期运行数据统计推断 | pgvector (模式向量) + PostgreSQL |
| L4 | 通用预训练 + 领域微调 | 模型文件 (MinIO) |

### 7.2 KnowHow 提取

DTS 采用基于 Gen-Ba 碎片策略的四阶段 KnowHow 提取流水线，超越传统 Nonaka SECI 模型的局限:

**Phase 1: 现场观察 (Socialization)**
- 专家在 Pilot 端做日常决策
- AI Agent 静默观察: 查看了哪些数据、做了什么判断、花了多长时间、采取了什么行动
- 不干扰专家工作流

**Phase 2: 碎片化外化 (Fragmented Externalization)**
- 不要求专家"系统性描述"（那是传统知识管理失败的原因）
- 在决策瞬间自然追问: "您刚才为什么选择提前维保？"
- 输出知识碎片: 容许不完整、容许矛盾、标记置信度
- 关键原则: 保持专家在 focal awareness，不破坏 subsidiary awareness

**Phase 3: 松散组合 (Loose Combination)**
- LLM 将知识碎片聚类、去矛盾、补全
- 生成 KnowHow 草稿 (status: draft)
- 关联到 Ontology ObjectType
- 不追求完美，追求"够用的近似"

**Phase 4: 协作内化 (Collaborative Internalization)**
- KnowHow 转化为 Skill (status: testing)
- 在真实场景中与专家并行运行
- 专家校正 AI 输出 -> 反馈循环 -> Bad Case 飞轮
- 通过 evaluation suite 量化准确率
- 达到 baseline_accuracy -> status: active

**三种专家角色**:

| 角色 | 参与方式 | 知识层级 |
|------|---------|---------|
| 日常使用者 | 通过 Pilot 正常工作，行为被 AI 学习 | L0-L2 |
| 知识审核者 | 审核 AI 生成的 KnowHow 草稿 | L2-L3 |
| 领域守护者 | 定义 Ontology 边界和不变量 | L3-L4 |

### 7.3 KnowHow 交易

**知识资产发布**: 专家/ISV 开发的 KnowHow 封装在 AppPack 中，经评测验证后发布到知识市场。

**知识市场**: AppPack Marketplace 同时是知识交易平台。行业一线从业者通过 AI 对话提取隐性经验，结构化为 AppPack 后发布到市场持续获得分润。

**贡献者激励**:
- 平台抽成 30% (类 App Store 模式)
- 贡献者（行业专家/ISV/咨询公司）获得 70% 分润
- 复用率和用户评价驱动排名

---

## 8. 安全架构

### 8.1 认证: Keycloak + JWT

- Keycloak 作为统一身份源，支持 OIDC/SAML/PKI 三种协议
- JWT 令牌在服务间通过 gRPC metadata (RequestContext) 传递
- dts-gateway 强制验证所有外部请求的 JWT
- 内部服务间通过 JWT 传递身份，无需 mTLS（JWT 足以建立信任）

### 8.2 授权: RBAC + ABAC

- RBAC: 基于角色的功能权限控制（菜单/按钮/API 级别）
- ABAC: 基于属性的数据权限控制（数据分级 x 用户属性 -> allow/mask/deny）
- 角色自适应菜单: 权限高不等于看到所有功能，"功能角色"和"数据权限"分离

### 8.3 数据安全: 分级分类 + 动态脱敏

- 四级分类: public / internal / confidential / top-secret
- 动态脱敏: 按用户权限实时脱敏敏感字段
- 行级过滤: SecuritySqlRewriter 自动注入 WHERE 条件
- 列级控制: 按列返回 allow/mask/deny 策略
- 数据水印: 报告和导出嵌入密级水印和用户标识
- TaskContext 加密: 按任务最高涉及密级加密存储中间态，任务结束后按策略清理

### 8.4 审计: 全链路追踪

- 人和 AI 走同一个 Capability API，审计天然统一
- CloudEvents 规范，Kafka 异步收集
- Append-only 不可篡改（哈希链），涉密环境加密存储
- 每个 SkillResponse 内嵌审计条目，不依赖开发者额外实现

### 8.5 AI 安全: RAG 检索过滤 + Prompt 注入防护

- RAG 检索结果按用户数据权限过滤，防止越权信息泄露
- 传给 LLM 的数据摘要按用户可见范围裁剪
- AppPack 无特权: Pack 的 Skill 看到的数据 = 当前用户有权看到的数据
- Prompt 注入防护: ToolExecutionPipeline 中的 Guardrail Filter 层

---

## 9. 可观测性

### 9.1 OpenTelemetry 全链路追踪

- 所有服务 (Python/Java/Go) 统一接入 OpenTelemetry SDK
- traceId 贯穿整个请求链路: 前端 -> Gateway -> 业务服务 -> AI 服务 -> 数据层

### 9.2 Grafana 统一仪表盘

| 组件 | 用途 |
|------|------|
| Grafana Tempo | 分布式追踪可视化，支持按 traceId 查询完整调用链 |
| Prometheus | 指标监控: 请求延迟/吞吐/错误率/资源使用 |
| Grafana Loki | 日志聚合和检索，多维度过滤 |
| Grafana Dashboard | 统一看板: 服务健康/AI 性能/业务指标 |

### 9.3 AI Agent 执行链路可视化

- AI Trace 树: 可视化展示 Agent 的多步推理过程
- 每步记录: 输入/输出/Skill 选择理由/数据访问/Token 消耗/耗时
- traceId 关联: 基础设施 Trace 和 AI 执行 Trace 通过同一个 traceId 串联

### 9.4 成本统计与优化

- 每次 LLM 调用记录 token 消耗和计算成本
- 按用户/租户/Skill/时间段汇总成本报表
- 成本异常告警: 单次调用成本超阈值自动提醒
- 优化建议: 高频调用的 Skill 建议缓存或规则化降级

---

## 10. 部署架构

### 10.1 All-in-K8s

所有组件（应用服务 + 中间件）统一运行在 Kubernetes 上。运行时统一 RKE2，不保留非 K8s 运行形态。

- 中间件 (PostgreSQL/Kafka/ClickHouse/Neo4j/MinIO) 默认部署在 K8s 内
- 支持通过 Helm values 指向外部中间件实例
- 在线 vs 离线: 唯一差异是 LLM 端点 + 镜像仓库地址

### 10.2 dts-operator 自动化管理

Go 语言实现的 K8s Operator，负责:
- DTS 组件生命周期管理（部署/升级/回滚/健康检查）
- AppPack 部署与运行时管理
- 中间件配置和状态管理
- 证书和密钥自动轮转

### 10.3 在线/离线统一架构

| 场景 | 规模 | 特点 |
|------|------|------|
| 开发单机 | 1 节点 | single-node RKE2，本地开发 |
| 小型离线 | 1-3 节点 | 离线镜像仓库 + 本地 LLM |
| 中型集群 | 3-10 节点 | 标准部署，HA 中间件 |
| 云环境 | 弹性 | 托管 K8s + 云 LLM 端点 |

### 10.4 Helm Chart + 离线 Bundle

- Helm 是部署真相源，分层 Chart: platform-base / dts-core / extensions
- Values 分层: common / local-rke2 / shared-rke2 / ci / prod / offline
- 离线 Bundle: dts-cli 打包所有镜像 + Chart + 配置为离线安装包
- Argo CD / GitOps 用于环境级持续交付

### 10.5 dts-cli 运维工具

```
dts install          # 安装 DTS 集群
dts upgrade          # 升级版本
dts backup           # 备份数据
dts restore          # 恢复数据
dts offline-bundle   # 打包离线安装包
dts pack install     # 安装 AppPack
dts pack upgrade     # 升级 AppPack
dts health           # 健康检查
```

---

## 11. 开发者体验

### 11.1 DAP 协议 SDK

为三种语言提供 DAP 协议 SDK:
- **Python SDK**: AI Skill 开发，实现 SkillService gRPC 接口
- **Java SDK**: 业务 Skill 开发，平台扩展
- **Go SDK**: 基础设施扩展，Operator 插件

### 11.2 AppPack ADK (Application Development Kit)

ADK 的核心理念: 开发者不一定是程序员，更重要的是行业一线从业者。

**Pack Studio (面向行业专家)**:
- AI Pack Builder: 对话式创建 Pack，描述业务场景 -> AI 生成完整骨架
- Ontology Designer: 可视化拖拽 + AI 建议补全
- Tool Playground: 描述规则 -> AI 生成脚本 + 即时测试
- Quality Rule Builder: 自然语言 -> 规则表达式
- Pack Validator + Publish

**Pack CLI (面向专业开发者)**:
- dts-pack init / dev / test / validate / build / publish / upgrade
- 本地开发服务器 (Mock Kernel API + 热重载)

### 11.3 API 体系

| API 类型 | 协议 | 用途 |
|---------|------|------|
| REST API | HTTP/JSON | 外部集成，前端调用（经 dts-gateway） |
| gRPC API | Protocol Buffers | 内部服务间通信，Skill 调用 |
| SSE | Server-Sent Events | Agent 执行进度流式推送到前端 |
| Kafka Events | CloudEvents | 审计/CDC/通知等异步事件 |

### 11.4 开发者文档

- API Reference: 自动生成的 OpenAPI 3.0 / gRPC protobuf 文档
- DAP 协议规范: Ontology/Skill/Intent/Security/Audit 五层协议详细说明
- AppPack 开发指南: 从零开始创建行业 Pack 的完整教程
- 示例 AppPack: 电力/科研/制造三个参考实现

---

## 12. 技术规格

### 12.1 支持的数据源

| 类别 | 数据源 |
|------|--------|
| 关系数据库 | PostgreSQL, MySQL, Oracle, SQL Server, ClickHouse |
| 大数据 | Hive, Spark SQL (via JDBC) |
| 文件 | CSV, Excel, JSON, Parquet (via MinIO) |
| API | REST endpoint (JSON response) |
| 工业协议 | OPC-UA (MES/SCADA), MQTT (IoT sensors) |

### 12.2 性能指标

| 指标 | 目标 |
|------|------|
| NL2SQL 响应时间 | < 3 秒 (P95) |
| 简单查询端到端 | < 5 秒 (P95) |
| 多步决策任务 | < 120 秒 (含 HITL 等待外) |
| Skill 调用延迟 | < 500ms (规则类), < 3s (LLM 类) |
| 并发对话数 | 100+ (单集群) |
| 数据源连接数 | 50+ (单租户) |
| Ontology 规模 | 1000+ ObjectType, 10000+ 实例 |
| 审计日志吞吐 | 10000+ events/s (Kafka) |

### 12.3 部署要求

| 配置 | 最低要求 | 推荐配置 |
|------|---------|---------|
| CPU | 16 核 | 32 核+ |
| 内存 | 32 GB | 64 GB+ |
| 磁盘 | 200 GB SSD | 500 GB+ SSD |
| K8s 节点 | 1 (开发) | 3+ (生产) |
| LLM | 本地 7B 模型 / 云端 API | 本地 70B+ 或云端 GPT-4 级别 |

### 12.4 浏览器兼容性

| 浏览器 | 最低版本 |
|--------|---------|
| Chrome | 100+ |
| Firefox | 100+ |
| Safari | 16+ |
| Edge | 100+ |

---

## 附录 A: 关键术语表

| 术语 | 含义 |
|------|------|
| **DAP** | DTS Agent Protocol — AI Agent 与物理世界的交互协议，产品核心护城河 |
| **Decision Twin** | 决策过程的数字孪生 — 不是物理镜像，是"替你想"的系统 |
| **Ontology** | 企业认知框架 — 实体+关系+指标+规则+操作的结构化表达 |
| **Intent** | 结构化决策意图 — 自然语言解析后的目标+约束+执行计划 |
| **Skill** | AI 技能 — 遵循 DAP Layer 2 协议的标准化能力单元 |
| **AppPack** | 全栈行业能力包 — 封装决策层精华的可插拔插件 |
| **KnowHow** | 行业隐性知识的结构化表达 — 从碎片到验证的知识资产 |
| **HITL** | Human-In-The-Loop — 高风险操作必须人工审批 |
| **Persona** | 行业人格 — Prompt + Skill 集 + few-shot 的行业上下文配置 |
| **Gen-Ba** | 现场碎片提取策略 — 在决策瞬间自然追问，容许不完整 |
| **ADK** | Application Development Kit — AppPack 开发工具链 |
| **POV** | Proof of Value — 5 天用客户真实数据证明价值 |

## 附录 B: 服务清单

| 语言 | 服务 | 职责 |
|------|------|------|
| Python | dts-agent | Agent 运行时 (LangGraph), 多步推理, Persona |
| Python | dts-intent-engine | NL -> 结构化 Intent, 路由 |
| Python | dts-ontology-engine | 语义推理, 向量索引, NL->Ontology 映射 |
| Python | dts-data-connector | 多源接入, CDC, 格式转换 |
| Python | dts-data-quality | AI 数据质量检测, 异常发现 |
| Python | dts-query-ai | NL2SQL, 查询建议 |
| Python | dts-ai-eval | 评测套件, 回归门禁, Bad Case 飞轮 |
| Python | dts-observability | AI Trace 树, Agent 链路追踪, 成本统计 |
| Python | dts-scheduler | 任务调度 DAG, 定时 Skills, backfill, SLA |
| Python | dts-perception (P2) | 多模态输入 (语音/视觉), 预留 |
| Java | dts-platform | 用户/角色/权限/租户/菜单/配置/Keycloak |
| Java | dts-gateway | API 网关, 认证, gRPC-Web |
| Java | dts-ontology-store | Ontology CRUD (types/instances/relations/metrics/rules/actions) |
| Java | dts-query-service | SQL 查询执行, 数据源路由 |
| Java | dts-governance | 元数据管理, 数据血缘, 分级分类 |
| Java | dts-asset | 数据资产目录, 搜索, 共享 |
| Java | dts-data-security | 动态脱敏, 行列级权限, 水印 |
| Java | dts-workflow | 审批引擎 (HITL, 访问申请, 变更审批) |
| Java | dts-audit-log | 审计日志 (append-only, 不可篡改) |
| Java | dts-data-service | 数据 API 发布, Token/配额, 推送/订阅 |
| Go | dts-operator | K8s Operator, 组件生命周期, AppPack 部署 |
| Go | dts-cli | CLI 安装/升级/备份/离线包/Pack 管理 |
| Frontend | dts-pilot-webapp | 决策门户 (Vercel AI SDK, @dnd-kit) |
| Frontend | dts-cortex-webapp | 专家工作台 (React Flow) |
| Frontend | dts-admin-webapp | 管理控制台 |

---

*本文档基于 DTS AI Decision OS 架构设计文档、DAP 协议规范、知识战略框架编写。*
*版本 v3.0.0 | 2026-03-12*
