# Sprint-23: 基于现有业务优势的 Agent BI 报表 (AR)

**时间**: 2026-05  
**前缀**: AR (Agent Report)  
**状态**: DONE (本地回归通过，待上线环境 IT 补证)  
**目标**: 把 PRS 现有应用系统的业务沉淀、权限边界、报表资产和 dbt 主题表转成 Agent 可使用的 BI 语义资产，让业务用户能用自然语言生成可信报表，并能保存为查询、图表或大屏。

## 背景

PRS 不是空白数据库。`adminapi` 已经有成熟的 `rs-flowers-base` 业务模块，覆盖报花、项目、客户、摆位、采购、库存、租赁应收、发票、回款、费用、任务、巡检和审批；`adminweb` 已有对应菜单、列表、固定报表、帆软/JimuReport 入口和租赁月报页面；`dts-copilot` 已有语义包、固定报表快路径、`REPORT_DRAFT`、`analysis_draft`、图表/大屏能力，以及 PRS 报花域 dbt 模型和 12 张大屏 JSON。

因此 Sprint-23 不做泛化的“让大模型直接扫全库写 SQL”。本 sprint 的路线是：

```text
现有业务系统优势
  -> 业务域资产图
  -> Agent BI 报表目录
  -> 取数面路由：固定报表 / dbt ADS-DWS / adminapi 只读接口
  -> REPORT_DRAFT
  -> 预览 / 保存 / 创建图表 / 加入大屏
```

## 核心判断

### PRS 当前的真实优势

| 优势 | 对 Agent BI 的价值 | 本 sprint 处理方式 |
|---|---|---|
| 业务流程完整 | Agent 可以理解“报花 -> 采购 -> 入库 -> 摆放 -> 月账 -> 开票 -> 回款”的业务链，而不是只看表名 | 先整理业务域资产图 |
| 页面和接口已按业务域拆好 | adminweb 目录和 adminapi controller 已经天然提供业务语义 | 把页面/接口映射成报表意图和业务词汇 |
| 有固定报表和报表配置能力 | 高频场景不需要每次动态生成 SQL | 优先走固定报表或模板快路径 |
| 有 dbt ADS/DWS 试点 | 趋势、排行、汇总类报表有稳定取数面 | PRS 租赁/报花域优先走 `xycyl_ads_*` / `xycyl_dws_*` |
| 有权限、审批、日志 | 后续业务反向指导可以走受控动作，不绕开业务系统 | Sprint-23 只做建议和草稿，不直接写业务数据 |

### Agent 报表分层

| 层 | 名称 | 适用问题 | 首选取数面 |
|---|---|---|---|
| L2 | 已有报表资产 | “打开租赁月报”“看报花总览大屏” | 固定报表、12 张 screen JSON、帆软/JimuReport |
| L1 | 主题分析 | “今年各项目租赁收入趋势”“坏账最多的客户” | dbt ADS/DWS |
| L0 | 业务明细 | “这张单现在谁在处理”“这个项目有哪些待确认账单” | adminapi 只读 Bridge 或现有业务接口 |

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 状态 | 说明 |
|----|---------|---------|--------|------|------|
| F1 | 现有业务系统优势资产化 | 2 | P0 | DONE | 盘点 adminapi/adminweb/dts-copilot，形成业务域资产图和问句分类 |
| F2 | Agent BI 语义与报表目录 | 3 | P0 | DONE | 定义报表目录、数据面路由、`REPORT_DRAFT` 协议 |
| F3 | 自然语言导报表闭环 | 3 | P0 | DONE | Copilot 报表入口、预览、保存、创建图表/大屏 |
| F4 | PRS 租赁首个验收场景 | 3 | P0 | DONE | 以租赁/报花域为首个自然语言出表验收场景 |

## 影响范围

### 文档和资产

- `assets/application-advantages-and-domain-map.md`：现有应用优势总结 + 业务域到 Agent BI 的映射。
- `worklog/prs/v1/agent-business-report-design.md`：作为架构输入，Sprint-23 不重复搬运，只在任务中引用。
- `worklog/prs/v1/screens/*.json`：作为 L2 固定大屏资产输入。
- `worklog/prs/v1/xycyl-flowerbiz-dbt-model.zip`：作为 L1 主题分析资产输入。

### 后续代码方向

