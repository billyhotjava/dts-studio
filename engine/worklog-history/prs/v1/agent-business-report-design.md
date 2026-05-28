# PRS Agent 报表与业务反向指导设计

**版本**: Draft v0.1  
**日期**: 2026-05-20  
**范围**: `dts-copilot`、`adminapi`、`adminweb`  
**首要落地目标**: 先完善基于 Agent 的报表生成、预览、保存和复用链路；业务反向指导先做建议和草稿，不直接自动改业务数据。

## 1. 能力定义

让业务用户可以在 PRS 管理后台里用自然语言生成报表、解释指标、下钻到业务明细，并在可信数据范围内形成业务建议。系统最终要能从报表异常反向指导业务系统，例如提示未出账、未开票、未回款、待审批、数据缺口，并在用户确认后生成待办、提醒或发起受控业务动作。

本能力不是“让大模型直接连业务库写 SQL”。目标是把 Agent 放在受控分析和受控动作中间：

```text
业务用户
  -> adminweb Copilot 报表入口
  -> dts-copilot Agent / NL2SQL / 报表草稿
  -> 数据仓库 ADS/DWS 或 adminapi 权威只读接口
  -> 报表预览 / 保存草稿 / 创建图表 / 加入大屏
  -> 业务建议
  -> 用户确认
  -> adminapi 受控动作
```

## 2. 当前事实基础

### 2.1 dts-copilot 已具备的基础

- 已有 `analysis_draft` 方向的设计，适合承接 Copilot 生成的 SQL 和报表草稿。
- 已有 Planner-first 对话编排设计，适合让 `ConversationPlannerService` 输出 `responseKind` 和执行模式。
- 已有固定报表快路径和模板目录，适合处理已知报表。
- PRS flowerbiz 已有 dbt 模型、ADS/DWS 主题表和 12 张大屏 JSON。
- PRS flowerbiz 的 `xycyl_ads_*`、`xycyl_dws_*` 可以作为 Agent 报表优先取数层。

### 2.2 adminapi 已具备的基础

- `rs-flowers-base` 是 PRS 核心业务模块，包含项目、报花、租摆、采购、库存、财务、统计、报表等业务能力。
- 已有租赁应收月报接口：
  - `/operate/rentalMonthlyReport/summary`
  - `/operate/rentalMonthlyReport/detail`
  - `/operate/rentalMonthlyReport/chart`
  - `/operate/rentalMonthlyReport/chartDetail`
- 已有报表打印和报表配置能力：
  - `/report/print/*`
  - `/biz/print/*`
  - `/report/config/*`
  - `/fanruan/*`
- 已有 Flowable 审批动作接口，包括启动、通过、拒绝、驳回、停止流程。
- 已有 `@Log` 操作日志、`@PreAuthorize` 权限、网关 token 鉴权和数据权限基础。

### 2.3 adminweb 已具备的基础

- 已有报表菜单、帆软报表入口、租赁月报页面和 ECharts 展示。
- 现有前端 API 都通过 `@/utils/request` 携带用户 token 走网关。
- 动态路由来自后端菜单，适合后续增加 Copilot 报表入口或业务页内 AI 面板。

## 3. 非目标

第一阶段不做以下事情：

- 不让 Agent 直接对 PRS 业务库执行 `INSERT/UPDATE/DELETE`。
- 不让 Agent 绕过 adminapi 的业务 service、权限和审批流程。
- 不把所有 adminapi controller 自动暴露成 Agent tool。
- 不重写现有租赁月报、帆软、JimuReport。
- 不用自由 SQL 代替已有的业务明细接口和权限边界。
- 不承诺低质量数据维度可以驱动自动业务动作。

## 4. 总体架构

### 4.1 三层职责

| 层 | 系统 | 职责 |
|---|---|---|
| 体验层 | `adminweb` | 提供 AI 报表入口、报表预览、草稿打开、图表创建、业务建议确认 |
| Agent 层 | `dts-copilot` | 意图规划、语义包、NL2SQL、安全 SQL、报表草稿、建议生成、动作编排 |
| 业务层 | `adminapi` | 权威业务查询、权限、数据范围、业务动作、审批、日志 |

### 4.2 数据取数分层

Agent 报表必须先判断取数面，而不是默认全库 NL2SQL。

| 层级 | 名称 | 适用场景 | 访问方式 |
|---|---|---|---|
| L0 | 业务权威只读 | 当前状态、台账、明细、可编辑对象详情 | adminapi 现有接口或新增只读 Bridge |
| L1 | 仓库主题层 | 趋势、排行、汇总、跨实体分析 | dbt DWD/DWS/ADS |
| L2 | 固定报表资产 | 已知报表、已沉淀大屏、帆软/JimuReport | 模板快路径或报表目录 |

