# Sprint-16 Template Backing Audit

**更新时间**: 2026-03-21
**范围**: `0040_seed_finance_procurement_templates.xml` 当前 16 个 `FIN/PROC/WH` 固定报表种子

## 判定规则

- `BACKED`
  - `targetObject` 已有真实数据面，可由目录或运行接口直接稳定执行。
- `DECLARED_ONLY`
  - 现网页面锚点、后端归属和业务口径已知，但 `targetObject` 仍未落成真实权威视图或主题表。
- `PLACEHOLDER_ONLY`
  - 当前只存在概念目标或占位命名，既没有真实数据面，也没有已确认的页面化映射。

## 审计结论

当前 `16` 个种子里：

- `BACKED`: `0`
- `DECLARED_ONLY`: `16`
- `PLACEHOLDER_ONLY`: `0`

结论：

- 这些模板并非“毫无业务来源”，因为都能映射回现网页面或明确业务域。
- 但它们当前也都不能算真正 `BACKED`，因为 `authority.*`、`mart.finance.*`、`fact.procurement.*` 等目标对象尚未在当前环境中完成真实取数面落地。
- 因此，目录和运行页必须继续将它们视为“待补数据面”，不能展示成已可执行固定报表。

## Template Matrix

| templateCode | 当前名称 | domain | targetObject | 页面锚点 | backingStatus | nextAction |
|---|---|---|---|---|---|---|
| `FIN-AR-OVERVIEW` | 应收总览看板 | 财务 | `authority.finance.receivable_overview` | 财务结算汇总 / 财务报表（月度） | `DECLARED_ONLY` | 重命名对齐现网页面；补财务结算 L1 汇总面 |
| `FIN-CUSTOMER-AR-RANK` | 客户欠款排行 | 财务 | `mart.finance.customer_ar_rank_daily` | 财务结算汇总 | `DECLARED_ONLY` | 以 `mart_finance_settlement_daily` 或等价汇总面替代 |
| `FIN-PROJECT-COLLECTION-PROGRESS` | 项目回款进度 | 财务 | `authority.finance.project_collection_progress` | 财务结算列表 / 项目回款类页面 | `DECLARED_ONLY` | 改为项目回款清单/进度模板并补 L0 视图 |
| `FIN-PENDING-RECEIPTS-DETAIL` | 待收款明细 | 财务 | `authority.finance.pending_receipts_detail` | 财务结算列表 | `DECLARED_ONLY` | 对齐财务结算/待收明细页面并补 L0 权威查询 |
| `FIN-PENDING-PAYMENT-APPROVAL` | 待付款审批清单 | 财务 | `authority.finance.pending_payment_approval` | 支出管理 / 付款流程 | `DECLARED_ONLY` | 绑定付款申请/审批状态页并补流程态直读面 |
| `FIN-ADVANCE-REQUEST-STATUS` | 预支申请状态 | 财务 | `authority.finance.advance_request_status` | 预支申请 | `DECLARED_ONLY` | 对齐预支申请页面并补 L0 权威查询 |
| `FIN-REIMBURSEMENT-STATUS` | 报销单状态跟踪 | 财务 | `authority.finance.reimbursement_status` | 日常报销 | `DECLARED_ONLY` | 对齐日常报销页面并补 L0 权威查询 |
| `FIN-INVOICE-RECONCILIATION` | 发票收付对账 | 财务 | `authority.finance.invoice_reconciliation` | 开票管理 | `DECLARED_ONLY` | 对齐开票管理页面并补发票/收付对账取数面 |
| `PROC-PURCHASE-REQUEST-TODO` | 采购申请待办 | 采购 | `authority.procurement.request_todo` | 采购计划明细 / 采购待办类页面 | `DECLARED_ONLY` | 对齐采购计划待处理页并补 L0 直读面 |
| `PROC-ORDER-EXECUTION-PROGRESS` | 采购单执行进度 | 采购 | `authority.procurement.order_execution_progress` | 采购明细 / 配送记录 | `DECLARED_ONLY` | 对齐采购执行台账并补 L0/L1 混合面 |
| `PROC-SUPPLIER-AMOUNT-RANK` | 供应商采购金额排行 | 采购 | `fact.procurement.order_event` | 采购汇总 | `DECLARED_ONLY` | 建 `fact_procurement_order_event` 或等价主题表 |
| `PROC-ARRIVAL-ONTIME-RATE` | 到货及时率 | 采购 | `fact.procurement.order_event` | 配送记录 / 入库管理 | `DECLARED_ONLY` | 建采购到货事件事实表并沉淀时效率指标 |
| `PROC-PENDING-INBOUND-LIST` | 待入库清单 | 采购 | `authority.procurement.pending_inbound_list` | 入库管理 | `DECLARED_ONLY` | 对齐入库管理页面并补 L0 权威查询 |
| `PROC-INTRANSIT-BOARD` | 在途采购看板 | 采购 | `authority.procurement.intransit_board` | 配送记录 | `DECLARED_ONLY` | 对齐配送记录/在途采购看板并补 L0/L1 取数面 |
| `WH-STOCK-OVERVIEW` | 库存总览 | 仓库 | `authority.inventory.stock_overview` | 库存管理 | `DECLARED_ONLY` | 对齐库存现量页并补库存快照/现量直读面 |
| `WH-LOW-STOCK-ALERT` | 低库存预警 | 仓库 | `authority.inventory.low_stock_alert` | 库存管理 / 低库存提醒 | `DECLARED_ONLY` | 对齐库存预警页并补阈值预警逻辑 |

## 当前已完成整改

### 后端

- `FixedReportResource`
  - 对 `placeholderReviewRequired=true` 的模板不再返回伪 `READY`
  - 显式返回：
    - `supported=false`
    - `executionStatus=BACKING_REQUIRED`
- `ReportTemplateCatalogService`
  - 目录接口已回传 `placeholderReviewRequired`
- `0040_seed_finance_procurement_templates.xml`
  - 当前 16 个种子全部显式带 `placeholderReviewRequired=true`

### 前端

- `FixedReportsPage`
  - 占位模板显示 `待补数据面`
  - 不再显示为可直接运行
- `FixedReportRunPage`
  - 占位模板显示说明文案
  - 运行按钮禁用

## 下一步

1. 将 `0040` 的概念模板逐步替换成现网页面心智对齐的模板编码与名称
2. 为财务、采购、仓库首批模板补齐真实 L0/L1 backing
3. 在 Copilot 模板优先链路里继续淘汰仍未接通的数据面模板，避免误命中
