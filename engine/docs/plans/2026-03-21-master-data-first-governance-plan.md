# XYCYL 主数据优先治理 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 `adminapi/adminweb/app` 与 `rs_cloud_flower` 建立“项目轴 + 物品轴”优先的主数据治理骨架，支撑后续固定报表和 Copilot 的受控消费面。

**Architecture:** 先做三端源码与业务库的主数据归属审计，再沉淀 canonical model、主数据/交易事实边界表和固定报表消费约束。第一阶段不重写流程，只统一主数据语义和归属。

**Tech Stack:** Spring Cloud、Vue 2、uni-app、MySQL、Liquibase、dts-copilot worklog/docs

---

### Task 1: 主数据现状盘点与归属审计

**Files:**
- Create: `worklog/v1.0.0/sprint-17/tasks/MD-01-master-data-inventory-and-ownership-audit.md`
- Modify: `worklog/v1.0.0/sprint-17/README.md`
- Reference: `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/project/**`
- Reference: `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/base/**`
- Reference: `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/storehouse/**`
- Reference: `adminweb/src/views/flower/project/**`
- Reference: `adminweb/src/views/flower/base/**`
- Reference: `app/pages/project/**`

**Step 1: 列出主数据候选对象**

- 项目、客户、合同、项目角色、摆位、物品、分类、属性、价格、库房、供应商、组织、字典

**Step 2: 记录三端与数据库归属**

为每类对象记录：

- 后端控制器/服务模块
- PC 页面
- 移动端页面
- 主要表族

**Step 3: 标记问题类型**

- 一类主数据多处归属
- 只有页面无独立主数据接口
- 只有表无明确 UI 管理面
- 被交易事实反向“污染”

### Task 2: 项目轴 canonical model

**Files:**
- Create: `worklog/v1.0.0/sprint-17/tasks/MD-02-project-axis-canonical-model.md`
- Modify: `worklog/v1.0.0/sprint-17/README.md`
- Reference: `adminapi/.../project/controller/ProjectController.java`
- Reference: `adminapi/.../project/controller/CustomerController.java`
- Reference: `adminapi/.../project/controller/ContractController.java`
- Reference: `adminweb/src/views/flower/project/project/list-project.vue`
- Reference: `adminweb/src/views/flower/project/customer/list-customer.vue`
- Reference: `adminweb/src/views/flower/project/contract/list-contract.vue`

**Step 1: 定义项目轴对象树**

- 客户
- 合同
- 项目点
- 楼层/楼号/摆位
- 项目角色

**Step 2: 定义稳定业务键**

- 客户编码
- 合同编码
- 项目点编码
- 摆位编码

**Step 3: 明确哪些属于配置事实**

- 项目绿植
- 结算配置
- 合同租金规则

### Task 3: 物品轴 canonical model

**Files:**
- Create: `worklog/v1.0.0/sprint-17/tasks/MD-03-goods-axis-canonical-model.md`
- Modify: `worklog/v1.0.0/sprint-17/README.md`
- Reference: `adminapi/.../base/controller/GoodsController.java`
- Reference: `adminapi/.../base/controller/GoodsPriceController.java`
- Reference: `adminapi/.../base/controller/GoodsAttributeController.java`
- Reference: `adminweb/src/views/flower/base/goods/list-goods.vue`
- Reference: `adminweb/src/views/flower/base/goodPrice/list-good-price.vue`

**Step 1: 定义物品轴对象树**

- 物品
- 分类
- 属性
- 规格
- 价格
- 别名

**Step 2: 定义唯一识别与展示规则**

- 业务编码
- 标准名称
- 规格名称
- 项目内别名

**Step 3: 识别库存/采购/报花对物品轴的反向耦合**

- 哪些页面直接改价格
- 哪些流程直接依赖库存结果面

### Task 4: 共享参考主数据模型

**Files:**
- Create: `worklog/v1.0.0/sprint-17/tasks/MD-04-shared-reference-master-data.md`
- Modify: `worklog/v1.0.0/sprint-17/README.md`
- Reference: `adminapi/.../storehouse/controller/StorehouseInfoController.java`
- Reference: `adminweb/src/views/flower/store/storehouse/list-store-house.vue`
- Reference: `adminweb/src/views/flower/project/contract/list-supply.vue`

**Step 1: 定义共享参考对象**

- 库房
- 供应商
- 员工/组织/角色
- 状态字典
- 结算/付款/来源码

**Step 2: 标记供应商缺口**

- 是否存在独立后端主数据归属
- 是否仅作为业务页附属信息存在

**Step 3: 输出参考主数据治理优先级**

- 先库房
- 再供应商
- 再组织/字典

### Task 5: 主数据与交易事实划界

**Files:**
- Create: `worklog/v1.0.0/sprint-17/tasks/MD-05-master-vs-transaction-boundary.md`
- Modify: `worklog/v1.0.0/sprint-17/README.md`
- Reference: `adminapi/.../storehouse/controller/StockInfoController.java`
- Reference: `adminapi/.../purchase/controller/PurchaseInfoController.java`
- Reference: `adminapi/.../flowerbiz/controller/FlowerBizInfoController.java`
- Reference: `adminapi/.../tasknew/controller/ExecuteTaskController.java`
- Reference: `adminapi/.../operate/controller/MonthAccountController.java`
- Reference: `adminapi/.../finace/controller/VoucherController.java`

**Step 1: 建主数据清单**

- 保留为 master

**Step 2: 建业务事实清单**

- 采购、库存、报花、任务、账单、凭证

**Step 3: 标记配置型事实**

- project_green
- contract_rent_fee
- project_settlement_config

### Task 6: 主数据消费约束

**Files:**
- Create: `worklog/v1.0.0/sprint-17/tasks/MD-06-master-data-consumption-rules.md`
- Modify: `worklog/v1.0.0/sprint-17/README.md`
- Reference: `worklog/v1.0.0/sprint-16/**`

**Step 1: 固定报表消费规则**

- 报表必须挂项目轴/物品轴/参考主数据

**Step 2: Copilot 消费规则**

- 先主数据识别
- 再模板命中或探索

**Step 3: 页面硬编码收口规则**

- 新页面不得重复定义来源码/状态码/名称别名

### Task 7: 迁移顺序与验收基线

**Files:**
- Create: `worklog/v1.0.0/sprint-17/tasks/MD-07-migration-sequencing-and-acceptance.md`
- Create: `worklog/v1.0.0/sprint-17/it/README.md`
- Modify: `worklog/v1.0.0/sprint-17/README.md`

**Step 1: 定义四阶段顺序**

- 阶段 1：项目轴
- 阶段 2：物品轴
- 阶段 3：共享参考主数据
- 阶段 4：固定报表/Copilot 接入

**Step 2: 定义验收基线**

- 页面/API/表归属唯一
- 主数据键稳定
- 状态码口径统一
- 固定报表与 Copilot 能挂载主数据

**Step 3: 定义后续实现拆分**

- 不做大爆炸重写
- 以治理、映射、受控消费面为主

Plan complete and saved to `docs/plans/2026-03-21-master-data-first-governance-plan.md`. Two execution options:

1. `Subagent-Driven (this session)`：我继续在当前会话里按 task 往下拆和实现
2. `Parallel Session (separate)`：新会话使用 `executing-plans` 按计划执行
