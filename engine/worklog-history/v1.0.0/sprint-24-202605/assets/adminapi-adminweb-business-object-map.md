# Adminapi/Adminweb 业务对象资产图

**更新日期**: 2026-05-21  
**用途**: 支撑 Sprint-24 “业务对象问答器”，避免 Agent 只从 ODS 表名推断业务含义。

## Review 范围

- `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base`
- `adminweb/src/api/flower`
- `adminweb/src/views/flower`
- `adminweb/src/views/expense`

PRS 租赁系统的对象不是单一采购域。当前最重的业务域应按下面顺序进入 Copilot 资产目录：

1. 报花：报花单据、加花、减花、换花、调花、摆位、实摆绿植、回收、坏账。
2. 采购：采购计划、采购汇总、采购明细、配送记录、自采、供应商和付款方式。
3. 项目：项目点、合同、客户、摆位、项目绿植、养护记录。
4. 财务/运营：结算、费用、预支、支出、银行流水、凭证、开票、收款、对账。
5. 仓库：库存、入库、出库、调拨、报损、退货。

## 首批业务对象

| 对象编码 | 业务域 | 页面路径 | adminweb 证据 | adminapi 只读边界 | Copilot 用法 |
|---|---|---|---|---|---|
| `prs.flowerbiz.biz_order` | 报花 | 报花管理 > 报花单据 | `adminweb/src/views/flower/flowerbiz/biz` | `/rs-flowers-base/flower/biz/listPage` | 单据状态、业务类型、项目点、发起/完成时效画像 |
| `prs.flowerbiz.change_order` | 报花 | 报花管理 > 换花/调花 | `adminweb/src/views/flower/flowerbiz/change`, `transfer` | `/rs-flowers-base/changeInfo/listPage`, `/rs-flowers-base/flower/transfer/listPage` | 换花类型、调整类型、未完成换花、处理时效 |
| `prs.flowerbiz.position_placement` | 报花/项目 | 项目点管理 > 摆位/实摆绿植 | `adminweb/src/views/flower/project/green`, `position` | `/rs-flowers-base/project/position/listPage`, `/rs-flowers-base/project/green/listPage` | 摆位、实摆绿植、养护人、绿植规格和状态画像 |
| `prs.procurement.plan_purchase` | 采购 | 采购管理 > 采购计划明细 | `adminweb/src/views/flower/purchase/plan` | `/rs-flowers-base/self/planPurchase/listPlanPurchaseItemPage` | 计划状态、采购人任务、项目点需求 |
| `prs.procurement.purchase_order` | 采购 | 采购管理 > 采购汇总/采购明细 | `adminweb/src/views/flower/purchase/purchase`, `purchaseitem`, `self` | `/rs-flowers-base/self/purchaseInfo/listPage`, `/rs-flowers-base/self/info/listPage` | 采购金额、供应商、付款方式、采购人画像 |
| `prs.procurement.delivery_record` | 采购 | 采购管理 > 配送记录 | `adminweb/src/views/flower/purchase/delivery` | `/rs-flowers-base/self/deliveryInfo/listDeliveryInfoPage` | 配送状态、类型、接收人、配送时效画像 |
| `prs.project.project_site` | 项目 | 项目点管理 > 项目点 | `adminweb/src/views/flower/project/project` | `/rs-flowers-base/project/project/listPage` | 项目状态、项目经理、监管、业务经理和养护负责人画像 |
| `prs.project.contract` | 项目 | 项目点管理 > 合同管理 | `adminweb/src/views/flower/project/contract` | `/rs-flowers-base/project/contract/listPage` | 合同状态、到期、客户和结算周期画像 |
| `prs.finance.settlement` | 财务 | 财务管理 > 财务结算 | `adminweb/src/views/flower/finance/settlement` | `/rs-flowers-base/finace/settlement/listPage` | 结算状态、申请人、结算人、结算明细画像 |
| `prs.finance.expense` | 财务/运营 | 运营侧 > 报销/预支/支出 | `adminweb/src/views/expense`, `adminweb/src/views/flower/finance/expense` | `/rs-flowers-base/finace/expense/listPage`, `/rs-flowers-base/advance/listPage` | 费用状态、费用类型、申请人和付款时效 |
| `prs.finance.bank_statement` | 财务 | 财务管理 > 银行流水 | `adminweb/src/views/flower/finance/bankStatement` | `/rs-flowers-base/finace/bankStatement/listPage` | 未核对流水、收入支出方向、对方户名、凭证关联 |
| `prs.finance.voucher` | 财务 | 财务管理 > 凭证管理 | `adminweb/src/views/flower/finance/voucher` | `/rs-flowers-base/finace/voucher/list` | 凭证状态、会计期间、借贷金额校验 |
| `prs.finance.invoice_collection` | 财务/运营 | 运营管理 > 开票/收款 | `adminweb/src/views/flower/operate/invoice`, `collection` | `/rs-flowers-base/operate/invoiceInfo/listInvoiceInfoPage`, `/rs-flowers-base/operate/collection/listCollectionRecordPage` | 开票状态、收款状态、项目收款单据追溯 |
| `prs.warehouse.stock_movement` | 仓库 | 仓库管理 > 库存/出入库 | `adminweb/src/views/flower/store` | `/rs-flowers-base/store/info/listPage`, `/rs-flowers-base/store/ware/listPage`, `/rs-flowers-base/store/exWarehouseInfo/listPage` | 库存状态、入库、出库、调拨、报损和退货画像 |

## 路由原则

- 经营类问题优先找 DTS 已计算好的 ADS/DWS，例如项目经营 TOP、租赁收入趋势、客户贡献、回款排行。
- 用户问单据状态、字段分布、未核对、未完成、接收人、负责人、页面字段含义时，优先命中业务对象目录。
- 业务对象只读，默认回答字段画像和结构化摘要；需要具体明细时，先查 schema，再走 DTS ODS 或 adminapi 只读桥。
- 业务写操作不在本 sprint 直接执行，只生成动作提案和待确认参数。

## 后续扩展

- F2 候选 ADS 生产器应从这些业务对象选择 grain、维度、指标和源表，而不是从裸 ODS 表名开始。
- F3 字段画像应补齐状态字典、默认时间字段、业务页面深链和字段别名。
- 后续新增业务域时先补本资产图，再进入 `BusinessObjectCatalogService`。
