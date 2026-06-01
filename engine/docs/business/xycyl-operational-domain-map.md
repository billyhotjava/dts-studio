# 馨懿诚绿植租摆系统 · 业务域全景地图（源码+数据库级）

> **用途**：dts-copilot / dts-stack 建模与 NL2SQL 口径治理的事实基线。
> **方法**：2026-06-01 对 adminapi（Java 微服务）、adminweb（Vue）、`rs_cloud_flower.sql`（147 张业务表）做的六域源码级勘探汇总。
> **边界**：DDL 取自 `adminapi/docker/mysql/db/rs_cloud_flower.sql`（⚠️ 敏感示例，勿外泄）。本文档只描述结构与口径，不含真实业务数据。

---

## 0. 系统全景

| 组件 | 技术 | 角色 |
|------|------|------|
| **adminapi** | Spring Cloud（rs-gateway/auth/system/flowable/flowers-base…）| 业务后端,核心在 `rs-modules/rs-flowers-base`(184 controllers / 88 mapper XML) |
| **adminweb** | Vue 2 + Element | 业务前端,`src/views/` 按 flower/expense/plant/tasknew/report/system 组织 |
| **app** | rs-flowers-base/app 包(53 controllers) | 移动端 API(养护人/采购员现场作业) |
| **rs_cloud_flower** | MySQL | 业务库,**147 张表** |
| **rs_cloud** | MySQL | 系统库,24 张 sys_*(权限/菜单/用户) |
| **dts-stack** | PG 中台 + dbt + Airflow + OpenMetadata + dts-platform 指标治理 | 数据治理与建模(ODS→stg→dwd→dws→ads) |
| **dts-copilot** | agent-first NL2SQL + 指标联邦 | 消费治理产物,交互与解释 |

数据流向:`rs_cloud_flower(MySQL) --Airflow ODS--> dts-stack(dbt 5层) --契约--> dts-copilot(只读消费)`。

---

## 1. 业务域地图(6 域)

| 域 | 核心表前缀 | adminapi 包 | adminweb | dts-stack 覆盖 |
|----|-----------|-------------|----------|---------------|
| **① 报花/业务单据** | `t_flower_*` `a_biz_*` `t_change_*` `t_frm_*` | flowerbiz | flower/bizbatch | ✅ flowerbiz.json 10 对象(部分 mart 未物化) |
| **② 项目/合同/点位/客户** | `p_project_*` `p_contract` `p_customer*` `p_position*` `p_floor*` `back_project_*` | project/appcustomer | flower/project | 🟡 sprint-25 project dbt(结算配置/租金段未建) |
| **③ 摆放/回收/撤场/调拨** | `i_pendulum_*`(15) `t_recovery_*` `t_allocation*` `back_*` `a_back_green` | pendulum/back/operate/storehouse | plant/change | 🟡 仅初摆主表+回收语义对象(ADS 未落地) |
| **④ 采购/库存/仓储/商品** | `b_goods*` `t_purchase_*` `t_plan_*` `s_stock_*` `t_warehousing_*` `t_ex_warehouse_*` `t_inventory_*` | purchase/storehouse/plan | purchase/store | 🟡 procurement.json 仅市场采购明细(入/出/库存全空白) |
| **⑤ 财务/结算/开票/收款/坏账/预付** | `a_month_accounting*` `a_green_accounting*` `a_sale_account*` `a_invoice*` `a_collection*` `f_settlement*` `f_expense_*` `f_advance_*` `f_voucher*` | finace/settlement/advance/operate | expense/finance/operate | 🟡 4 个 flowerbiz 汇总 mart(月对账/开票/收款/凭证全空白) |
| **⑥ 任务/督导/养护/统计报表** | `t_daily_task_*` `t_task_*` `t_supervise_*` `t_curing_*` `r_month_reprot_*` `u_staff_*` | tasknew/task/supervise/statistics/visualized/home | tasknew/work/report/dashboard | 🟡 field-operations.json 4 对象(2 视图未建;督导/薪资/报表全空白) |

---

## 2. 端到端业务流（缝合全部 6 域）

```
[②] 客户 p_customer → 合同 p_contract(续签链 parent_id) → 项目点 p_project
        → 楼号 p_floor_number → 楼层 p_floor_layer → 摆位 p_position
                                                          │
[③] 初摆 i_pendulum_info(项目点1:1) → 预算/采购/配送/决算 → 实摆台账 p_project_green
                                                          │
[①] 报花单 t_flower_biz_info(bizType 换/加/减/调/售/赠/坏)
        ├─ 草稿 draft_item_json(status=20) → 提交(1) → 审核 → 备货(2) → 复核(3) → 结算(4) → 结束(5)
        ├─ 明细 t_flower_biz_item → 锁实摆库存 lockBizNumber
        │                                                 │
[④]     ├─ 加花/换花 → 计划采购 t_plan_purchase_info
        │       ├─ 市场采购 t_purchase_info → 入库 t_warehousing(type=2) → s_stock_info(+)
        │       └─ 基地出库 t_ex_warehouse_info → 配送 → 项目点接收
        │                                                 │
        ├─ 减花/调花 → 回收 t_recovery_info(报损1/回购2/留用3)
[③]     │       └─ 回购 → 入库 t_warehousing(type=1) → s_stock_info(+)
        │                                                 │
[⑥]     └─ 配送完成 → 养护 t_curing_record / 督导 t_supervise_info(评分 zldf/hydf/wsdf)
                                                          │
[⑤] 月末 → 月对账 a_month_accounting(租摆链) + a_sale_account(售/赠/坏链)
        → 明细 a_green_accounting(三级金额×五分项)
        → 开票 a_invoice_info → 收款 a_collection_record → 回款 status=5
        → 财务结算 f_settlement → 凭证 f_voucher(SpEL 分录)
        → 月度财务报表 r_month_reprot_info(adminweb 内建报表)
```

