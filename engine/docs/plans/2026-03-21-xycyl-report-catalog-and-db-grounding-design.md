# XYCYL 固定报表目录与业务库对照设计

**日期**: 2026-03-21
**范围**: `adminweb/adminapi/app` 三端源码盘点、`rs_cloud_flower` 业务库扫描、`dts-copilot` 固定报表快路径设计

## 背景

在 `https://app.xycyl.com/#/login` 使用只读账号实际登录后，结合已登录态菜单接口与 `adminweb` 页面源码盘点，确认当前现网系统并不是“缺少报表”，而是：

- 很多报表能力散落在业务菜单中，不集中在 `统计报表`
- 同一业务域内已有大量汇总、明细、排行、状态台账页
- 现有 Copilot 如果继续对所有问题都走“现场找表 + 临时生成 SQL”，客户演示时会慢且不稳

同时，直接扫描测试业务库 `rs_cloud_flower` 后确认：

- 业务库存在 `163` 张物理表
- 业务库存在 `0` 张视图
- 采购、仓库、财务、报花、任务、项目相关表都很完整，但没有现成的“报表专用视图层”

这意味着后续设计不能假设“业务库里已经有成熟报表面”。固定报表快路径必须补一层受控取数面，再叠加模板目录和多端复用。

补扫 `adminweb`、`adminapi`、`app` 三端源码后又确认：

- `adminweb` 已经有大量报表型页面，是固定报表模板的首要来源
- `adminapi` 的业务域控制器边界清晰，可直接作为固定报表取数面的后端归属
- `app` 主要覆盖现场执行，不是财务固定报表的主入口

## 现状核实

### 页面侧

通过 `system/menu/getRouters` 和 `adminweb/src/views/**` 盘点，当前账号可见 `67` 个路由，其中至少 `60` 个属于典型报表型页面：

- 有筛选区
- 有表格列或汇总指标
- 支持明细台账、状态统计、时间筛选、导出或下钻

报表能力最集中的一级业务域：

1. `采购管理`
2. `仓库管理`
3. `财务管理`
4. `运营管理`
5. `报花管理`
6. `项目点管理`
7. `任务中心`
8. `准备池`

典型页面示例：

- `采购管理 -> 采购汇总`
- `采购管理 -> 采购明细`
- `仓库管理 -> 库存管理-库存`
- `仓库管理 -> 库存管理-出入库记录`
- `财务管理 -> 财务报表`
- `运营管理 -> 财务结算`
- `运营管理 -> 开票管理`
- `报花管理 -> 报花明细`
- `任务中心 -> 任务中心`
- `项目点管理 -> 项目点汇总`

### 源码侧

#### adminweb

- `flower/purchase` `13`
- `flower/store` `26`
- `flower/finance` `22`
- `flower/operate` `20`
- `flower/project` `46`
- `flower/task` `51`
- `plant` `23`

#### adminapi

- `project/controller` `19`
- `flowerbiz/controller` `18`
- `purchase/controller` `6`
- `task/controller` `5`
- `tasknew/controller` `9`
- `operate/controller` `7`

#### app

- `pages/task` `100`
- `pages/flowers` `88`
- `pages/purchase` `20`
- `pages/warehousing` `13`
- `pages/pendulum` `17`

结论：固定报表第一阶段必须以 PC 为主，移动端以轻量查询和执行协同为辅。

### 数据库侧

扫描 `rs_cloud_flower` 后确认的核心表族：

- 采购/仓库：
  - `i_purchase_info`
  - `i_purchase_item`
  - `t_plan_purchase_info`
  - `t_plan_purchase_item`
  - `t_delivery_info`
  - `t_delivery_item`
  - `t_warehousing_info`
  - `t_warehousing_item`
  - `t_allocation`
  - `t_allocation_item`
  - `t_back_good_info`
  - `t_back_good_item`
  - `s_stock_info`
  - `s_stock_item`
  - `s_storehouse_info`

- 财务：
  - `f_settlement`
  - `f_settlement_item`
  - `f_advance_info`
  - `f_expense_account_info`
  - `f_expense_account_item`
  - `f_pay_record`
  - `f_bank_statement`
  - `a_invoice_info`
  - `a_invoice_item`
  - `a_green_accounting`
  - `a_month_accounting`

- 报花/任务/项目：
  - `t_flower_biz_info`
  - `t_flower_biz_item`
  - `t_daily_task_info`
  - `t_daily_task_item`
  - `p_project`
  - `p_project_green`
  - `p_project_green_item`
  - `p_position`
  - `p_problem_record`

体量最大的业务表也直接落在这些域上：