PRS flowerbiz 的第一优先级：

```text
自然语言报表 / 汇总 / 趋势
  -> xycyl_ads_* / xycyl_dws_*

明细追溯 / 当前业务状态 / 要进入业务页面处理
  -> adminapi L0 只读接口或业务深链

已存在固定大屏 / 固定报表
  -> 固定模板或 screen JSON
```

## 5. 信任边界和硬约束

### 5.1 SQL 安全约束

Agent 生成 SQL 只能满足以下条件：

- 只允许 `SELECT`。
- 只允许访问语义包登记的表、视图或白名单数据集。
- 默认加行数上限。
- 默认禁止跨库自由拼接。
- 禁止访问敏感字段，除非语义包显式允许且当前用户有权限。
- 失败时返回可解释错误，不降级到无限制 SQL。

### 5.2 数据质量约束

PRS flowerbiz 当前数据质量不能一概视为可信。历史分析显示：

- ODS 技术同步质量较好。
- 客户关联不稳定，客户维度分析必须标注可信度。
- `t_change_info.biz_id` 关联主单存在结构性风险。
- `allocate_time` 等字段可能长期为空。
- 回收数据覆盖近期业务不足，回收类报表不能直接驱动自动动作。

因此 Agent 输出必须携带 `qualityLevel`：

| 等级 | 含义 | 行为 |
|---|---|---|
| `HIGH` | 数据链路、口径、字段完整性都稳定 | 可生成报表和建议 |
| `MEDIUM` | 口径可用但存在缺口 | 可生成报表，建议需提示核验 |
| `LOW` | 关键字段或关联不稳定 | 只能做探索和问题提示，不生成业务动作 |

### 5.3 业务动作约束

Agent 不能直接调用任意业务接口。所有可执行动作必须先注册为 `BusinessAction`：

- 有唯一 `actionCode`。
- 有输入 schema。
- 有权限码。
- 有前置条件检查。
- 支持 `preview`。
- 高风险动作必须 `confirm`。
- 执行后写入审计。
- 支持幂等键。
- 支持失败回滚或失败补偿说明。

## 6. 核心对象模型

### 6.1 报表草稿 `analysis_draft`

沿用 dts-copilot 分析草稿作为 Agent 报表落点。

建议字段：

| 字段 | 说明 |
|---|---|
| `id` | 草稿 ID |
| `source_type` | `copilot` / `manual` |
| `response_kind` | `REPORT_DRAFT` / `TEMPLATE_SQL` / `BUSINESS_ANALYSIS` |
| `session_id` | Copilot 会话 |
| `message_id` | Copilot 消息 |
| `question` | 原始自然语言问题 |
| `database_id` | 数据源 |
| `sql_text` | 安全 SQL |
| `explanation_text` | Agent 解释 |
| `suggested_display` | `table` / `line` / `bar` / `pie` / `scalar` |
| `quality_level` | 数据可信等级 |
| `quality_notes` | 数据质量提示 |
| `linked_card_id` | 后续生成图表 |
| `linked_screen_id` | 后续加入大屏 |
| `status` | `DRAFT` / `SAVED_QUERY` / `VISUALIZED` / `PUBLISHED` |

### 6.2 报表建议 `agent_report_insight`

用于保存 Agent 对报表结果的解释和可行动建议。

| 字段 | 说明 |
|---|---|
| `id` | 建议 ID |
| `draft_id` | 关联报表草稿 |
| `insight_type` | `ANOMALY` / `FOLLOW_UP` / `DATA_QUALITY` / `BUSINESS_RISK` |
| `severity` | `INFO` / `WARN` / `CRITICAL` |
| `title` | 建议标题 |
| `description` | 建议内容 |
| `evidence_sql` | 证据 SQL 或查询引用 |
| `evidence_snapshot` | 结果摘要 |
| `target_entity_type` | `project` / `customer` / `flower_biz` / `invoice` 等 |
| `target_entity_id` | 业务对象 ID |
| `suggested_action_code` | 可选动作编码 |
| `status` | `OPEN` / `DISMISSED` / `ACCEPTED` / `EXECUTED` |

### 6.3 业务动作注册 `agent_business_action`

建议第一版先用代码注册表，稳定后再落库。