**关键状态机**
- 报花 `t_flower_biz_info.status`: `20草稿 → 1审核中 →(21驳回) → 2备货 → 3复核 → 4结算 → 5已结束 / -1作废`。**4 个 service 并发写,无行级乐观锁**。
- 初摆 `i_pendulum_info.status`: `1草稿 → 2待审批 → 3初摆中 → 4完成 /(5拒绝 6作废)`（项目点 1:1,不可重摆）。
- 月对账 `a_month_accounting.status`: `1待归档 → 2已对账 → 3开票中 → 4待回款 → 5已回款`。
- 销售账单 `a_sale_account.status`: `1待结算 → 2开票中 → 3已回款`(回款时同步推报花 status=5)。

---

## 3. 口径治理铁律（NL2SQL 红线，最高优先级）

> 这些是六域勘探中最危险的发现。任何 mart / NL2SQL / agent 生成 SQL 必须遵守。

### 3.1 `biz_type` 是三套独立枚举,字段同名含义不同 ⚠️⚠️⚠️
| 出现位置 | 枚举 |
|---------|------|
| `t_flower_biz_info.biz_type`(主表) | 1换花 2加花 3减花 4调花 6坏账 7售花 8赠花 |
| `t_flower_biz_item.biz_type`(明细) | …4=调减 5=调加(与主表完全不同) |
| 单据编号生成函数(`FlowerBizInfoServiceImpl` L572) | 第三套:5售/6赠/7坏 |
| `a_invoice_item.biz_type` | 1租摆月对账 2销售账单 3绿化 4其他 |
| `a_collection_item.biz_type` / `t_warehousing.warehousing_type` / `t_ex_warehouse.out_house_type` | 各自又是独立枚举 |
**→ 跨表 JOIN 按 biz_type 过滤前,必须确认是哪张表的枚举。**

### 3.2 两条结算链,绝不混 SUM ⚠️⚠️⚠️
- **租摆链**(bizType 1/2/3/4)→ `a_month_accounting` + `a_green_accounting`,金额**不含增值税、含折扣**。
- **售/赠/坏链**(bizType 6/7/8)→ `a_sale_account`,金额**含增值税**(`receivable × (1+taxRate/100)`)。
- 两链都汇入 `a_invoice_info`,用 `a_invoice_item.biz_type`(1/2) 区分。
- **坏账(6)的 receivable 是损失,不是收入。**

### 3.3 月对账三级金额,极易选错列 ⚠️⚠️
每个分项(period/add/cut/adjust/sale)都有三套金额:
- `*_total_amount` = 名义租金(单价×数量×天数)
- `*_receivable_total_amount` = 应收(折前)
- `*_net_receipt_total_amount` / `folding_after_total_amount` = **折后实收(×discount_rate)**
- `total_amount` = 已回款。
普通项目(折扣=1)三者相等,极易写成 `SUM(receivable)` 而实际要 `SUM(folding_after)`。

### 3.4 销售摊入租摆(source_type=8)会双重计数 ⚠️
售花经 `saveSaleInRentData` 摊入租摆后,同一笔金额既在 `a_sale_account` 又在 `a_month_accounting`(saleTotalAmount)。分别统计会重复。

### 3.5 其它口径陷阱
- **合同租金回写**:`saverentfeeandapply` 会直接覆盖 `p_project_green.rent`,历史租金只在 `p_project_green_sett` 保留——历史口径必须读 sett 表,不能读 green.rent。
- **库存成本**:`s_stock_info.out_cost` 是**加权平均**(非 FIFO),归零时被置 0,有并发瞬态写 0 风险。`b_goods_price.cost_price`(主数据指导价)与入库实际成本是两个独立口径。
- **good_price_id 而非 good_name**:库存/采购/出库全部按 `b_goods_price.id`(SKU)关联,同名物品多 SKU,不能只按名聚合。
- **JSON 关联**:`f_settlement.biz_ids_json`、`t_*.draft_item_json` 是 JSON 文本,等值 JOIN 全不匹配,需展开。
- **varchar 金额**:`a_sale_account_rent_item.rent` 是 `varchar(255)`,聚合前须 CAST。
- **额外费用 ≠ 费用报销**:`t_flower_extra_cost`(花单向客户收的附加费/成本) vs `f_expense_account_info`(公司支出报销),完全不同实体。
- **SpEL 开关**:`rs.spelrule.enabled/autorun` 关闭时结算单不生成分录、不写凭证——"有结算无凭证"的根因。