- `dts-copilot-ai`：Planner、语义包、模板匹配、SQL 安全、`REPORT_DRAFT` 输出。
- `dts-copilot-analytics`：`analysis_draft`、执行预览、保存图表、加入大屏。
- `dts-copilot-webapp`：Copilot 报表卡、报表预览、保存/创建图表交互。
- `adminweb`：后续嵌入 Copilot 报表入口，保留原有业务页面和固定报表。
- `adminapi`：后续提供 L0 只读 Bridge 和受控业务动作；本 sprint 不要求直接写业务动作。

## 完成标准

- [x] PRS 现有业务系统优势已整理为 5 个以上 Agent BI 可使用的业务域。
- [x] 每个业务域明确：高频自然语言问句、首选取数面、数据质量等级、可生成的报表类型、是否允许业务建议。
- [x] Agent Planner 能区分 `FIXED_REPORT`、`REPORT_DRAFT`、`BUSINESS_DETAIL`、`BUSINESS_INSIGHT`、`ACTION_PROPOSAL` 响应意图。
- [x] `REPORT_DRAFT` 输出包含 SQL、展示建议、数据来源、质量等级、可保存动作和可视化建议。
- [x] PRS 租赁/报花域已整理 20 条以上自然语言问句，其中 8 条以上覆盖 `REPORT_DRAFT` 草稿、图表和加入大屏链路。
- [x] 对数据质量为 `MEDIUM` / `LOW` 的报表，前端必须显示口径和质量提示。
- [x] 不允许 Agent 对业务库执行写操作；业务反向指导只生成建议和动作草稿。

## 实施记录

- `dts-copilot-ai` 增加 Agent BI 报表目录、L0/L1/L2 路由、`REPORT_DRAFT` metadata、SSE `done` 事件扩展和会话消息持久化字段。
- `dts-copilot-analytics` 增加 `analysis_draft` 的 `response_kind`、`data_surface`、`quality_level`、`quality_notes`、`report_code` 字段和接口返回。
- `dts-copilot-webapp` 增加 Copilot 报表草稿 metadata 传递、生成报表卡质量提示、保存草稿和“加入大屏”入口。
- `it/golden-questions-prs-agent-bi.md` 提供 24 条 PRS Golden Questions。
- `it/test_prs_agent_bi_report.sh` 提供后端、分析端、前端本地回归和可选上线 API 冒烟。

## 与相邻 sprint 的关系

| Sprint | 关系 |
|---|---|
| sprint-16 | 已做业务页面盘点和固定报表实施基线，Sprint-23 复用其“固定报表优先”的原则 |
| sprint-19/20 | 已有 `analysis_draft` 和查询资产中心协同工作流，Sprint-23 把它升级为自然语言导报表主链 |
| sprint-22 | PRS 报花 dbt、12 张大屏和数据质量分析是 Sprint-23 第一个验收域 |
| 后续 sprint | 可继续扩展采购、库存、财务、任务巡检、流程审批等业务域，并逐步接入受控业务动作 |

## 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| 直接 NL2SQL 访问裸业务库 | SQL 不稳定、权限不可控、口径错误 | 强制 Planner 先选 L2/L1/L0 取数面 |
| PRS 历史数据质量一般 | Agent 报表可能误导业务 | 每个报表输出 `qualityLevel` 和口径说明 |
| 已有固定报表被重复生成 | 报表资产污染 | 固定报表和 12 张大屏优先匹配 |
| Agent 建议被误当作自动业务动作 | 业务风险 | Sprint-23 只生成建议/草稿，不执行写操作 |
| adminapi/adminweb 与 dts-copilot 边界不清 | 后续集成复杂 | 文档先明确 L0/L1/L2 和 Bridge 边界 |

## 输出物清单

- `README.md`：Sprint 总览。
- `assets/application-advantages-and-domain-map.md`：现有系统优势与业务域资产图。
- `features/F1-现有业务系统优势资产化/`：资产盘点与业务问句分类。
- `features/F2-Agent-BI语义与报表目录/`：报表目录、路由、协议。
- `features/F3-自然语言导报表闭环/`：产品闭环与前端/后端改造任务。
- `features/F4-PRS租赁首个验收场景/`：租赁/报花域首个验收。
- `it/README.md`：Sprint-23 验收入口。