| 字段 | 说明 |
|---|---|
| `action_code` | 例如 `rental.create_collection_followup` |
| `domain` | `rental` / `flowerbiz` / `finance` |
| `title` | 用户可读名称 |
| `permission` | adminapi 权限码 |
| `risk_level` | `LOW` / `MEDIUM` / `HIGH` |
| `input_schema` | JSON Schema |
| `preview_handler` | 预览处理器 |
| `execute_handler` | 执行处理器 |
| `requires_confirm` | 是否需要人工确认 |
| `idempotency_key_fields` | 幂等字段 |

### 6.4 动作提案 `agent_action_proposal`

Agent 只先创建提案，用户确认后才执行。

| 字段 | 说明 |
|---|---|
| `id` | 提案 ID |
| `run_id` | Agent 执行 ID |
| `insight_id` | 来源建议 |
| `action_code` | 动作编码 |
| `input_payload` | 参数 |
| `preview_result` | 预览结果 |
| `risk_level` | 风险等级 |
| `status` | `PROPOSED` / `CONFIRMED` / `EXECUTED` / `FAILED` / `CANCELLED` |
| `confirmed_by` | 确认人 |
| `executed_at` | 执行时间 |

## 7. API 设计

### 7.1 dts-copilot 报表 API

已有聊天流继续保留，同时补齐报表资产化接口。

| API | 用途 |
|---|---|
| `POST /api/copilot/chat/stream` | 对话生成，返回 `REPORT_DRAFT` |
| `POST /api/analysis-drafts` | 保存 Agent 报表草稿 |
| `POST /api/analysis-drafts/{id}/run` | 试跑草稿 SQL |
| `POST /api/analysis-drafts/{id}/create-visualization` | 从草稿创建图表 |
| `POST /api/analysis-drafts/{id}/publish-screen` | 加入大屏 |
| `POST /api/report-insights/generate` | 基于报表结果生成建议 |

### 7.2 adminapi Agent Bridge

建议在 `rs-flowers-base` 新增受控 Bridge，不暴露自由动作。

```text
GET  /rs-flowers-base/agent/context/reportable-domains
GET  /rs-flowers-base/agent/context/entities/{entityType}/{id}
POST /rs-flowers-base/agent/actions/{actionCode}/preview
POST /rs-flowers-base/agent/actions/{actionCode}/execute
GET  /rs-flowers-base/agent/actions
```

第一阶段只需要只读上下文和动作预览，不执行写动作。

### 7.3 调用方式

adminweb 侧用户 token 是权限来源：

```text
adminweb
  -> dts-copilot
      携带用户 token 或用户上下文
  -> adminapi
      使用用户身份访问 Bridge
      由 adminapi 复用现有权限、数据范围、业务 service
```

不建议让 dts-copilot 伪造 `FROM_SOURCE=INNER` 绕过网关。内部调用只适合服务自身健康检查或后台任务，不适合代表用户执行业务动作。

## 8. adminweb 交互设计

### 8.1 新入口

第一版建议新增一个轻入口，不打断现有页面：

- 左侧菜单：`AI 报表`
- 或在已有报表菜单下加二级页：`智能报表`

页面能力：

- 自然语言输入。
- 数据域选择：`PRS 花卉租赁`、`项目`、`财务`。
- 时间范围快捷选择。
- Agent 生成 SQL 和解释。
- 预览表格。
- 保存草稿。
- 创建图表。
- 加入大屏。

### 8.2 业务页内入口

在高价值页面增加局部入口：

| 页面 | 入口 |
|---|---|
| 租赁应收月报 | `让 AI 分析本报表` |
| 项目统计 | `分析当前项目` |
| 报花明细 | `解释该单据` |
| 待审批 | `汇总待处理风险` |

上下文传给 dts-copilot：

```json
{
  "surface": "adminweb",
  "route": "/flower/report/rentalMonthlyReport",
  "domain": "rental",
  "filters": {
    "startMonth": "202501",
    "endMonth": "202605",
    "projectId": 123
  }
}
```

### 8.3 报表结果卡

Agent 报表回答不只是一段文本，前端需要结构化卡片：

- 报表标题。
- 数据来源。
- SQL 折叠区。
- 可信度提示。
- 预览表格。
- 图表建议。
- 下钻链接。
- 操作按钮：
  - `保存草稿`
  - `创建图表`
  - `加入大屏`
  - `生成建议`

## 9. 首个落地场景: 租赁应收月报

选择租赁应收月报作为第一闭环，因为现有业务页面、后端接口、图表和明细已经稳定。

### 9.1 用户问题示例