---

## 4. dts-stack 覆盖矩阵（已建 vs 空白）

| 口径/对象 | 状态 | 说明 |
|----------|------|------|
| 报花租金净额/销售/坏账/额外费用汇总 | ✅ flowerbiz.json 已建 mart | sprint-22/26 |
| 客户月度汇总 | ✅ `xycyl_dws_flowerbiz_customer_monthly` | |
| 项目概览/状态分布/合同到期/客户维度/点位调整 | 🟡 sprint-25 dbt | 部分 |
| 报花/日常任务/养护覆盖/初摆(field-operations) | 🟡 2/4 视图未建 | `v_curing_coverage`/`v_pendulum_progress` 缺 |
| 市场采购明细/采购人汇总(procurement) | 🟡 仅这两个 | |
| **入库/出库/库存余量/库存成本** | ❌ 空白 | s_stock/t_warehousing/t_ex_warehouse 未进 ODS |
| **月对账应收/回款进度** | ❌ 空白 | a_month_accounting 无专项 mart |
| **开票/发票/收款进度** | ❌ 空白 | a_invoice/a_collection 无 mart |
| **预付/备用金/费用报销/财务凭证** | ❌ 空白 | f_advance/f_expense/f_voucher 无 mart |
| **督导监管/盘点盈亏/员工薪资/任务工时** | ❌ 空白 | 无语义对象、无 ELT |
| **在摆历史快照/调拨/退货/自采** | ❌ 空白 | back_project_put_place / a_back_green / t_allocation 等 |
| **月度财务报表 r_month_reprot** | ❌ 空白 | adminweb 内建报表直接打业务库 |

血缘断点:`t_change_info`、`t_warehousing_info` 在 mart 设计中被引用,但 **Airflow 无对应 ODS 同步**,相关 mart 无法从 ODS 构建。

---

## 5. 风险与技术债台账

| 级别 | 项 | 位置 |
|------|----|------|
| 🔴 **CRITICAL 安全** | **生产 MySQL/PG 明文密码硬编码**(`Devops123@`) | `dts-stack/services/dts-airflow/dags/ptr_mysql_flow-*_ods_*.json`(全部 5 个 tenant) |
| 🟠 HIGH | 大屏总览 `/visualized/getRealData` 返回**写死假数据**;真实同步 Job 全被注释 | `VisualizedController.java` L27-33 / `AutoSyncBiDataJob.java` |
| 🟠 HIGH | 角色过滤 SQL 片段拼接进 mapper(潜在注入) | `FlowerBizInfoController.listFlowerBizNew` → `getProjectRealRoleSql()` |
| 🟡 MEDIUM | 报花提交在单 `@Transactional` 内串调锁库存+消息+推送,任一失败全回滚 | `FlowerBizInfoServiceImpl` L332-396 |
| 🟡 MEDIUM | 月对账确认 `placeByAccountData` 用 `Thread.sleep(2000)` 等待(非原子) | `ProjectPutPlaceServiceImpl` L479 |
| 🟡 MEDIUM | 统计端点直接打业务库:`listProjectSummary` 7 路子查询、`listFlowerItemData` 17 路 UNION ALL | `ProjectSummaryMapper.xml` / `FlowerNewSpaperMapper.xml` |
| 🟢 LOW | 生产 `System.out.println` / `printStackTrace` 多处 | DistributionController / StockInfoServiceImpl 等 |
| 🟢 LOW | 死代码:`PlanPurchaseInfoServiceImpl` 注释方法体后退化为全表删/查;adminweb `*- 副本.vue` 残留 | |

---

## 6. 对「agent 介入建模 authoring」brainstorm 的输入

1. **口径敏感域不能交给 agent 现生成 SQL**:§3 的 biz_type 三枚举、两结算链、三级金额、双重计数——这些规则编码进 dbt/语义包一次即确定性;agent 临时推导必错。这正是"治理在 stack"的硬理由。
2. **空白域是 agent authoring 的高价值靶区**:§4 的 ❌ 项(库存/财务回款/督导/薪资)——agent 可**草拟** ODS 同步 + dbt 模型 + 语义对象,但产物须评审晋升,不做运行时转换。
3. **先补血缘断点**:`t_change_info`/`t_warehousing_info` 等 ODS 缺失,是任何下游建模的前置。
4. **报表对齐**:adminweb 的 `r_month_reprot` 内建报表与 FineReport 外链,是 dts-copilot fixed-report 的现实参照系;统计端点的重查询(17 路 UNION)是"长尾迁移到 mart"的天然候选。
5. **安全前置**:§5 的密码硬编码必须在任何 agent 接触 ELT 配置前清除。

---

## 附:六域详细简报来源

本文档由 6 个 code-explorer 并行勘探汇总。各域详细的表清单/端点/行号锚点见勘探原始输出(报花/项目/摆放回收/采购库存/财务/任务督导统计)。关键文件索引见各域简报。
