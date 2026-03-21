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
| CS-10 | 数据源表单收敛与错误透传 | DONE | CS-06 |
| CS-11 | Copilot 动态数据源绑定 | DONE | CS-10 |

**统计**: READY=0, IN_PROGRESS=1, DONE=10, BLOCKED=0

## Sprint-10: 园林业务语义层与 NL2SQL 落地 (BG)

| ID | 任务 | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| BG-01 | 业务域盘点与高频问句清单 | P0 | READY | IN-03, NV-07 |
| BG-02 | 业务视图层建设 | P0 | READY | BG-01 |
| BG-03 | 状态码与业务枚举词典 | P0 | READY | BG-01 |
| BG-04 | 预制查询模板 TOP 20 | P0 | READY | BG-02, BG-03 |
| BG-05 | 语义模型基线与视图元数据标注 | P1 | READY | BG-02 |
| BG-06 | 业务语义包（项目履约 + 现场业务） | P1 | READY | BG-05 |
| BG-07 | 意图路由规则引擎 | P1 | READY | BG-02, BG-03 |
| BG-08 | 结算域指标直查对齐 | P2 | READY | BG-02 |
| BG-09 | 查询权限桥接 | P2 | READY | BG-02, BG-07 |
| BG-10 | IT 集成测试与验收矩阵 | P2 | READY | BG-01~09, BG-11 |
| BG-11 | Copilot 交互体验增强 | P0 | READY | BG-04, BG-07 |

**统计**: READY=11, IN_PROGRESS=0, DONE=0, BLOCKED=0

## Sprint-11: 轻量 ELT 主题层与增量同步 (EL)

| ID | 任务 | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| EL-01 | 主题层表结构设计 | P0 | READY | BG-02 |
| EL-02 | 增量同步引擎 | P0 | READY | EL-01 |
| EL-03 | 项目履约日维度主题表 | P0 | READY | EL-01, EL-02 |
| EL-04 | 现场业务事件事实表 | P0 | READY | EL-01, EL-02 |
| EL-05 | 意图路由扩展（视图层 vs 主题层判定） | P1 | READY | EL-03, EL-04 |
| EL-06 | 主题层预制查询模板补充 | P1 | READY | EL-03, EL-04 |
| EL-07 | 同步监控与告警 | P2 | READY | EL-02 |
| EL-08 | IT 集成测试与性能基准 | P2 | READY | EL-01~07 |

**统计**: READY=8, IN_PROGRESS=0, DONE=0, BLOCKED=0

## Sprint-13: ELT 主题层收口与数仓分层整改 (ER)

| ID | 任务 | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| ER-01 | ELT 物理表与同步 SQL 对齐 | P0 | DONE | EL-01, EL-03, EL-04 |
| ER-02 | Watermark 模型与状态机收口 | P0 | DONE | EL-02, EL-07 |
| ER-03 | 项目履约主题表同步链修复 | P0 | DONE | ER-01, ER-02 |
| ER-04 | 现场业务事实表同步链修复 | P0 | DONE | ER-01, ER-02 |
| ER-05 | Data-layer 路由接入 Copilot 主链 | P1 | DONE | EL-05, ER-03, ER-04 |
| ER-06 | 主题层健康检查与自动降级 | P1 | DONE | ER-02, ER-05 |
| ER-07 | 监控、手动触发与编排服务统一 | P2 | DONE | ER-02, ER-03, ER-04 |
| ER-08 | IT 验收与性能基线补齐 | P2 | DONE | ER-03~ER-07 |
| ER-09 | 数仓分层策略与落库核验 | P1 | DONE | ER-03, ER-04 |

**统计**: READY=0, IN_PROGRESS=0, DONE=9, BLOCKED=0

## Sprint-14: 已知报表优先化与 Copilot 探索兜底 (RF)

| ID | 任务 | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| RF-01 | 初始报表目录锁定与候选清单 | P0 | DONE | 2026-03-20 known report fastpath plan |

