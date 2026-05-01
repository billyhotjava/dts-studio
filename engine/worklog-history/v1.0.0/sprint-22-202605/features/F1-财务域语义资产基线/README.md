# F1: 财务域语义资产基线

**优先级**: P0
**状态**: READY

## 目标

把 sprint-19 ~ sprint-21 已落地的 8+ 张财务 authority / mart 视图沉淀成 LLM 可识别的语义资产，覆盖应收 / 待付 / 回款 / 报销 / 发票 / 预支 / 月度结算 7 个核心场景。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 财务 authority/mart 面盘点与口径锁定 | P0 | READY | sprint-21 完成 |
| T02 | finance.json 语义包落地 | P0 | READY | T01 |
| T03 | 财务字段 display/semantic 语义补齐 | P0 | READY | T02 |
| T04 | 财务枚举字典扩充 | P0 | READY | T01 |

## 完成标准

- [ ] 财务域 9+ 个业务对象（settlement / receivable / payment / advance / reimbursement / invoice / collection / ar_rank / ar_overview）有明确 authority 视图归属
- [ ] `finance.json` 通过 `SemanticPackService` 加载，且不与 `procurement.json` / `project-fulfillment.json` 字段冲突
- [ ] 财务字段同义词（应收/待收/欠款/未收/已收/已回款/已付/实付/账期/月份）显式入字段元数据
- [ ] `BizEnumDictionary` 覆盖 6 类财务状态枚举，问答中状态显示中文标签
