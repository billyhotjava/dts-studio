# v1.0.0 Sprint Queue

## Queue Rules

1. 同一时间最多一个 `IN_PROGRESS`。
2. `DONE` 必须附带可复现验证命令或报告链接。
3. sprint-1 为基础设施，必须最先完成。
4. sprint-2/3 为 AI 引擎主线，sprint-5 为 BI 主线，两条主线在 sprint-4（认证）后汇合。

## Sprint-1: 项目脚手架与基础设施 (SC)

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| SC-01 | Maven 多模块项目骨架搭建 | READY | - |
| SC-02 | copilot_ai schema Liquibase 基线 | READY | SC-01 |
| SC-03 | copilot_analytics schema Liquibase 基线 | READY | SC-01 |
| SC-04 | Docker Compose 编排 | READY | SC-01 |
| SC-05 | Ollama 容器集成与健康检查 | READY | SC-04 |
| SC-06 | 证书生成脚本与本地 CA | READY | - |
| SC-07 | Traefik 反向代理与 TLS 终止 | READY | SC-04, SC-06 |
| SC-08 | CI 构建脚本与冒烟验证 | READY | SC-01~07 |

**统计**: READY=8, IN_PROGRESS=0, DONE=0, BLOCKED=0

## Sprint-2: AI 引擎核心抽取 (AE)

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| AE-01 | OpenAI 兼容客户端抽取 | READY | SC-01 |
| AE-02 | LLM Gateway 服务（多 Provider + 熔断降级） | READY | AE-01 |
| AE-03 | AI 配置管理服务（Provider 模板 + 持久化） | READY | AE-01 |
| AE-04 | AiCopilotService 核心抽取（complete/stream/explain/optimize） | READY | AE-02, AE-03 |
| AE-05 | NL2SQL 服务抽取（语义增强） | READY | AE-04 |
| AE-06 | AI REST API 端点 | READY | AE-04, AE-05 |
| AE-07 | AI 引擎集成测试 | READY | AE-01~06 |

**统计**: READY=7, IN_PROGRESS=0, DONE=0, BLOCKED=0

## Sprint-3: AI 高级能力抽取 (AA)

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| AA-01 | pgvector schema + Embedding 服务迁移 | READY | AE-01 |
| AA-02 | RAG 向量存储与混合检索抽取 | READY | AA-01 |
| AA-03 | ReAct Agent 引擎抽取 | READY | AE-04 |
| AA-04 | Tool 注册与执行管线抽取（裁剪治理专用 Tool） | READY | AA-03 |
| AA-05 | 安全防护抽取（SQL 沙箱 + 权限过滤 + 审计） | READY | AA-03, AA-04 |
| AA-06 | Agent Chat 会话管理（持久化 + 流式） | READY | AA-03 |
| AA-07 | AI 高级能力集成测试 | READY | AA-01~06 |

**统计**: READY=7, IN_PROGRESS=0, DONE=0, BLOCKED=0

## Sprint-4: API Key 认证与安全体系 (AK)

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| AK-01 | API Key 数据模型与管理服务 | READY | SC-02 |
| AK-02 | API Key 认证过滤器（copilot-ai） | READY | AK-01 |
| AK-03 | 用户身份传递与会话建立 | READY | AK-02 |
| AK-04 | API Key 认证集成到 copilot-analytics | READY | AK-02, SC-03 |
| AK-05 | 认证体系集成测试 | READY | AK-01~04 |

**统计**: READY=5, IN_PROGRESS=0, DONE=0, BLOCKED=0

## Sprint-5: BI 分析引擎抽取 (BA)

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| BA-01 | dts-analytics 核心代码 fork 与包名重构 | READY | SC-03 |
| BA-02 | 数据源管理独立化（从 copilot-ai 获取） | READY | BA-01, AE-06 |
| BA-03 | 认证层替换（PlatformTrustedUser → ApiKeyAuth） | READY | BA-01, AK-04 |
| BA-04 | AI 屏幕生成对接 copilot-ai | READY | BA-01, AE-06 |
| BA-05 | SQL Workbench + 仪表盘 + 报表功能验证 | READY | BA-01~04 |
| BA-06 | 公开链接与嵌入式分析 | READY | BA-05 |
| BA-07 | BI 引擎集成测试 | READY | BA-01~06 |