- `生成一张 2025 年 5 月以来每月租赁回款趋势报表`
- `哪些项目最近三个月一直未出账单`
- `本月已开票未回款金额按业务经理排行`
- `帮我分析 2026 年以来租赁回款率变化`

### 9.2 推荐取数策略

| 问题类型 | 取数面 |
|---|---|
| 月度趋势 | `xycyl_ads_flowerbiz_lease_summary` 或租赁月报 mart |
| 项目明细 | adminapi `/operate/rentalMonthlyReport/chartDetail` 或 ADS 明细 |
| 业务经理排行 | ADS/DWS 汇总 |
| 未出账单项目 | adminapi L0 权威明细优先 |
| 回款异常 | ADS 汇总 + adminapi 明细复核 |

### 9.3 第一批建议类型

只做建议，不直接动作：

| 建议 | 说明 |
|---|---|
| 未出账提醒 | 发现项目连续多月未出账 |
| 已开票未回款提醒 | 发现超过阈值未回款 |
| 回款率异常 | 环比明显下降 |
| 数据质量提醒 | 缺关键关联或字段为空 |

## 10. 业务反向指导路线

### 10.1 阶段 1: 只读报表

目标：

- adminweb 能打开 Agent 报表。
- 用户自然语言生成 SQL。
- SQL 可预览。
- 可保存为 `analysis_draft`。
- 可创建图表。

验收：

- 不能产生业务写操作。
- 生成 SQL 只能读白名单 ADS/DWS。
- 报表卡显示数据来源和可信度。

### 10.2 阶段 2: 报表下钻业务对象

目标：

- 报表行能跳转 adminweb 业务页面。
- Agent 回答能附带 `projectId`、`flowerBizId`、`invoiceId` 等业务实体引用。
- 当前报表上下文可以继续追问。

验收：

- 至少支持项目、报花单、租赁月报明细三类下钻。
- 无权限用户不能看到超出数据范围的业务对象。

### 10.3 阶段 3: 业务建议

目标：

- Agent 基于报表结果生成建议卡。
- 建议有严重级别、证据和目标对象。
- 建议可关闭、采纳、转待办。

验收：

- 建议必须能追溯到报表结果。
- 低可信数据只能生成核验建议。

### 10.4 阶段 4: 受控动作

目标：

- adminapi 注册少量低风险动作。
- Agent 只能创建提案。
- 用户确认后执行。

首批低风险动作：

| 动作 | 风险 | 说明 |
|---|---|---|
| `rental.create_followup_task` | LOW | 创建跟进待办 |
| `rental.send_collection_reminder` | MEDIUM | 发送回款提醒，需确认 |
| `flowerbiz.request_data_check` | LOW | 创建数据核验任务 |
| `flowable.append_comment` | MEDIUM | 给流程追加意见，需确认 |

暂不开放：

- 审批通过。
- 审批拒绝。
- 财务入账。
- 库存出入库。
- 删除或作废单据。

### 10.5 阶段 5: 业务闭环学习

目标：

- 记录建议采纳率、执行结果、误报原因。
- 反哺语义包、指标口径和规则阈值。
- 形成“业务异常 -> 建议 -> 处理 -> 效果复盘”的闭环。

## 11. 后端实施拆分

### 11.1 dts-copilot

| 任务 | 说明 |
|---|---|
| `REPORT_DRAFT` 主链完善 | 明确新报表请求走 Agent 工作流 |
| 报表草稿自动保存 | Copilot 结果落 `analysis_draft` |
| 可信度模型 | 根据语义包和数据质量配置输出 `qualityLevel` |
| 报表建议生成 | 从查询结果提取异常、趋势、风险 |
| adminapi Bridge client | 后续调用只读上下文和动作预览 |
| 审计链路 | 记录 `agent_run_id`、SQL、建议、用户确认 |

### 11.2 adminapi

| 任务 | 说明 |
|---|---|
| Agent Bridge controller | 只暴露受控上下文和动作入口 |
| BusinessActionRegistry | 注册可被 Agent 提案的业务动作 |
| preview/execute 分离 | 所有动作先预览，后执行 |
| 权限复用 | 使用当前用户 token 和 `@PreAuthorize` |
| 审计增强 | 记录 Agent 来源、提案 ID、执行结果 |
| 业务深链元数据 | 返回 adminweb 可跳转 route 和参数 |

### 11.3 adminweb

