# Sprint-21: 剩余固定报表数据面收口

**时间**: 2026-03
**状态**: READY
**目标**: 把剩余 8 张 `placeholderReviewRequired=true` 的固定报表补齐真实数据面，完成从占位模板到可执行固定报表的收口。

## 背景

`sprint-16` 完成了固定报表目录、模板和一批 authority SQL 落地，但仍有 8 张模板停留在“待补数据面”状态。  
这些报表集中在两条业务线：

- 采购 / 配送 / 入库
- 财务待收 / 待付 / 回款 / 客户欠款

目前系统中的实际待收口模板为：

- `PROC-INTRANSIT-BOARD` 配送记录-在途采购
- `PROC-PENDING-INBOUND-LIST` 入库管理-待入库清单
- `PROC-ARRIVAL-ONTIME-RATE` 配送记录-到货及时率
- `PROC-PURCHASE-REQUEST-TODO` 采购计划明细-待处理
- `FIN-PENDING-PAYMENT-APPROVAL` 支出管理-待付款审批
- `FIN-PENDING-RECEIPTS-DETAIL` 财务结算列表-待收款明细
- `FIN-PROJECT-COLLECTION-PROGRESS` 财务结算列表-项目回款进度
- `FIN-CUSTOMER-AR-RANK` 财务结算汇总-客户欠款排行

## Feature 列表

| ID | Feature | Task 数 | 状态 |
|----|---------|---------|------|
| F1 | 剩余固定报表数据面收口 | 5 | READY |

## 完成标准

- [ ] 8 张剩余固定报表都有明确的 `authority / mart / fact` 落地数据面
- [ ] 对应模板从 `placeholderReviewRequired=true` 切到可执行
- [ ] 采购线与财务线都至少有一条远程环境回归样例
- [ ] `ai.yuzhicloud.com` 上固定报表执行不再出现“待补数据面”