**统计**: READY=7, IN_PROGRESS=0, DONE=0, BLOCKED=0

## Sprint-6: 前端 Webapp 抽取与整合 (FE)

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| FE-01 | dts-analytics-webapp fork 与品牌重构 | READY | BA-01 |
| FE-02 | AI 聊天面板组件合并（从 dts-platform-webapp） | READY | FE-01 |
| FE-03 | API 客户端适配（指向 copilot-ai + copilot-analytics） | READY | FE-01, FE-02 |
| FE-04 | iframe 嵌入模式支持 | READY | FE-03 |
| FE-05 | Dockerfile 与静态资源打包 | READY | FE-01~04 |
| FE-06 | 前端集成测试 | READY | FE-01~05 |

**统计**: READY=6, IN_PROGRESS=0, DONE=0, BLOCKED=0

## Sprint-7: 园林平台集成与端到端验证 (IN)

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| IN-01 | 园林平台 Gateway 路由配置（转发到 copilot 服务） | READY | AK-05 |
| IN-02 | adminweb 嵌入 copilot-webapp（iframe / 菜单集成） | READY | FE-04 |
| IN-03 | 园林平台数据源注册（MySQL 业务库接入 copilot） | READY | BA-02 |
| IN-04 | 园林业务 Tool 扩展示例（查询项目/花卉/财务数据） | READY | AA-04, IN-03 |
| IN-05 | 端到端集成测试（登录→AI 对话→BI 查询→仪表盘） | READY | IN-01~04 |

**统计**: READY=5, IN_PROGRESS=0, DONE=0, BLOCKED=0

## Sprint-8: NL2SQL 聊天到可视化闭环 (NV)

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| NV-01 | 默认数据源自动注册 | READY | BA-02 |
| NV-02 | Agent Chat 透传 datasourceId | READY | AA-06 |
| NV-03 | NL2SQL Agent 系统提示词优化 | READY | NV-02 |
| NV-04 | CopilotChat 数据源选择器 | READY | NV-01, FE-02 |
| NV-05 | CopilotChat "创建可视化" 按钮 | READY | NV-03 |
| NV-06 | CardEditorPage autorun 支持 | READY | FE-03 |
| NV-07 | 同义词字典可配置化 | READY | NV-03 |
| NV-08 | 端到端冒烟测试与评测用例 | READY | NV-01~06 |

**统计**: READY=8, IN_PROGRESS=0, DONE=0, BLOCKED=0

## Sprint-9: Copilot 系统配置中心 (CS)

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| CS-01 | AI Provider 安全 DTO 与更新语义 | DONE | AE-03 |
| CS-02 | Analytics 聚合配置接口（站点设置 + Provider 代理） | DONE | CS-01, BA-03 |
| CS-03 | Analytics 聚合 API Key 管理接口 | DONE | AK-01, CS-02 |
| CS-04 | Webapp 配置 API 客户端 | DONE | CS-02, CS-03, FE-03 |
| CS-05 | Webapp 系统配置页面与导航入口 | DONE | CS-04 |
| CS-06 | 联调验证与回归测试 | IN_PROGRESS | CS-01~05 |
| CS-07 | Provider 模板目录增强（国际/国内主流 + 推荐模板元数据） | DONE | AE-03, CS-01 |
| CS-08 | Webapp Provider Type 下拉与推荐模板联动 | DONE | CS-04, CS-05, CS-07 |
| CS-09 | Provider 模板化交互回归验证 | DONE | CS-07, CS-08 |

**统计**: READY=0, IN_PROGRESS=1, DONE=8, BLOCKED=0

## 总体统计

**READY=53, IN_PROGRESS=1, DONE=8, BLOCKED=0 (总计 62 任务)**
