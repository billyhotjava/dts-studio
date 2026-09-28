# Sprint-21 IT

本目录用于存放“剩余固定报表数据面收口”的远程环境回归与验收记录。

## 目标报表

- `PROC-INTRANSIT-BOARD`
- `PROC-PENDING-INBOUND-LIST`
- `PROC-ARRIVAL-ONTIME-RATE`
- `PROC-PURCHASE-REQUEST-TODO`
- `FIN-PENDING-PAYMENT-APPROVAL`
- `FIN-PENDING-RECEIPTS-DETAIL`
- `FIN-PROJECT-COLLECTION-PROGRESS`
- `FIN-CUSTOMER-AR-RANK`

## 验收要求

每张报表至少记录：

- 环境：`ai.yuzhicloud.com`
- 数据源
- 入参
- 返回状态码
- 是否仍为 `placeholderReviewRequired=true`
- 核心结果截图或日志

## 输出要求

- 采购线与财务线各至少保留一条完整远程回归链
- 若口径与旧系统不一致，必须记录来源页面和差异说明