**统计**: READY=0, IN_PROGRESS=0, DONE=1, BLOCKED=0

## Sprint-15: Copilot 语音输入支持 (VI)

| ID | 任务 | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| VI-01 | useVoiceInput Hook 实现 | P0 | READY | - |
| VI-02 | VoiceInputButton 组件 | P0 | READY | VI-01 |
| VI-03 | CopilotChat 集成 | P0 | READY | VI-02 |
| VI-04 | 移动端适配与手势处理 | P1 | READY | VI-03 |
| VI-05 | 后端 ASR 降级接口（可选） | P2 | READY | VI-03 |
| VI-06 | IT 测试与兼容性验证 | P2 | READY | VI-01~04 |

**统计**: READY=2, IN_PROGRESS=2, DONE=5, BLOCKED=0

## Sprint-16: 业务页面盘点、数据库对照与固定报表实施基线 (RC)

| ID | 任务 | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| RC-01 | 现网页面盘点与 `adminweb/app` 报表型页面识别 | P0 | DONE | 真实登录态 |
| RC-02 | 业务库扫描与 `adminapi/adminweb/app` 对照 | P0 | DONE | RC-01 |
| RC-03 | 固定报表 Top 30 候选目录 | P0 | DONE | RC-01, RC-02 |
| RC-04 | 受控取数面策略（L0 / L1） | P0 | DONE | RC-02, RC-03 |
| RC-05 | 固定报表模板模型与目录种子 | P1 | IN_PROGRESS | RC-03, RC-04 |
| RC-06 | 模板优先 Copilot 路由接入 | P1 | IN_PROGRESS | RC-05 |
| RC-07 | Dashboard / Screen / Report Factory 模板复用 | P2 | DONE | RC-05 |
| RC-08 | IT 验收与性能基线 | P2 | DONE | RC-04~RC-07 |
| RC-09 | 固定报表 backing 审计与占位模板退役 | P0 | DONE | RC-03, RC-04 |

**统计**: READY=0, IN_PROGRESS=2, DONE=7, BLOCKED=0

## Sprint-17: 主数据优先治理与业务锚点收口 (MD)

| ID | 任务 | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| MD-01 | 主数据盘点与归属审计 | P0 | DONE | `adminapi/adminweb/app`、业务库 |
| MD-02 | 项目轴 canonical model | P0 | DONE | MD-01 |
| MD-03 | 物品轴 canonical model | P0 | DONE | MD-01 |
| MD-04 | 共享参考主数据模型 | P1 | DONE | MD-01 |
| MD-05 | 主数据与交易事实边界表 | P0 | DONE | MD-01~04 |
| MD-06 | 主数据消费规则（固定报表 / Copilot） | P1 | DONE | MD-02~05 |
| MD-07 | 迁移顺序与验收基线 | P1 | DONE | MD-01~06 |

**统计**: READY=0, IN_PROGRESS=0, DONE=7, BLOCKED=0

## Sprint-19: Copilot 与查询资产中心协同工作流 (202603)

| Feature | Task 数 | 状态 |
|---------|---------|------|
| F1-分析草稿模型与接口 | 3 | DONE |
| F2-查询资产中心承接草稿 | 3 | READY |
| F3-Copilot到查询的协同动作 | 3 | READY |
| F4-草稿晋升与IT验证 | 3 | READY |

**统计**: READY=9, IN_PROGRESS=0, DONE=3, BLOCKED=0

## Backlog

| ID | 任务 | 状态 | 说明 |
|----|------|------|------|
| BL-01 | Join Contract 与 Allowed Tables 编译 | DEFERRED | 视图层已替代其核心功能，降为补充手段 |

## 总体统计

**READY=78, IN_PROGRESS=4, DONE=33, BLOCKED=0, DEFERRED=1 (总计 116 任务)**
