# dbt 模型 vs 生产数据库 review

> 数据来源：39.106.43.56:3307/rs_cloud_flower（生产副本，MySQL 5.7）
> review 日期：2026-05-02
> 被 review 对象：`assets/xycyl-flowerbiz-dbt-model.zip`（commit 17d69fd）
> 数据规模：t_flower_biz_info **29,405 行**，t_flower_biz_item 196,334 行

## 结论

**zip 不能直接上线**，存在 3 类阻塞问题：

| 严重度 | 问题域 | 数量 | 影响 |
|---|---|---|---|
| CRITICAL | ODS DDL 列名/STG 引用 phantom 列 | 12 处 | dbt run 直接编译失败 |
| HIGH | 金额方向重复加签（biz_total_rent 已自带符号） | 1 处口径 | 加摆/减摆净额错误 |
| HIGH | total_amount 误当通用金额（实为售价专用） | 5 个 mart | 租赁报表全错 |
| MEDIUM | 25.5% detailed 孤儿、log.biz_type 98% NULL | 2 处 | mart 字段含义偏差 |

---

## 一、CRITICAL — 列名 phantom（DDL 与 STG 双层错位）

### t_flower_biz_info（main 表）

| 我假设的列名 | 真实列名 | 状态 |
|---|---|---|
| `apply_user_id` | **`apply_use_id`** | typo（少一个 r） |
| `apply_user_name` | **`apply_use_name`** | typo |
| `project_manager_id` | **`project_manage_id`** | 多了 `r` |
| `project_manager_name` | **`project_manage_name`** | 多了 `r` |
| `customer_id` | **不存在** | t_flower_biz_info 没有 customer_id 列。客户信息以反范式 `customer_name`/`phone_number`/`address` 存在主表 |
| `end_lease_time` | **不存在** | 实际字段为 `plan_finish_time` + `finish_time` + `settlement_time` |

### t_change_info（变更单）

| 我假设 | 真实 | 状态 |
|---|---|---|
| `del_flag` | **不存在** | 直接 `WHERE del_flag = '0'` 会报错 |
| `change_reason` | **不存在** | 实际是 `remark` |
| `change_user_id` | **不存在** | 实际是 `apply_user_id`（发起人）+ `confirmed_user_id`（确认人） |
| `change_time` | **不存在** | 实际是 `apply_time` + `confirmed_time` |

### t_recovery_info / t_recovery_info_item / t_flower_rent_time_log / t_flower_biz_log

**全部没有 `del_flag` 字段**。我的 STG 都加了 `WHERE del_flag = '0'` — 全部会编译失败。

仅 `t_flower_biz_info`、`t_flower_biz_item`、`t_flower_extra_cost` 这 3 张表真的有 `del_flag`。

### t_flower_rent_time_log

| 我假设 | 真实 |
|---|---|
| `change_reason` | 不存在 |
| `del_flag` | 不存在 |
| `update_time` | 不存在 |

### t_flower_biz_item_detailed

我建模时假设它有 `position_id / good_id / good_name / plant_number / rent / cost / del_flag / create_time / update_time`，**全部不存在**。真实 schema 只有 8 个字段：

```
id, flower_biz_item_id, project_green_item_id, status, source, price, allocate_time, plan_purchase_info_id
```

它本质是 **报花明细 → 项目摆位库存** 的分配/出库 junction 表，不是平行明细。需要重新建模。

### p_project（项目）

| 我假设 | 真实 |
|---|---|
| `customer_id` | **不存在**（项目不直接挂客户） |
| `customer_name` | **不存在** |
| `project_manager_id` | 真实是 `manager_id` |
| `project_manager_name` | **不存在**（需 join u_personnel） |

---

## 二、HIGH — 金额口径错误

### 2.1 `biz_total_rent` **已是有符号字段**（按 biz_type 自然分布）

```text
biz_type=1 换花 :  657 正 / 0 负 / 12,916 零（多数等价交换不计差额）
biz_type=2 加花 : 2213 正 / 0 负 / 346 零（自然为正）
biz_type=3 减花 :    0 正 / 2090 负 / 252 零（**自然为负**）
biz_type=4 调花 :  744 正 / 632 负 / 2159 零（同时存在正负）
```

**冲突**：我在 `dim_flowerbiz_biztype` 加了 `amount_direction`（in/out/neutral），并在 fact 里 `amount_abs * sign_multiplier`。这会**重复加符号**，减摆净额变成 `(-1428k)` × `(-1)` = +1428k。lease_summary mart 输出会和 UI 完全相反。

**修复**：删除 `dim_biztype.amount_direction` 列；DWD/ADS 直接 `SUM(biz_total_rent)`，自然得到加摆-撤摆净额。

