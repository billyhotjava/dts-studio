# XYCYL 固定报表目录与业务库对照 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 基于现网业务页面、`adminweb/adminapi/app` 三端源码和 `rs_cloud_flower` 业务库，建立固定报表目录、受控取数面和 Copilot 模板优先路由的实施基线。

**Architecture:** 先完成页面-后端-数据库-模板四方对照，再按财务、采购、仓库优先顺序建设固定报表模板和受控取数面。已知报表走固定模板快路径，未知报表再进入 Copilot 探索链，移动端只承接必要的轻量查询与执行协同。

**Tech Stack:** Spring Boot 3.4、React/Vite、PostgreSQL、MySQL、Liquibase、现有 Copilot/Analytics 模块

---

### Task 1: 固化三端页面盘点与数据库对照

**Files:**
- Create: `worklog/v1.0.0/sprint-16/tasks/RC-01-visible-page-inventory-and-report-like-pages.md`
- Create: `worklog/v1.0.0/sprint-16/tasks/RC-02-business-db-scan-and-page-mapping.md`
- Modify: `worklog/v1.0.0/sprint-16/README.md`

**Step 1: 固化 adminweb / app 页面盘点结论**

- 记录 67 个可见路由、60 个报表型页面
- 按采购、仓库、财务、运营、报花、任务、项目、准备池分组

**Step 2: 固化 adminapi 模块归属**

- 记录后端控制器边界
- 确认主要业务域的控制器和服务归属

**Step 3: 固化业务库扫描结论**

- 记录 `163` 张物理表、`0` 视图
- 按财务、采购/仓库、报花/任务、项目分域归类

**Step 4: 形成页面 -> API -> 控制器 -> 表族映射**

- 对高价值页面记录：
  - 页面路径
  - API 模块
  - 控制器归属
  - 主要表族

### Task 2: 产出固定报表候选 Top 30

**Files:**
- Create: `worklog/v1.0.0/sprint-16/tasks/RC-03-top30-fixed-report-candidates.md`
- Modify: `worklog/v1.0.0/sprint-16/README.md`

**Step 1: 财务域 Top 报表**

- 财务结算列表/汇总
- 财务报表
- 开票管理
- 日常报销
- 预支申请
- 支出管理

**Step 2: 采购/仓库域 Top 报表**

- 采购汇总
- 采购明细
- 采购计划明细
- 采购驳回
- 配送记录
- 库存现量/出入库记录/调拨/退货/报损

**Step 3: 报花/任务/项目样板报表**

- 报花明细
- 任务中心
- 项目点汇总
- 实摆物品统计
- 实摆租金统计

### Task 3: 设计受控取数面

**Files:**
- Create: `worklog/v1.0.0/sprint-16/tasks/RC-04-reporting-data-surface-strategy.md`
- Modify: `docs/plans/2026-03-21-xycyl-report-catalog-and-db-grounding-design.md`

**Step 1: 识别必须实时直读的页面**

- 状态类
- 待办类
- 明细类

**Step 2: 识别适合准实时主题层的页面**

- 趋势类
- 排行类
- 汇总类

**Step 3: 输出首批 L0/L1 资产**

- 财务：结算/开票/费用/预支
- 采购：采购/配送/计划
- 仓库：库存/出入库/调拨/退货/报损

### Task 4: 固定报表模板模型与目录种子

**Files:**
- Modify: `dts-copilot-analytics/src/main/java/**`（模板目录模型）
- Modify: `dts-copilot-webapp/src/pages/fixed-reports/**`
- Create: `worklog/v1.0.0/sprint-16/tasks/RC-05-fixed-report-template-model-and-catalog-seed.md`

**Step 1: 为模板定义统一字段**

- `templateCode`
- `domain`
- `sourceType`
- `queryMode`
- `parameterSchema`
- `refreshPolicy`
- `presentationSchema`

**Step 2: 为 Top 30 生成首批目录种子**

- 财务优先
- 采购/仓库次优先

### Task 5: 模板优先路由接入 Copilot 主链

**Files:**
- Modify: `dts-copilot-ai/src/main/java/**`
- Modify: `dts-copilot-webapp/src/components/copilot/**`
- Create: `worklog/v1.0.0/sprint-16/tasks/RC-06-template-first-routing.md`

**Step 1: 模板优先匹配**

- 命中固定报表模板时不再走 NL2SQL

**Step 2: 未命中才进入探索**

- 限制在对应业务域的受控取数面

### Task 6: 多入口复用

**Files:**
- Modify: `dts-copilot-webapp/src/pages/fixed-reports/**`
- Modify: `dts-copilot-webapp/src/pages/screens/**`
- Modify: `dts-copilot-webapp/src/pages/dashboard/**`
- Create: `worklog/v1.0.0/sprint-16/tasks/RC-07-multi-surface-reuse.md`

**Step 1: 固定报表目录**

- 可浏览
- 可筛选
- 可直达

**Step 2: 与 dashboard / screen / report factory 复用同一模板**

### Task 7: IT 基线与真实业务验证

**Files:**
- Create: `worklog/v1.0.0/sprint-16/it/README.md`
- Create: `worklog/v1.0.0/sprint-16/tasks/RC-08-it-validation-and-performance-baseline.md`

**Step 1: 建立页面 -> 模板 -> SQL -> 数据结果的验收链**

**Step 2: 为固定报表和 Copilot 探索分别定义响应时间基线**

Plan complete and saved to `docs/plans/2026-03-21-xycyl-report-catalog-and-db-grounding-plan.md`. Two execution options:

1. `Subagent-Driven (this session)`：我继续在当前会话里按 task 往下拆和实现
2. `Parallel Session (separate)`：新会话使用 `executing-plans` 按计划执行
