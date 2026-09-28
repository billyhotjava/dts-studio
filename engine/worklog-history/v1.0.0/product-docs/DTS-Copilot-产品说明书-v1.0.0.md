---
marp: true
theme: default
paginate: true
size: 16:9
style: |
  section {
    font-family: "PingFang SC", "Microsoft YaHei", "Noto Sans CJK SC", sans-serif;
    background: #0f172a;
    color: #e2e8f0;
  }
  h1 { color: #38bdf8; font-size: 2.2em; }
  h2 { color: #38bdf8; font-size: 1.6em; border-bottom: 2px solid #1e3a5f; padding-bottom: 8px; }
  h3 { color: #7dd3fc; }
  strong { color: #facc15; }
  code { background: #1e293b; color: #38bdf8; padding: 2px 6px; border-radius: 4px; }
  table { font-size: 0.78em; }
  th { background: #1e3a5f; color: #e2e8f0; }
  td { background: #1e293b; }
  blockquote { border-left: 4px solid #38bdf8; padding-left: 16px; color: #94a3b8; }
  a { color: #38bdf8; }
  section.lead { text-align: center; }
  section.lead h1 { font-size: 3em; }
  .columns { display: flex; gap: 32px; }
  .col { flex: 1; }
  ul { font-size: 0.92em; }
  li { margin-bottom: 4px; }
---

<!-- _class: lead -->

# DTS Copilot

### AI-Native 智能数据分析平台

**v1.0.0 产品说明书**

---

## 目录

1. **产品概述** — 定位与价值主张
2. **核心能力** — NL2SQL / AI Copilot / 大屏自动生成
3. **产品架构** — 技术栈与服务拓扑
4. **NL2SQL 引擎** — 自然语言转 SQL
5. **AI Copilot 对话** — ReAct Agent + 工具链
6. **大屏自动生成** — 从描述到可视化
7. **BI 分析引擎** — 仪表盘 / 查询 / 数据源
8. **LLM 网关** — 多模型熔断与降级
9. **安全体系** — SQL 沙箱 / 审计 / 认证
10. **RAG 混合检索** — 向量 + 全文检索
11. **部署方案** — Docker / 本地 / 云
12. **集成方案** — Gateway 嵌入式接入
13. **竞争优势** — 差异化价值
14. **产品路线图** — 战略演进方向
15. **总结**

---

## 1. 产品概述

### 定位

DTS Copilot 是一款 **AI-Native 智能数据分析平台**，将大语言模型深度融入数据查询、可视化与决策全链路。

### 核心价值

| 痛点 | DTS Copilot 方案 |
|------|-----------------|
| 业务人员不会写 SQL | **NL2SQL** — 自然语言提问，AI 自动生成安全 SQL |
| 大屏设计耗时费力 | **AI 大屏生成** — 一句话描述，自动输出完整布局 + 图表 + SQL |
| BI 工具割裂、数据孤岛 | **统一平台** — 查询、仪表盘、大屏、指标管理一站式 |
| LLM 供应商锁定 | **多模型网关** — 17 家 LLM 热切换，熔断自动降级 |
| SQL 注入风险 | **三层 SQL 沙箱** — 从 LLM 输出到执行全链路安全 |

---

## 2. 核心能力矩阵

<div class="columns">
<div class="col">

### AI 能力
- **NL2SQL** — 自然语言 → 安全 SQL
- **SQL 补全 / 解释 / 优化**
- **ReAct Agent** — 推理 + 行动循环
- **大屏自动生成 & 迭代优化**
- **RAG 混合检索增强**
- **SSE 流式对话**

</div>
<div class="col">

### BI 能力
- **仪表盘** — 拖拽布局 + 参数联动
- **大屏设计器** — 24 栅格 + 组件库
- **SQL / 可视化查询构建器**
- **数据源管理** — PG / MySQL / Oracle / 达梦
- **指标管理** — 定义、版本、血缘
- **公开分享** — UUID 链接无需登录

</div>
</div>

---

## 3. 产品架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Traefik (TLS 443)                            │
│   /api/ai/* → copilot-ai    /api/* → analytics    /* → webapp      │
└──────┬──────────────────────────┬──────────────────────┬────────────┘
       │                          │                      │
  ┌────▼─────┐            ┌───────▼──────┐        ┌──────▼─────┐
  │copilot-ai│            │  analytics   │        │   webapp   │
  │  :8091   │◄──────────►│    :8092     │        │  React 19  │
  │ LLM/NL2  │  REST API  │  BI Engine   │        │  Vite 6    │
  │ Agent/RAG│            │  Screen/Dash │        │  TypeScript│
  └────┬─────┘            └──────┬───────┘        └────────────┘
       │                          │
  ┌────▼──────────────────────────▼───────┐
  │     PostgreSQL 17 + pgvector          │
  │  schema: copilot_ai | copilot_analytics│
  └───────────────────────────────────────┘
```

**技术栈**: Java 21 / Spring Boot 3.4 / React 19 / TypeScript / Vite 6 / PostgreSQL 17

---

## 4. NL2SQL 引擎

### 工作原理

```
用户提问 → 注入 Schema 上下文(DDL) → LLM 生成 SQL → SqlSafetyChecker 校验 → 返回
      "最近30天各项目的加花数量"       ↓
                              SELECT p.name, COUNT(f.id) AS flower_count
                              FROM p_project p
                              JOIN p_flower_record f ON p.id = f.project_id
                              WHERE f.created_at >= CURRENT_DATE - INTERVAL '30 days'
                              GROUP BY p.name ORDER BY flower_count DESC
```

### 关键特性

| 特性 | 说明 |
|------|------|
| **Schema 感知** | 自动注入目标数据库的 DDL（表结构、字段类型、主外键）|
| **安全约束** | System Prompt 强制只生成 SELECT/WITH，输出经 SafetyChecker 二次校验 |
| **低温生成** | temperature=0.3，确保 SQL 语法准确性 |
| **同义词字典** | 可配置业务术语映射（花卉→p_flower_record）|
| **多数据源** | 支持按会话绑定不同数据源 |

---

## 5. AI Copilot 对话引擎

### ReAct Agent 循环

```
用户消息 → LLM 推理(Thought) → 选择工具(Action) → 执行工具(Observation)
                    ↑                                        │
                    └────────── 反馈结果，继续推理 ◄──────────┘
                              （最多 10 轮迭代）
```

### 内置工具

| 工具 | 功能 | 安全保障 |
|------|------|---------|
| `schema_lookup` | 查询表结构元数据 | 只读 JDBC MetaData |
| `execute_query` | 执行 SQL 查询 | SqlSandbox 白名单 + 100 行限制 + 30s 超时 |
| 园林项目查询 | 按条件筛选项目 | 预定义 SQL 模板 |
| 花卉统计 | 多维度聚合 | 预定义 SQL 模板 |
| 财务摘要 | 多类型报表 | 预定义 SQL 模板 |

### 四大 AI 能力

- **SQL 补全** — 输入片段或描述，AI 补全完整查询
- **SQL 解释** — 将复杂 SQL 翻译为自然语言
- **SQL 优化** — 分析索引使用、Join 顺序，给出优化建议
- **流式对话** — SSE 实时输出，打字机效果

---

## 6. 大屏自动生成引擎

### 从一句话到完整大屏

```
用户输入: "生成一个面向运营的周报大屏，包含趋势、结构占比、区域排名和明细表"
    │
    ▼
┌─────────────────────────────────────────────────────┐
│  LLM 生成完整 screenSpec JSON                        │
│  ├─ intent: 运营领域, 近7天, 日粒度                    │
│  ├─ sqlBlueprints: 4 条 SQL (趋势/占比/排名/明细)      │
│  ├─ vizRecommendations: line-chart, pie-chart, ...   │
│  └─ screenSpec: 24栅格布局, KPI行+图表行+明细行         │
└─────────────────────────────────────────────────────┘
    │
    ▼
  创建草稿 → 进入大屏设计器 → 微调发布
```

### 双模式迭代优化

| 模式 | 行为 |
|------|------|
| **Apply 模式** | AI 直接修改大屏配置，即改即用 |
| **Suggest 模式** | AI 生成建议方案，人工确认后应用 |

支持 **多轮对话上下文**（最近 12 轮），逐步精细化大屏设计。

---

## 7. BI 分析引擎

<div class="columns">
<div class="col">

### 仪表盘
- 拖拽式卡片布局
- 参数映射 & 全局筛选联动
- 公开分享（UUID 链接）
- 嵌入 iframe 支持

### 查询构建
- **SQL 编辑器** — 语法高亮 + AI 补全
- **可视化构建器** — 点选表/字段/聚合
- 结果缓存 + 导出

</div>
<div class="col">

### 大屏设计器
- 24 列栅格 + 自由布局
- 10+ 图表组件（折线、柱状、饼图、KPI、雷达...）
- 实时协作编辑 + 编辑锁
- 版本快照 + 一键回滚
- 行业模板 & 插件系统
- 全屏预览 + 导出

### 高级分析
- 分析会话、报告工厂、指标透视、全局搜索

</div>
</div>

---

## 8. LLM 多模型网关

### 17 家 LLM 开箱即用

<div class="columns">
<div class="col">

**国际**
- OpenAI GPT-4o
- Azure OpenAI
- Anthropic Claude Sonnet
- Google Gemini 2.5 Flash
- Groq Llama 3.3 70B
- Mistral Small

</div>
<div class="col">

**国内**
- DeepSeek ⭐ (默认推荐)
- 通义千问 Qwen Plus
- 智谱 GLM-4
- Moonshot Kimi
- 百度千帆 ERNIE
- 火山方舟 / 豆包
- SiliconFlow

</div>
<div class="col">

**本地部署**
- Ollama (离线)
- vLLM
- LM Studio

</div>
</div>

### 熔断 & 降级机制

```
请求 → 默认 Provider → 成功 ✓
                     → 失败 → 熔断器标记不可用 → 自动切换 Provider B → 成功 ✓
                                                                   → 失败 → Provider C ...
```

- 每个 Provider 独立熔断器，故障自动隔离
- 支持手动重置，恢复后自动参与路由
- 环境变量 + 数据库双重配置，灵活切换

---

## 9. 安全体系

### 三层 SQL 安全防护

```
Layer 1: SqlSafetyChecker (NL2SQL 输出校验)
  → 仅允许 SELECT / WITH 开头
  → 拦截 INSERT / UPDATE / DELETE / DROP / TRUNCATE / ALTER / GRANT ...

Layer 2: GuardrailsInterceptor (Agent 工具执行前拦截)
  → 对 execute_query 参数做预校验
  → 无论成功/失败均写审计日志

Layer 3: SqlSandbox (最终执行校验)
  → 去注释、去字符串后分析
  → 拦截多语句(分号分隔)攻击
  → 白名单: SELECT / WITH / EXPLAIN / SHOW
```

### 认证与审计

| 机制 | 说明 |
|------|------|
| **API Key** | `cpk_` 前缀，SHA256 哈希存储，创建时一次性展示 |
| **Admin Secret** | 引导阶段的管理员密钥 |
| **审计日志** | 独立事务，记录所有工具执行与对话消息 |
| **用户上下文** | `X-DTS-User-Id` 头透传，多租户隔离 |

---

## 10. RAG 混合检索

### 架构

```
文档/Schema/SQL 片段 → Embedding → pgvector 存储
                                      │
用户查询 → Embedding ────────────────►│ 向量相似度搜索 (Top 3N)
        → tsvector/tsquery ──────────►│ 全文关键词搜索 (Top 3N)
                                      │
                                      ▼
                            Reciprocal Rank Fusion (k=60)
                                      │
                                      ▼
                              Top-N 融合结果 → 注入 LLM Context
```

### 特点

- **pgvector** 嵌入存储与业务库共享 PostgreSQL，零额外基础设施
- **RRF 融合算法** — 兼顾语义相关性与关键词精确匹配
- 支持 `schema`、`document`、`sql` 三种内容类型
- 3x 候选集策略，确保融合覆盖率

---

## 11. 部署方案

### 方式一：Docker Compose（推荐）

```bash
git clone <repo>
cp .env.example .env         # 配置 LLM API Key
docker compose up -d          # 一键启动全部服务
```

**服务清单**: PostgreSQL + copilot-ai + analytics + webapp + Traefik

### 方式二：开发环境

```bash
./dev.sh                      # 一键启动（infra + backend + frontend）
./dev.sh status               # 查看各服务状态
./dev.sh logs                 # 查看后端日志
```

### 方式三：Kubernetes

```yaml
# 每个微服务独立 Deployment + Service
# ConfigMap 管理 LLM 配置
# Ingress 替代 Traefik
```

### 硬件要求

| 场景 | CPU | 内存 | 存储 |
|------|-----|------|------|
| 公有云 LLM（推荐） | 2C | 4GB | 20GB |
| 本地 Ollama 7B | 4C | 16GB | 40GB |

---

## 12. 园林平台集成方案

### 网关嵌入模式

```
┌───────────────────────────────────────────────────────┐
│                  园林平台 (rs-gateway)                  │
│                                                       │
│  adminweb ──► /copilot/ai/**   ──► copilot-ai:8091   │
│  adminweb ──► /copilot/web/**  ──► copilot-webapp:80  │
│                                                       │
│            CopilotAuthConvertFilter                    │
│            JWT → Bearer API Key + X-DTS-User-Id       │
└───────────────────────────────────────────────────────┘
```

### 集成步骤

1. **生成 API Key** — `POST /api/auth/keys`
2. **注册业务数据源** — 脚本自动写入
3. **Gateway 路由** — Nacos 添加 copilot 路由规则
4. **认证转换** — 添加 `CopilotAuthConvertFilter`（JWT → API Key）
5. **前端嵌入** — iframe 加载 copilot-webapp 或 Vue 组件

### 适配能力

- **任意 Spring Cloud Gateway** 均可接入
- **多平台接入** — 每个平台独立 API Key
- **用户透传** — 保留原平台用户身份

---

## 13. 竞争优势

<div class="columns">
<div class="col">

### vs 传统 BI（Metabase / Superset）
- **AI 原生** — NL2SQL + 大屏生成，零代码分析
- **Agent 自主推理** — 多步骤数据探索
- **对话式交互** — 非表单驱动

### vs 纯 AI 产品（ChatGPT / 文心一言）
- **数据安全** — 私有化部署，SQL 沙箱
- **BI 闭环** — 从提问到图表到大屏一站式
- **数据源直连** — 实时数据，非文件上传

</div>
<div class="col">

### 独特优势
- **17 家 LLM 热切换** — 无厂商锁定
- **三层 SQL 安全** — 业界领先的防护深度
- **ReAct Agent** — 自主决策 + 工具调用
- **RAG 混合检索** — 向量 + 全文融合
- **嵌入式架构** — 零侵入集成现有系统
- **国产适配** — 达梦数据库 + 国产 LLM

</div>
</div>

---

## 14. 产品路线图

### 近期（v1.1 — Q2 2026）

| 方向 | 规划 |
|------|------|
| **多数据源联邦查询** | 跨库 JOIN，统一数据视图 |
| **自动可视化推荐** | NL2SQL 结果自动匹配最佳图表类型 |
| **对话式仪表盘** | "在仪表盘上加一个本月销量趋势图" |
| **定时报告推送** | 周报/日报自动生成 + 邮件/钉钉推送 |

### 中期（v2.0 — Q4 2026）

| 方向 | 规划 |
|------|------|
| **语义层 / Metrics Store** | 统一指标定义，口径一致 |
| **异常检测 & 预警** | AI 自动识别数据异常 |
| **多模态分析** | 支持图片/文档/Excel 上传分析 |
| **协同分析空间** | 多人实时协作 + 评论 + 标注 |

### 长期（v3.0 — 2027）

| 方向 | 规划 |
|------|------|
| **自主分析 Agent** | 完全自主的端到端数据分析 |
| **知识图谱** | 企业数据关系自动发现与推理 |
| **行业解决方案** | 制造 / 园林 / 金融 / 零售垂直包 |
| **SaaS 多租户** | 云端托管 + 按量计费 |

---

## 15. 战略定位

### 三层演进模型

```
                  ┌──────────────────────────┐
          2027    │   自主分析 Agent 平台      │  ← 完全自动化
                  │   知识图谱 + 行业方案      │
                  ├──────────────────────────┤
          2026    │   AI-Native BI 平台       │  ← 当前阶段
                  │   NL2SQL + 大屏生成 + RAG  │
                  ├──────────────────────────┤
          基础     │   传统 BI 引擎            │
                  │   仪表盘 + 查询 + 数据源    │
                  └──────────────────────────┘
```

### 核心战略

1. **AI First** — 每个功能都有 AI 增强入口
2. **开放生态** — 多 LLM、多数据源、插件系统
3. **安全合规** — 私有化部署、SQL 沙箱、审计全链路
4. **渐进集成** — 嵌入式架构，0 到 1 低门槛接入

---

## 产品架构全景图

```
┌──────────────────────────────────────────────────────────────────────────┐
│                              DTS Copilot v1.0.0                          │
│                                                                          │
│  ┌─ AI Engine (copilot-ai) ──────────────────────────────────────────┐  │
│  │  NL2SQL │ SQL补全/解释/优化 │ ReAct Agent │ 大屏生成 │ RAG │ 审计  │  │
│  │  ┌─────────────────────────────────────────────────────────┐      │  │
│  │  │  LLM Gateway (17 Providers, Circuit Breaker, Fallback)  │      │  │
│  │  └─────────────────────────────────────────────────────────┘      │  │
│  │  ┌──────────────────────────┐ ┌──────────────────────────┐       │  │
│  │  │  SQL Sandbox (3 Layers)  │ │  API Key Auth + Audit    │       │  │
│  │  └──────────────────────────┘ └──────────────────────────┘       │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌─ BI Engine (copilot-analytics) ───────────────────────────────────┐  │
│  │  仪表盘 │ 大屏设计器 │ 查询卡片 │ 数据源 │ 指标 │ 报告 │ 公开分享  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌─ Frontend (copilot-webapp) ───────────────────────────────────────┐  │
│  │  AI Chat Panel │ 大屏设计器 │ 仪表盘 │ SQL 编辑器 │ 管理后台       │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌─ Infrastructure ──────────────────────────────────────────────────┐  │
│  │  PostgreSQL 17 + pgvector  │  Traefik TLS  │  Ollama (Optional)   │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 典型应用场景

### 场景 1：园林项目运营分析

> **用户**: "最近30天各项目的加花记录，按项目分组统计"
> **AI**: 自动查询 p_project + p_flower_record → 柱状图 + 明细表

### 场景 2：一键生成运营大屏

> **用户**: 点击"自动生成"→ 输入"生产车间运营大屏，含产量趋势、良率、设备状态"
> **AI**: 生成 4 个 KPI + 3 个图表 + 1 个明细表的完整大屏 → 一键创建草稿

### 场景 3：多步分析探索

> **用户**: "最近哪些客户的订单下降了？"
> **Agent**: schema_lookup → 发现 sales_order 表 → execute_query → 分析结果 → "客户 A 订单环比下降 32%，建议关注"

### 场景 4：嵌入现有系统

> 园林管理平台 → Gateway 一个 Filter → copilot 即嵌入 → 业务人员直接在原系统中用 AI 分析

---

## 开箱体验

### 5 分钟快速启动

```bash
# 1. 克隆代码
git clone <repo> && cd dts-copilot

# 2. 配置 LLM（只需一个 API Key）
echo "LLM_API_KEY=sk-your-key" >> .env

# 3. 一键启动
docker compose up -d

# 4. 访问
open https://copilot.local      # 需配置 hosts
# 或 http://localhost:3003       # 开发模式
```

### 首次使用

1. 访问 → 自动进入初始化向导
2. 创建管理员账户
3. 添加数据源（MySQL / PostgreSQL / Oracle / 达梦）
4. 打开 AI Copilot → 开始提问

---

## 总结

### DTS Copilot v1.0.0

<div class="columns">
<div class="col">

**已交付能力**
- NL2SQL 自然语言查询
- ReAct Agent 多步推理
- 大屏 AI 自动生成
- 17 家 LLM 多模型网关
- 三层 SQL 安全沙箱
- RAG 混合检索增强
- 完整 BI 分析引擎
- 嵌入式集成方案

</div>
<div class="col">

**核心数据**
- **3** 个微服务
- **17** 家 LLM 支持
- **10+** 图表组件
- **4** 种数据库适配
- **3** 层安全防护
- **2** 种语言(中/英)
- **1** 条命令部署
- **0** 厂商锁定

</div>
</div>

---

<!-- _class: lead -->

# 感谢阅读

**DTS Copilot — AI 赋能每一次数据决策**

`v1.0.0` · 2026.03

> 联系我们: dts-copilot@yuzhi.com