- `a_back_green`
- `t_supervise_check_green`
- `t_flower_biz_info`
- `t_flower_biz_item`
- `s_stock_item`
- `t_plan_purchase_item`

## 核心判断

### 判断 1：现网系统“已有大量已知报表雏形”

客户说“系统没有现场报表”，更准确的说法是：

- 报表型能力已经在业务页里存在
- 但没有被产品化成统一的固定报表目录
- 也没有被沉淀成统一模板资产供 dashboard / screen / copilot 复用

### 判断 2：不能继续把“未知报表”和“已知报表”都交给 NL2SQL

对已知高频报表继续走实时 NL2SQL，会同时带来：

- 响应慢
- 指标口径漂移
- SQL 生成不稳定
- 演示不可控

因此：

- 已知报表必须走模板优先快路径
- 未知报表才由 Copilot 探索兜底

### 判断 3：固定报表快路径必须建立受控取数面

因为业务库 `0` 视图，当前不能假设“直接命中现成业务视图”。需要在 `dts-copilot` 中建立三类受控取数面：

1. `L0 业务权威直读`
   面向实时状态、待办、明细页
2. `L1 准实时主题层`
   面向趋势、排行、汇总、跨实体聚合
3. `L2 固定报表模板层`
   面向目录、卡片、大屏、报告工厂、Copilot 模板命中

## 推荐方案

推荐采用“固定报表中心 + Copilot 探索兜底”的双引擎结构。

### 已知报表快路径

路径：

`用户问题/点击报表目录 -> 模板命中 -> 固定 SQL/视图/主题表 -> 结构化结果 -> 多端展示`

特点：

- 不依赖 LLM 现场生成 SQL
- 口径固定
- 可缓存
- 可沉淀为统一 `templateCode`

### 未知报表探索路径

路径：

`用户问题 -> 主题域识别 -> 模板未命中 -> 受控表集合/主题层 -> NL2SQL -> 结果返回 -> 候选模板沉淀`

特点：

- 只处理真正未知的问题
- 使用受控表族和模板示例
- 高价值问题可回灌为固定报表模板

## v1 业务优先级

结合现网菜单、客户反馈和数据库结构，建议第一阶段按这个顺序推进：

1. `财务相关`
2. `采购管理`
3. `仓库管理`
4. `报花管理`
5. `任务中心`
6. `项目点管理 / 运营管理`

补充约束：

- 第一阶段固定报表主入口优先落在 PC 端
- 移动端优先承接轻量查询、任务协同和现场核对

## v1 固定报表候选

### 第一批必须固化

#### 财务

- 财务结算列表
- 财务结算汇总
- 财务报表（月度）
- 开票管理
- 日常报销
- 预支申请
- 支出管理

#### 采购

- 采购汇总
- 采购明细
- 采购计划明细
- 采购驳回
- 配送记录

#### 仓库

- 库存现量
- 出入库记录
- 入库管理
- 出库管理
- 报损管理
- 调拨管理
- 调拨明细
- 退货管理

### 第二批样板

- 报花单据管理
- 报花明细
- 任务中心
- 我的任务
- 项目点汇总
- 实摆物品统计
- 实摆租金统计
- 准备池-备货汇总
- 准备池-项目汇总
- 准备池-采购计划汇总

## 技术落地方向

### 数据层

- 财务、采购、仓库优先建立报表权威取数面
- 优先采用“业务库直读 + analytics 准实时主题层”的混合模型
- 暂不强推 `DWD/DWS/ADS` 完整数仓分层

### 模板层

引入统一 `ReportTemplate` / `FixedReportCatalog` 资产，至少包含：

- `templateCode`
- `domain`
- `name`
- `queryMode`
- `sourceType`
- `targetView/targetTable`
- `sqlTemplate`
- `parameterSchema`
- `metricDefinition`
- `refreshPolicy`
- `presentationSchema`
- `permissionPolicy`

### 交互层

统一支持以下入口复用：

- 固定报表目录
- Dashboard 卡片
- Screen 大屏
- Report Factory
- Copilot 模板优先命中

## 非目标

当前阶段不做以下承诺：

- 不承诺把 `163` 张业务表全部语义化
- 不承诺把所有业务页一次性改造成标准 BI 报表
- 不承诺第一阶段就建设完整 `DWD/DWS/ADS`

## 成功标准

第一阶段验收标准：

1. 固定报表目录至少覆盖财务、采购、仓库三大域的高频报表
2. 已知报表优先命中模板，不再进入通用 NL2SQL
3. 未知报表仅在模板未命中时进入 Copilot 探索链
4. 报表模板可被 dashboard / screen / report factory / copilot 复用
5. 形成页面-数据库-模板三者之间的可追踪对照关系