| 任务 | 说明 |
|---|---|
| 智能报表入口 | 新增菜单或报表页子入口 |
| Copilot 嵌入 | 复用 dts-copilot chat/report 组件或 iframe |
| 报表卡组件 | 展示 SQL、图表建议、可信度、操作按钮 |
| 草稿打开 | 从 adminweb 跳转或嵌入 dts-copilot 分析草稿 |
| 业务建议卡 | 展示风险、证据和可执行建议 |
| 确认弹窗 | 高风险动作必须人工确认 |

## 12. 安全与合规

必须满足：

- Agent 所有请求绑定真实用户。
- 所有数据查询遵守用户数据范围。
- 所有 SQL 有白名单和只读校验。
- 所有业务动作需要权限校验。
- 中高风险动作必须人工确认。
- Agent 生成内容和执行结果写审计。
- 审计中保留原始问题、生成 SQL、执行人、确认人、动作结果。

需要新增审计字段：

| 字段 | 说明 |
|---|---|
| `agent_run_id` | 一次 Agent 执行 |
| `agent_session_id` | 对话会话 |
| `source_surface` | `adminweb` / `dts-copilot` |
| `user_id` | 发起用户 |
| `proposal_id` | 动作提案 |
| `confirmed_by` | 确认用户 |
| `risk_level` | 风险等级 |

## 13. 验收标准

### 13.1 Agent 报表 V1

- 用户能在 adminweb 进入智能报表。
- 用户能问 PRS 花卉租赁相关问题。
- Agent 能生成 `REPORT_DRAFT`。
- 报表 SQL 可预览并保存草稿。
- 草稿可创建图表。
- 报表卡显示数据来源。
- 低质量数据维度显示风险提示。
- SQL 不允许写操作。

### 13.2 业务建议 V1

- 租赁月报结果能生成至少 3 类建议：
  - 未出账。
  - 已开票未回款。
  - 回款率异常。
- 每条建议有证据。
- 每条建议可关闭或采纳。
- 采纳不直接改业务数据，只创建提案或待办草稿。

### 13.3 业务动作 V1

- 只开放低风险动作。
- 每个动作都有 preview。
- 执行前必须确认。
- 执行后有审计。
- 无权限用户无法执行。

## 14. 风险和应对

| 风险 | 影响 | 应对 |
|---|---|---|
| 语义包未覆盖真实字段 | SQL 生成不稳定 | 优先使用 ADS/DWS，补齐 flowerbiz 语义包 |
| 数据质量不足 | 建议误导业务 | 引入 `qualityLevel` 和低可信拦截 |
| adminapi 权限不统一 | 越权风险 | Bridge 复用 token、`@PreAuthorize` 和数据范围 |
| Agent 动作过早开放 | 误操作 | 先建议、再提案、后确认执行 |
| 报表资产污染 | 草稿太多 | 先落 `analysis_draft`，人工晋升正式资产 |
| 多系统跳转割裂 | 用户体验差 | 报表行返回业务深链元数据 |

## 15. 下一步实施建议

优先做 `Agent 报表 V1`，暂不进入业务写动作。

建议下一轮开发顺序：

1. 在 `adminweb` 增加智能报表入口，能够打开或嵌入 dts-copilot 报表 Copilot。
2. 在 `dts-copilot` 固化 `REPORT_DRAFT -> analysis_draft -> 预览 -> 创建图表` 主链。
3. 给 PRS flowerbiz 语义包补充报表可信度和推荐取数表。
4. 在报表结果卡增加 `qualityLevel`、数据来源、保存草稿、创建图表按钮。
5. 用租赁应收月报做第一条验收用例。
6. 等只读链路稳定后，再建设 `agent_report_insight` 和 `BusinessActionRegistry`。

## 16. 首轮任务清单

| 编号 | 任务 | 交付物 |
|---|---|---|
| AR-01 | 智能报表入口 | adminweb 菜单或报表页入口 |
| AR-02 | 报表 Copilot 嵌入 | 可发起自然语言报表请求 |
| AR-03 | REPORT_DRAFT 协议 | SSE/消息包含 SQL、展示类型、来源、可信度 |
| AR-04 | 草稿保存 | `analysis_draft` 自动或手动保存 |
| AR-05 | 预览与图表 | SQL 预览、创建图表 |
| AR-06 | PRS flowerbiz 语义补齐 | 推荐 ADS/DWS 表和字段说明 |
| AR-07 | 租赁月报验收 | 4 个典型问题可稳定出报表 |
| AR-08 | 数据质量提示 | 低可信字段显示提示并阻断动作 |

本设计完成后，代码实现应从 AR-01 到 AR-05 开始，先把“Agent 生成新报表”做成可用闭环。