### 2.2 `total_amount` 只对 biz_type=7（售）有意义

```text
biz_type=7 售 : 438 行有值（占 22.6%），合计 169.9 万
其他 7 个 bizType : 全部 NULL
```

**冲突**：我把 `total_amount` 暴露为通用金额并在多个 mart 里 SUM 它。租赁/调花/坏账/赠/配料场景下永远得 0。

**修复**：
- `lease_summary` / `change_log`：使用 `biz_total_rent`
- `sale_summary`：使用 `total_amount` WHERE biz_type=7
- `baddebt_summary`：使用 `biz_total_cost` 或自定义口径（坏账金额需进一步从 service impl 验证）
- `extra_cost_summary`：使用 `t_flower_extra_cost.price_amount` 或 `total_extra_cost`
- 建议在 dwd_main 拆出 4 列：`rent_amount` / `cost_amount` / `sale_amount` / `extra_cost_amount`，分别对应不同口径

### 2.3 `start_lease_time` 仅 biz_type 2/3/4 有值

```text
biz_type=2/3/4 : 81%-94% 有 start_lease_time
biz_type=1/6/7/8/10 : 0% 有 start_lease_time
```

**修复**：caliber-decision-1 `business_date = COALESCE(start_lease_time, apply_time)` 总体有效，但 dim 文档需明确：换花/售/赠/坏账/配料 没有"起租期"概念，business_date = apply_time 是唯一选择。

### 2.4 起租期被事后改动率：6.4%

`t_flower_rent_time_log` 中 `rent_time_type=1` 的真正变更（old != new）共 468 条 / 7275 条有 start_lease_time 的报花 = 6.4%。

**说明**：`start_lease_time` mutate 风险存在但比例低，DWS 取 main 当前值是可接受口径；audit_trail mart 应展示历史轨迹。

### 2.5 `rent_time_type` 有 2 个值，不是 1 个

```
rent_time_type=1 : 5875 行（起租期变更）
rent_time_type=2 :  262 行（用途待 service 确认）
```

dim 需要补充 type=2。

---

## 三、MEDIUM — 字段含义错位

### 3.1 t_flower_biz_item_detailed 25.5% 孤儿

57,353 / 224,823 detailed 记录的 `flower_biz_item_id` 在 `t_flower_biz_item` 中查无对应。可能是历史 hard-delete。

**修复**：
- 重写 dwd_item_detailed 暴露 8 个真实字段
- ADS lease_detail 必须 LEFT JOIN，加 `is_orphan` 标记
- schema test `relationships` 设 `severity: warn` 避免误报失败

### 3.2 `t_flower_biz_log.biz_type` 98% NULL

```
biz_type=NULL : 290,008 行（98.2%）
biz_type=1-4 : 仅 492 行
```

我 STG 当成报花单 biz_type 暴露，但实测它**不是父表 biz_type**（看分布完全不像）。

**修复**：STG 字段重命名 `biz_type_raw` → `log_biz_type_raw`，并加注释说明它是日志特定语义。dim 关联只走 `biz_id → main.id`。

### 3.3 t_flower_biz_info 缺暴露的关键字段

主表 84 个字段，我只暴露了 ~20 个。建议补充：

| 字段 | 用途 |
|---|---|
| `total_extra_cost` / `total_extra_price` | 额外成本/价格（复核口径） |
| `fare` / `labor_cost` / `cleaning_fee` | 运费/人工/清运分项（销售/坏账场景） |
| `tax_rate` / `rent_discount_ratio` | 税率/折扣率 |
| `accounting_status` / `cut_confirm_status` / `print_status` | 多种业务状态 |
| `examine_time` / `review_time` / `sign_time` / `cut_confitm_time` | 全审批链时间戳（audit_trail 需要） |
| `expense_id` / `expense_code` / `settle_id` / `settle_code` | 财务跨域钩子（虽然 sprint-25 才接，但应该 STG 里暴露） |
| `bad_debt_type` | 坏账分类（biz_type=6 mart 需要） |
| `transfer_type` / `source_type` / `bear_cost_type` / `sales_payment_type` | 4 个分类字段 |
| `urgent` | 紧急标记 |
| `customer_name` / `phone_number` / `address` | 反范式客户信息 |
| `tenant_id` | 多租户隔离 |
| `batch_code` | 批次编号 |
| `task_info_id` / `task_code` / `task_item_id` | 任务关联 |

### 3.4 t_change_info 缺 before/after 字段对

主 schema 包含丰富的 before/after pair：

```
before_total_amount / after_total_amount
before_settlement_time / after_settlement_time
before_good_price_id / after_good_price_id
before_good_name / after_good_name
before_good_type / after_good_type
before_good_norms / after_good_norms
before_good_specs / after_good_specs
before_good_unit / after_good_unit
change_number    -- 变更数量
```

