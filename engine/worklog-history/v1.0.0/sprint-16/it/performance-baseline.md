# Sprint-16 固定报表 / 模板优先性能基线

采样时间：`2026-03-21`
环境：本地 `webapp(3003) -> analytics(8092) -> ai(8091)` 开发链路
账号：本地 analytics `admin / Devops123@`
数据源：`7`（园林业务库）

## 当前基线

- `GET /api/report-catalog?limit=3`
  - 结果：`200`
  - 用时：`0.016s`
- `POST /api/fixed-reports/FIN-ADVANCE-REQUEST-STATUS/run`
  - 结果：`200`
  - 状态：`BACKING_REQUIRED`
  - 用时：`0.026s`
- `POST /api/copilot/chat/send` with `财务报表`
  - 结果：固定报表候选直答
  - 用时：`0.033s`
- `POST /api/copilot/chat/send` with `采购汇总`
  - 结果：命中 `PROC-SUPPLIER-AMOUNT-RANK`
  - 用时：`0.034s`

## 说明

- 这组基线验证的是“模板优先是否先命中目录/固定报表”，不是最终真实业务 SQL 的执行耗时。
- 当前 `FIN/PROC/WH` 模板大多仍然是 `placeholderReviewRequired=true` 的占位模板，因此固定报表运行态更多反映“目录、路由、backing 状态”。
- 当真实 `L0/L1` backing 接通后，需要重新记录：
  - 固定报表真实取数耗时
  - 模板命中后页面打开耗时
  - Copilot 候选直答与固定报表跳转的端到端耗时
