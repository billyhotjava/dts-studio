# RC-05 固定报表模板模型与目录种子

**状态**: IN_PROGRESS
**目标**: 为固定报表中心、Dashboard、Screen、Report Factory 和 Copilot 提供统一模板资产。

## 模板字段

- `templateCode`
- `name`
- `domain`
- `category`
- `sourceType`
- `queryMode`
- `targetObject`
- `sqlTemplate`
- `parameterSchema`
- `metricDefinition`
- `refreshPolicy`
- `permissionPolicy`
- `presentationSchema`

## 首批目录种子

优先生成三大域，并直接贴合现网页面：

- 财务
- 采购
- 仓库

模板编码建议：

- `FIN-*`
- `PROC-*`
- `WH-*`

## 当前种子问题

当前 `0040_seed_finance_procurement_templates.xml` 已有 `16` 个种子，但存在两类问题：

1. 模板名和现网页面心智不一致
   - 例如 `FIN-AR-OVERVIEW`、`FIN-CUSTOMER-AR-RANK` 更像分析抽象名，不是现网用户正在使用的页面/报表名
2. `targetObject` 多为占位目标
   - `authority.finance.*`
   - `authority.procurement.*`
   - `authority.inventory.*`
   - `mart.finance.customer_ar_rank_daily`
   - `fact.procurement.order_event`

## 本轮已完成

1. `0040_seed_finance_procurement_templates.xml`
   - 当前 16 个 `FIN/PROC/WH` 种子已经全部显式标记 `placeholderReviewRequired=true`
2. `0041_refresh_fixed_report_page_labels.xml`
   - 已新增目录名称刷新，将固定报表目录名称贴近现网页面心智，例如：
     - `FIN-AR-OVERVIEW` -> `财务结算汇总`
     - `PROC-SUPPLIER-AMOUNT-RANK` -> `采购汇总`
     - `WH-STOCK-OVERVIEW` -> `库存现量`
3. `TemplateMatcherService`
   - 已补页面化 alias，让 `财务结算汇总 / 采购汇总 / 库存现量 / 预支申请 / 日常报销 / 开票管理` 这类现网页面语言可以直接命中固定模板
4. `FixedReportsPage / FixedReportRunPage`
   - 已显式展示“待补数据面”，避免目录和运行页误导用户
5. 现网页面锚点
   - 目录详情和运行接口已开始暴露 `legacyPageTitle / legacyPagePath`
   - `FixedReportRunPage` 可先回落到 `app.xycyl.com` 的真实业务页，而不是只停在占位运行页

## 当前仍未完成

- 还没有把当前 `FIN/PROC/WH` 模板编码整体换成完全页面化的新编码
- 还没有把 page-aligned 模板与真实 L0/L1 backing 一一接通
- 还没有把 Dashboard / Screen / Report Factory 复用接到这批 page-aligned 模板上

## 首批建议 seed 包

### 财务

- `FIN-SETTLEMENT-LIST`
- `FIN-SETTLEMENT-SUMMARY`
- `FIN-MONTHLY-REPORT`
- `FIN-INVOICE-LIST`
- `FIN-EXPENSE-LIST`
- `FIN-ADVANCE-LIST`
- `FIN-PAYMENT-LIST`

### 采购

- `PROC-PURCHASE-SUMMARY`
- `PROC-PURCHASE-DETAIL`
- `PROC-PURCHASE-PLAN-DETAIL`
- `PROC-PURCHASE-REJECT-LIST`
- `PROC-DELIVERY-RECORD`

### 仓库

- `WH-STOCK-LIST`
- `WH-INOUT-RECORD`
- `WH-INBOUND-LIST`
- `WH-OUTBOUND-LIST`
- `WH-LOSS-LIST`
- `WH-ALLOCATION-LIST`
- `WH-ALLOCATION-DETAIL`
- `WH-RETURN-LIST`

## Seed 规则

- 第一阶段 templateCode 必须能直接映射回现网页面
- 第一阶段必须明确 `L0` 或 `L1`
- 对尚未有真实 backing 的模板，必须显式标记为 `placeholderReviewRequired`
- 目录页默认优先展示已经过 backing 审计的模板，不继续把“概念模板”当成可运行模板
- 在真实 backing 缺位时，运行页至少要能回落到对应的现网页面锚点

## 成果要求

- 可被固定报表目录直接展示
- 可被 Copilot 模板优先命中
- 可被 Dashboard / Screen / Report Factory 复用
