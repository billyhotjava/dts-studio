# 剩余固定报表映射

## 当前待补数据面模板

| 模板编码 | 页面名称 | 当前 targetObject | 当前 sourceType | 当前状态 |
|----------|----------|-------------------|-----------------|----------|
| `PROC-INTRANSIT-BOARD` | 配送记录-在途采购 | `authority.procurement.intransit_board` | `VIEW` | `placeholderReviewRequired=true` |
| `PROC-PENDING-INBOUND-LIST` | 入库管理-待入库清单 | `authority.procurement.pending_inbound_list` | `VIEW` | `placeholderReviewRequired=true` |
| `PROC-ARRIVAL-ONTIME-RATE` | 配送记录-到货及时率 | `fact.procurement.order_event` | `FACT` | `placeholderReviewRequired=true` |
| `PROC-PURCHASE-REQUEST-TODO` | 采购计划明细-待处理 | `authority.procurement.request_todo` | `VIEW` | `placeholderReviewRequired=true` |
| `FIN-PENDING-PAYMENT-APPROVAL` | 支出管理-待付款审批 | `authority.finance.pending_payment_approval` | `VIEW` | `placeholderReviewRequired=true` |
| `FIN-PENDING-RECEIPTS-DETAIL` | 财务结算列表-待收款明细 | `authority.finance.pending_receipts_detail` | `VIEW` | `placeholderReviewRequired=true` |
| `FIN-PROJECT-COLLECTION-PROGRESS` | 财务结算列表-项目回款进度 | `authority.finance.project_collection_progress` | `VIEW` | `placeholderReviewRequired=true` |
| `FIN-CUSTOMER-AR-RANK` | 财务结算汇总-客户欠款排行 | `mart.finance.customer_ar_rank_daily` | `MART` | `placeholderReviewRequired=true` |

## 文字口径修正

- `待首款明细` -> `待收款明细`
- `项目会uanjindu` -> `项目回款进度`
- `财务结算汇总g够-客户欠款排行` -> `财务结算汇总-客户欠款排行`
