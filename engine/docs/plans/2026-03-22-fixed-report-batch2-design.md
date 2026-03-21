# Sprint-16 第二批固定报表接通 — 设计文档

**日期:** 2026-03-22
**状态:** APPROVED
**目标:** 接通 7 个固定报表的真实 SQL backing，覆盖报花、配送、项目点、报销、出入库、合同、监管盘点七个业务域。

## 背景

Sprint-16 已接通 5 个固定报表（采购汇总、库存现量、库存预警、财务结算汇总、预支申请）。本批次基于现网页面盘点和数据库真实数据量，选出下一批 7 个高价值报表。

## 架构决策

- **直接查原始表** — 业务库无视图，flowerai 只读账号，状态码在 SQL 的 CASE WHEN 中翻译
- **复用现有 if-else 模式** — 每个报表一个 `executeXxx()` 方法，不重构架构
- **两个 liquibase migration** — 按业务域分组提升模板种子

## 报表清单

### 1. 报花单据汇总
- **template_code:** `BIZ-FLOWER-DOC-SUMMARY`（新增）
- **target_object:** `authority.flowerbiz.biz_document_summary`
- **核心表:** `t_flower_biz_info`（23,041 条）
- **参数:** projectId, bizType, status, startDate, endDate
- **字段:** 项目名称, 业务类型(加花/减花/换花/调花/初摆/售花/内购), 单号, 标题, 状态, 加急, 申请人, 养护人, 申请时间, 完成时间
- **状态码翻译:**
  - biz_type: 1=加花, 2=减花, 3=换花, 4=调花, 5=初摆, 10=售花, 11=内购
  - status: 1=草稿, 2=审核中, 3=已通过, 4=已完成, -1=已作废
  - urgent: 1=是, 其他=否

### 2. 配送记录
- **template_code:** `PROC-DELIVERY-RECORD`（新增）
- **target_object:** `authority.procurement.delivery_record`
- **核心表:** `t_delivery_info`（22,875 条）
- **参数:** projectId, status, startDate, endDate
- **字段:** 单号, 标题, 状态, 配送类型(市场->项目点/库房->项目点/库房->库房), 起始, 目的地, 配送人, 配送日期
- **状态码翻译:**
  - status: 1=配送中, 2=已结束
  - type: 1=市场->项目点, 2=库房->项目点, 3=库房->库房

### 3. 项目点经营汇总
- **template_code:** `PROJ-FULFILLMENT-SUMMARY`（新增）
- **target_object:** `authority.project.fulfillment_summary`
- **核心表:** `p_project` LEFT JOIN `p_contract`, `p_customer`, 子查询聚合报花/养护/租金/成本
- **参数:** projectId, status
- **字段:** 项目名称, 客户名称, 项目经理, 业务经理, 养护人员, 合同标题, 项目状态, 绿植数量, 摆位数量, 月租金合计
- **状态码翻译:**
  - status: 1=正常, 2=暂停, 3=结束

### 4. 日常报销
- **template_code:** `FIN-REIMBURSEMENT-STATUS`（已种入，提升）
- **target_object:** `authority.finance.reimbursement_list`
- **核心表:** `f_expense_account_info`（1,913 条）
- **参数:** code, status, applyUserId
- **字段:** 单号, 标题, 申请人, 报销总金额, 状态, 发票(有/无), 申请时间
- **状态码翻译:**
  - status: 1=草稿, 2=审核中, 3=待付款, 4=已付款, -1=已作废
  - invoice_status: 1=无, 2=有

### 5. 出入库记录
- **template_code:** `WH-INOUT-RECORD`（新增）
- **target_object:** `authority.inventory.inout_record`
- **核心表:** `t_warehousing_item` UNION ALL `t_ex_warehouse_item`
- **参数:** storehouseId, goodName, startDate, endDate
- **字段:** 方向(入库/出库), 单号, 库房名称, 物品名称, 数量, 单价, 日期

### 6. 合同管理
- **template_code:** `PROJ-CONTRACT-LIST`（新增）
- **target_object:** `authority.project.contract_list`
- **核心表:** `p_contract` LEFT JOIN `p_customer`（210 条）
- **参数:** customerName, status
- **字段:** 合同编号, 合同标题, 客户名称, 状态, 签订日期, 起始日期, 截止日期, 结算周期
- **状态码翻译:**
  - status: 1=执行中, 2=已结束

### 7. 监管盘点汇总
- **template_code:** `PROJ-SUPERVISION-CHECK`（新增）
- **target_object:** `authority.project.supervision_check_summary`
- **核心表:** `t_supervise_check`（需确认表名）
- **参数:** projectId, status
- **字段:** 项目名称, 盘点周期, 盘点人, 养护人, 状态, 已盘/总数(组), 已盘/总数(摆位), 完成率%

## 文件改动

| 文件 | 改动 |
|------|------|
| `DefaultFixedReportExecutionService.java` | +7 个 executeXxx() 方法 + if-else 路由扩展 |
| `AuthorityQueryService.java` | +`authority.flowerbiz.` 和 `authority.project.` 前缀支持 |
| `0047_promote_batch2_reports_part1.xml` | 新 migration：报花+配送+报销模板提升 |
| `0048_promote_batch2_reports_part2.xml` | 新 migration：项目点+出入库+合同+监管模板提升 |
| `master.xml` | 注册 0047 + 0048 |

## 不做的事

- 不重构 DefaultFixedReportExecutionService 的 if-else 架构
- 不在业务库建 VIEW
- 不改前端 FixedReportRunPage
- 不注册 NL2SQL 查询模板（后续 sprint）
- 不做 Copilot 模板匹配联动（后续 sprint）

## 验收标准

- [ ] 7 个报表都能通过 `/api/fixed-reports/{code}/run` 返回真实数据
- [ ] 每个报表的参数筛选都能正常工作
- [ ] 状态码翻译为中文显示名
- [ ] 前端 FixedReportRunPage 能展示结果表格
