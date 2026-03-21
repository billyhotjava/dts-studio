# RC-06 模板优先 Copilot 路由

**状态**: IN_PROGRESS
**目标**: 让 Copilot 在面对已知高频报表问题时优先命中固定模板，而不是继续生成临时 SQL。

## 路由规则

1. 先识别业务域
2. 在业务域内检索固定报表模板
3. 高置信度直接命中模板
4. 中置信度返回候选模板
5. 未命中才进入探索模式

## 探索模式边界

- 只能进入对应业务域的受控取数面
- 不能重新放开全库自由 SQL

## 验收

- 财务、采购、仓库首批模板问题优先命中固定模板
- 普通未知问题仍可进入 Copilot 探索链

## 本轮已完成

- `TemplateMatcherService`
  - 已支持 `财务结算汇总 / 采购汇总 / 库存现量 / 预支申请 / 日常报销 / 开票管理 / 入库管理待入库清单` 等页面语言问法
- `getSuggestedQuestions()`
  - 欢迎建议已优先混入 fixed report page phrase
  - Copilot 首屏建议不再完全停留在旧 `TPL-*` 模板问法
- 中置信度候选返回
  - `AssetBackedPlannerPolicy`
    - 泛问 `财务报表 / 采购报表 / 仓库报表` 时，已先返回 2-3 个固定报表候选
  - `TemplateMatcherService`
    - 已支持按业务域返回页面化固定报表候选列表
- 聊天区候选入口
  - `CopilotChat`
    - 已可把 `FIXED_REPORT_CANDIDATES` 响应渲染为可点击的固定报表候选链接
- 已接通模板的直达执行
  - 当 Copilot 命中 `PROC-SUPPLIER-AMOUNT-RANK` 时，后续固定报表页不再只展示计划元数据
  - `FixedReportResource` 已可直接执行 `authority.procurement.purchase_summary`
  - `FixedReportRunPage` 已可显示 `采购汇总` 的结果预览
- 已接通模板的第二个仓库域样板
  - 当 Copilot 命中 `WH-STOCK-OVERVIEW` 时，固定报表页已可直接执行 `authority.inventory.stock_overview`
  - `FixedReportRunPage` 已可显示 `库存现量` 的结果预览
- 已接通模板的第三个财务域样板
  - 当 Copilot 命中 `FIN-AR-OVERVIEW` 时，固定报表页已可直接执行 `authority.finance.settlement_summary`
  - `FixedReportRunPage` 已可显示 `财务结算汇总` 的结果预览

## 当前仍未完成

- 还没有把除 `财务结算汇总 / 采购汇总 / 库存现量` 外的固定报表目录命中结果与真实 backing 执行结果彻底接通