我只暴露了 amount + settlement_time 的 before/after。mart change_log 需要支持 "起租期变更" 和 "货物种类变更" 两种问句，全字段需暴露。

### 3.5 dim_change_type 只有 3 个值，不是 4 个

```
change_type=1 : 4 行
change_type=2 : 1139 行（占 96.5%）
change_type=3 : 24 行
```

dim 当前 4 行，需删一个 + 验证 1/2/3 的真实业务含义（需读 ChangeService.java）。

### 3.6 dim_biztype 中 11/12 是僵尸值

```
biz_type=11 加盆架 : 仅 1 行
biz_type=12 减盆架 : 0 行
```

保留 dim 项无害，但应在 `description` 标记 "未投入使用" 或 "试运行"，并在 NL2SQL prompts 中说明。

---

## 四、CORRECT — 验证通过的部分

| 项 | 状态 |
|---|---|
| 8 status 编码（-1/1/2/3/4/5/20/21）| ✅ 全部出现，分布合理（5=已结束 占 89.9%） |
| t_flower_biz_item.flower_biz_id 0% 孤儿 | ✅ FK 干净 |
| t_recovery_info_item.recovery_type 3 个值（1/2/3） | ✅ 与 dim 对齐 |
| t_flower_extra_cost.cost_type 5 个值（1/2/3/4/5）| ✅ 与 dim 对齐 |
| 主表 biz_total_rent ≈ SUM(item.rent × plant_number) 67.9% 完全相等，5.3% 不等，26.9% 主有值但无 item | ✅ 两层金额可独立维护，DWS 取 main 与业务 UI 一致 |
| `a_flower_biz_accounting` 表确认空（0 行）→ sprint-25 接入 | ✅ 边界正确 |
| `a_sale_account` 967 行实收凭证存在 → sprint-25 接入 | ✅ 边界正确 |
| t_flower_biz_log 有完整审批链（现场接收/采购完成/开始配送/结束/单据回退/...）| ✅ audit_trail mart 可行 |

---

## 五、新发现的边界范围

| 表 | 行数 | 处理方案 |
|---|---|---|
| `a_green_accounting` | 166,506 | sprint-25 财务域，**不是** sprint-22 范围 |
| `a_green_accounting_temp` | 347,027 | 临时表，忽略 |
| `a_month_accounting` | 531 | sprint-25 |
| `a_invoice_info/item/record` | 128/406/62 | sprint-25 财务域 |
| `p_project_green` / `p_project_green_item` / `p_project_green_sett` | 36k / 86k / 31k | sprint-24 摆放域 |
| `p_project_green_copy0526` | 42k | 备份表，忽略 |
| `t_plant_picking_good` | 52,571 | 采购域，sprint-23 |
| `t_plan_purchase_info` / `t_plan_purchase_item` | 33k / 98k | sprint-23 |

---

## 六、修复优先级与工作量估算

| 阶段 | 任务 | 工作量 |
|---|---|---|
| **A. 阻塞修复** | ODS DDL 列名修正 + STG 列引用对齐 + 删除 phantom del_flag 过滤 | ~1.5h |
| **B. 口径修复** | 删 dim_biztype.amount_direction；改 fact 用 raw 有符号字段；拆 4 列金额 | ~1h |
| **C. mart 重写** | lease_summary / sale_summary / baddebt_summary / change_log / extra_cost_summary 5 个 mart 用正确字段 | ~1.5h |
| **D. 字段补全** | 主表 + change_info 补全暴露字段；detailed 重写 | ~1h |
| **E. 测试更新** | accepted_values / relationships severity / 新字段 not_null | ~0.5h |
| **F. 文档对齐** | 字段映射 CSV、source-catalog、README、field-mapping CSV | ~1h |
| **G. 重打包 + 验证** | mysql 真实数据上模拟 ODS dump → 跑 dbt parse 检查所有引用 | ~0.5h |

总计：~7h 工作。**建议进入 phase A→B→G（阻塞修复+口径修复+解析验证）作为最小可上线增量**，DCEF 在第二轮迭代。

---

## 七、建议下一步

请确认是否：

1. **路线 1（推荐）**：我先做 phase A+B+G 出 v2 zip，确保导入生产环境能跑通基础链路（lease/sale/change/recovery 4 大主题正确），其它字段补全留 sprint-22 收尾或 sprint-23 滚入。
2. **路线 2**：一次性做 A→F 全套，时间更长但产出完整。
3. **路线 3**：先不动代码，把这份 review 直接发给业务/产品团队对齐口径，等 service impl 对照后再统一改。

我倾向路线 1。
