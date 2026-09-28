# 数据库扫描发现 (2026-03-20)

测试库: db.weitaor.com / rs_cloud_flower

## 数据量概览

| 表 | 总行数 | 活跃/有效 | 说明 |
|------|--------|----------|------|
| p_project | 208 | 161 (status=1) | 项目规模不大 |
| p_contract | 201 | 36 (status=2执行中) | 大部分已结束 |
| p_position | 13,567 | 12,460 (status=0) | |
| p_project_green | 42,990 | 42,968 (status=1在摆) | 核心数据 |
| t_flower_biz_info | 23,041 | 20,606 (status=5已完成) | 核心交易 |
| t_flower_biz_item | 146,111 | - | 明细量大 |
| t_curing_record | 34,979 | - | |
| p_curing_position | 40,639 | - | |
| a_month_accounting | 284 | 129 (status=2已结算) | 只有10个月(202405-202503) |
| t_daily_task_info | 170 | 149 (status=2进行中) | 量很小 |
| a_collection_record | **0** | - | **收款记录为空** |
| i_pendulum_info | **0** | - | **初摆数据为空** |
| s_stock_item | 194,829 | - | 库存单品量大 |

## 报花业务分布

| biz_type | 数量 | 说明 |
|----------|------|------|
| 1 (换花) | 10,778 | 最多 |
| 4 (调花) | 3,210 | |
| 2 (加花) | 2,201 | |
| 3 (减花) | 1,915 | |
| 10 (辅料) | 1,749 | **枚举词典里缺少** |
| 7 (销售) | 1,492 | |
| 8 (内购) | 1,225 | |
| 6 (坏账) | 470 | |
| 11 (加盆架) | 1 | |

业务时间范围: 2022-04-01 ~ 2026-02-06

## 关键问题与修正

### 1. p_project 没有人员姓名字段

`p_project` 只有 `manager_id`, `biz_user_id`, `supervisor_id`, `curing_director`（ID），没有 `manager_name` 等。

人员姓名存在 `p_project_role` 表:
- `project_manage=1` → 项目经理
- `biz_manage=1` → 业务经理
- `supervise=1` → 监理
- `curing_user=1` → 养护人

`p_project_role.user_name` 冗余存储了用户名。

**影响**: `v_project_overview` 视图需要 LEFT JOIN `p_project_role` 获取各角色姓名

### 2. a_collection_record 为空

测试库没有收款数据，`v_monthly_settlement` 的 `received_amount` / `outstanding_amount` 无法从收款表计算。

`a_month_accounting` 自带: `receivable_total_amount` (应收), `net_receipt_total_amount` (净收), `folding_after_total_amount` (折后)

**修正**: `v_monthly_settlement` 直接用 `a_month_accounting` 的字段，不依赖 `a_collection_record`

### 3. a_month_accounting 实际字段

| 方案中字段名 | 实际字段名 | 说明 |
|-------------|-----------|------|
| total_rent | receivable_total_amount | 应收总额 |
| received_amount | net_receipt_total_amount | 净收总额 |
| outstanding_amount | 需计算 | = receivable - net_receipt |
| settlement_month | year_and_month | 格式 YYYYMM (如 202405) |
| settlement_status_name | status | 1=待结算, 2=已结算 |

额外字段:
- `regular_rent` — 常规租金
- `discount_rate` — 折扣率
- `period_total_amount` — 期间总额
- `add_total_amount` — 加花总额
- `cut_total_amount` — 减花总额
- `sale_total_amount` — 销售总额
- `rent_type` — 计费方式
- `rent_from_contract` — 合同租金
- `rent_by_day` — 按天计租

### 4. 没有 sys_user 表

用户系统在另一个库，当前库没有 sys_user。
`p_project_role.user_name` 已冗余存储用户名，可直接使用。
`t_flower_biz_info` 有 `apply_use_name`, `curing_user_name` 等冗余字段。

### 5. i_pendulum_info 为空

初摆模块无数据，`v_pendulum_progress` 视图查不出任何结果。
Sprint-11 的 `mart_project_fulfillment_daily` 中初摆相关度量暂时无意义。

### 6. biz_type=10 (辅料) 缺失

枚举词典中缺少 `biz_type=10` 的条目，实际有 1,749 条。
需要补充: `10 = 辅料`

### 7. del_flag 软删除

大多数表使用 `del_flag='0'` 表示未删除。
**视图层 WHERE 条件必须加 `del_flag='0'`**

### 8. update_time 覆盖率

`t_flower_biz_info`: 22,753/23,041 有 update_time (98.7%)
可作为 ELT watermark 字段，但需处理约 1.3% 缺失的记录。
