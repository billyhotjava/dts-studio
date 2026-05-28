# PRS 现有应用优势与 Agent BI 业务域资产图

**日期**: 2026-05-20  
**用途**: 作为 Sprint-23 的业务资产输入，把已有 PRS 应用能力转成 Agent 生成报表时可使用的语义边界、取数面和验收域。

## 1. 现有系统优势总结

### 1.1 业务流程已经沉淀在系统里

`adminapi/rs-modules/rs-flowers-base` 下的业务模块不是简单 CRUD，而是围绕花卉租摆业务形成了完整链路：

```text
项目 / 客户 / 合同 / 摆位
  -> 报花 / 加摆 / 撤摆 / 换花 / 调拨 / 坏账 / 销售 / 赠花
  -> 采购 / 配送 / 入库 / 出库 / 库存 / 回收
  -> 月账 / 开票 / 回款 / 费用 / 凭证
  -> 养护任务 / 巡检 / 监督 / 问题整改
  -> 流程审批 / 操作日志
```

这对 Agent BI 的价值是：自然语言问题可以落到业务动作和业务对象上，而不是只落到表名上。

### 1.2 前端菜单已经表达业务语言

`adminweb/src/views/flower` 目录已经按业务人员理解的方式组织：

- `flowerbiz/add`、`cut`、`transfer`、`baddebt`：报花业务动作。
- `project/project`、`project/customer`、`project/green`、`position`：项目、客户、摆位和实物。
- `purchase/*`、`store/*`：采购与库存。
- `operate/monthAccount`、`operate/invoice`、`operate/collection`：租赁应收、开票、回款。
- `finance/*`：费用、付款、银行流水、凭证。
- `tasknew/*`、`supervise/*`：任务、巡检、现场执行。
- `report/*`、`fanruan/*`：固定报表入口。

这些路径可以直接反向生成 Agent 的业务词典、同义词和报表意图。

### 1.3 后端接口天然提供 L0 权威边界

`adminapi` 的 controller/service 已经承载权限、状态流转、业务校验、导出和操作日志。Agent 不应绕过这些边界。

第一阶段的使用方式：

- 报表汇总、趋势、排行：优先走 dbt ADS/DWS。
- 明细、当前状态、跳转处理：走 adminapi 只读 Bridge 或现有只读接口。
- 写动作：只生成建议和草稿，后续通过注册化 `BusinessAction` 由用户确认。

### 1.4 已有报表资产可做快路径

系统已有三类报表资产：

| 资产 | 位置 | Agent BI 价值 |
|---|---|---|
| 租赁月报 | `adminweb/src/views/flower/report/rentalMonthlyReport`、`adminapi/.../RentalMonthlyReportController.java` | 高频固定报表，适合固定报表快路径 |
| 帆软/JimuReport | `adminweb/src/views/report`、`adminapi/.../fanruan`、`.../jmreport` | 已发布报表入口，适合 L2 资产匹配 |
| PRS 报花大屏 | `worklog/prs/v1/screens/*.json` | 可作为“自然语言打开/生成类似大屏”的模板资产 |

### 1.5 dts-copilot 已具备报表闭环底座

当前 `dts-copilot` 已具备：

- 语义包加载：`project-fulfillment.json`、`field-operations.json`、`procurement.json`、`flowerbiz.json`。
- Planner-first 对话编排：可区分固定报表、Agent 工作流和报表草稿。
- `REPORT_DRAFT` 响应类型。
- `analysis_draft` 表和草稿保存/生成图表链路。
- screen/card 可视化和 AI 大屏生成基础。

Sprint-23 的重点不是从 0 做 BI，而是把这些能力接到 PRS 真实业务域上。

## 2. 业务域优先级

| 优先级 | 业务域 | 现有优势 | 首批 Agent BI 报表 | 首选取数面 | 数据质量 | 本 sprint 处理 |
|---|---|---|---|---|---|---|
| P0 | 租赁/报花 | dbt 模型、12 张大屏、已有报花语义包、业务链完整 | 租赁执行、报花变更、应收坏账、回收、养护工作量、待审批 | L1 dbt ADS/DWS + L2 screen | MEDIUM | 作为首个验收域 |
| P0 | 项目/客户/合同/摆位 | adminweb/adminapi 模块完整，是所有报表公共维度 | 项目价值排行、客户贡献、合同到期、摆位实租/空置 | L0 adminapi + L1 DWS | MEDIUM | 作为公共维度资产 |
| P1 | 租赁应收/开票/回款 | 现有租赁月报和 operate 模块成熟 | 月应收、待开票、待回款、逾期风险、回款趋势 | L2 固定报表 + L0/L1 | MEDIUM | 接入报表目录 |
| P1 | 采购/库存/回收 | 采购、配送、入库、出库、库存目录完整 | 采购需求、采购金额、库存周转、回收去向 | L0 adminapi + 后续 dbt | MEDIUM/LOW | 暂列下一批 |
| P2 | 任务/养护/巡检 | app/adminweb 双端执行数据，适合现场运营分析 | 养护人工作量、巡检问题、整改闭环、任务延期 | L0 adminapi + 后续 dbt | MEDIUM | 做问句储备 |
| P2 | 流程/日志/审计 | Flowable 和操作日志可追踪业务过程 | 待办积压、审批耗时、异常操作、业务过程审计 | L0 adminapi | HIGH/MEDIUM | 支撑反向指导 |

## 3. Agent BI 路由矩阵

| 用户自然语言 | Planner 目标 | 数据面 | 输出 |
|---|---|---|---|
| “打开报花总览大屏” | `FIXED_REPORT` | L2 screen JSON | 已有大屏链接 |
| “生成今年每个月租赁应收趋势” | `REPORT_DRAFT` | L1 `xycyl_ads_*` / `xycyl_dws_*` | SQL + 折线图建议 |
| “列出本月坏账最高的客户和项目” | `REPORT_DRAFT` | L1 ads + 项目/客户维度 | 表格 + 柱图建议 |
| “这个项目还有哪些待确认账单” | `BUSINESS_DETAIL` | L0 adminapi 只读 | 明细列表 + 业务页深链 |
| “哪些客户最近回款异常，需要跟进” | `BUSINESS_INSIGHT` | L1/L0 混合 | 建议列表 + 数据质量提示 |
| “帮我发起催收任务” | `ACTION_PROPOSAL` | 后续 BusinessAction | 仅生成提案，等待确认 |

## 4. Sprint-23 首批自然语言问题

### 租赁/报花域

- “从 2025 年 5 月到现在，租赁收入按月趋势怎么样？”
- “哪些项目本月加摆金额最高？”
- “哪些客户坏账金额最高，涉及哪些项目？”
- “本月待审批的报花单按业务类型分布？”
- “各养护人本月处理了多少报花和回收？”
- “回收去向按项目统计一下。”
- “打开租赁执行大屏。”
- “基于报花总览生成一张项目经理周报。”

### 项目/客户公共维度

- “客户贡献排名前 20 是哪些？”
- “哪些项目租赁金额高但回款慢？”
- “合同快到期的项目有哪些？”
- “项目对应的摆位和当前实租状态如何？”

### 财务/运营域

- “本月待开票和待回款金额分别是多少？”
- “银行流水和回款记录有没有未匹配项？”
- “各项目费用成本占比最高的是什么？”

### 采购/库存域

- “近 30 天采购金额按采购人统计。”
- “库存周转慢的品类有哪些？”
- “哪些采购需求来自加摆，哪些来自补货？”

## 5. 第一阶段边界

- 不直接连接裸 `ptr_mysql` 全库生成自由 SQL。
- 不绕过 `adminapi` 的权限、数据范围和操作日志。
- 不自动执行业务写动作。
- 不把数据质量为 `LOW` 的报表自动晋升为正式大屏。
- 不替换现有固定报表，优先把固定报表变成 Agent 的快路径资产。

## 6. 验收口径

| 类别 | 验收方式 |
|---|---|
| 业务域资产 | 每个域至少有 5 条高频问句、首选数据面、质量等级和报表类型 |
| Agent 路由 | 同一问句能稳定落到 `FIXED_REPORT` / `REPORT_DRAFT` / `BUSINESS_DETAIL` / `BUSINESS_INSIGHT` |
| 报表草稿 | 生成 SQL、说明、展示建议、质量等级、保存入口 |
| 可视化 | 至少 8 条租赁/报花问句可生成图表，至少 3 条可加入大屏 |
| 安全 | 所有动态 SQL 只允许 SELECT，并限制白名单数据集 |
| 业务可信 | MEDIUM/LOW 质量报表必须展示口径和质量提示 |
