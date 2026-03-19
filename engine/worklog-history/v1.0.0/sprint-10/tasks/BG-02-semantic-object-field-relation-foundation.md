# BG-02: 业务视图层建设

**优先级**: P0
**状态**: READY
**依赖**: BG-01

## 目标

在 `copilot_analytics` 数据库中建立一组业务宽表视图，预 join 核心实体并翻译状态码为中文，使 NL2SQL 模型只需查询视图即可回答业务问题，彻底消除 join 陷阱和编码歧义。

## 设计原则

1. **模型只查视图，不查原始表** — 视图作为 NL2SQL 的唯一数据接口
2. **所有状态码翻译为中文字符串** — `status_name = '进行中'`，不暴露数字编码
3. **每个视图选定一个默认业务时间字段** — 消除时间歧义
4. **字段命名用中文注释** — comment 供模型理解字段含义
5. **视图跨库访问** — 通过 `analytics` 的外部数据源连接读取业务库

## 视图清单

### v_project_overview — 项目总览

预 join: `p_project` + `p_contract` + `p_customer` + 绿植统计 + 当月结算

```sql
-- 核心字段设计
project_id              -- 项目ID
project_name            -- 项目名称
project_code            -- 项目编号
project_status_name     -- 项目状态（正常/停用）
project_type_name       -- 项目类型（租摆/节日摆）
contract_title          -- 合同名称
contract_status_name    -- 合同状态（草稿/执行中/已结束）
contract_start_date     -- 合同开始日期
contract_end_date       -- 合同到期日期
settlement_type_name    -- 结算方式（按实摆/固定月租）
month_settlement_money  -- 固定月租金额
discount_ratio          -- 折扣率
customer_name           -- 客户名称
customer_code           -- 客户编号
manager_name            -- 项目经理
supervisor_name         -- 监理
biz_user_name           -- 业务经理
curing_director_name    -- 养护主管
address                 -- 项目地址
area                    -- 面积
budget_amount           -- 预算金额
position_count          -- 摆位总数
green_count             -- 当前在摆绿植数
total_rent              -- 当前月租金合计
project_start_time      -- 项目开始时间（默认业务时间）
project_end_time        -- 项目结束时间
```

### v_flower_biz_detail — 报花业务明细

预 join: `t_flower_biz_info` + `t_flower_biz_item` + `p_project` + `p_position`

```sql
-- 核心字段设计
biz_id                  -- 业务单ID
biz_code                -- 业务单号
biz_type_name           -- 业务类型（换花/加花/减花/调花/售花/坏账/销售/内购/加盆架/减盆架）
biz_status_name         -- 业务状态（草稿/审核中/备货/核算/待结算/已完成/作废/驳回）
project_name            -- 项目名称
position_name           -- 摆位名称
position_full_name      -- 摆位全称（含楼栋楼层）
apply_user_name         -- 发起人
apply_time              -- 发起时间（默认业务时间）
finish_time             -- 完成时间
is_urgent               -- 是否紧急（是/否）
green_name              -- 绿植名称
good_name               -- 物品名称
good_norms              -- 物品规格
plant_number            -- 数量
rent                    -- 单价租金
cost                    -- 单价成本
biz_total_rent          -- 业务总租金
biz_total_cost          -- 业务总成本
bear_cost_type_name     -- 费用承担方（养护人/领导/公司/客户）
manager_name            -- 项目经理
curing_user_name        -- 养护人
biz_month               -- 业务月份（从 apply_time 提取，方便按月统计）
```

### v_project_green_current — 当前在摆绿植

过滤: `p_project_green.status = 1`（摆放中）

预 join: `p_project_green` + `p_position` + `p_project`

```sql
-- 核心字段设计
green_id                -- 绿植ID
project_name            -- 项目名称
position_name           -- 摆位名称
position_full_name      -- 摆位全称
green_type_name         -- 绿植类型（单品/组合）
good_name               -- 物品名称
good_norms              -- 规格
good_specs              -- 花盆
good_unit               -- 单位
good_number             -- 数量
rent                    -- 月租金
cost                    -- 成本价
pose_time               -- 摆放时间（默认业务时间）
curing_user_name        -- 养护人
manager_name            -- 项目经理
floor_number_name       -- 楼栋
floor_layer_name        -- 楼层
```

### v_monthly_settlement — 月度结算

预 join: `a_month_accounting` + `p_project` + `p_contract` + `p_customer`

```sql
-- 核心字段设计
project_name            -- 项目名称
customer_name           -- 客户名称
contract_title          -- 合同名称
settlement_month        -- 结算月份（默认业务时间）
settlement_status_name  -- 结算状态（待结算/已结算）
total_rent              -- 应收租金
total_cost              -- 总成本
receivable_amount       -- 应收金额
received_amount         -- 已收金额
outstanding_amount      -- 未收金额
settlement_type_name    -- 结算方式
manager_name            -- 项目经理
biz_user_name           -- 业务经理
```

### v_task_progress — 任务进度

预 join: `t_daily_task_info` + `p_project`

```sql
-- 核心字段设计
task_id                 -- 任务ID
task_code               -- 任务编号
task_title              -- 任务标题
task_type_name          -- 任务类型（销售/内购/实摆变更/增值服务/支持/初摆）
task_status_name        -- 任务状态（待发起/进行中/已结束/作废）
project_name            -- 项目名称
launch_user_name        -- 发起人
leading_user_name       -- 负责人
launch_time             -- 发起时间（默认业务时间）
start_time              -- 计划开始
end_time                -- 计划结束
finish_time             -- 实际完成
total_number            -- 总项数
finish_number           -- 已完成项数
completion_rate         -- 完成率（计算字段）
total_rent              -- 预期租金
total_budget            -- 预期成本
```

### v_curing_coverage — 养护覆盖

预 join: `t_curing_record` + `p_project` + 摆位统计

```sql
-- 核心字段设计
project_name            -- 项目名称
curing_user_name        -- 养护人
curing_month            -- 养护月份（默认业务时间）
curing_count            -- 养护次数
total_position_count    -- 负责摆位总数
covered_position_count  -- 已养护摆位数
coverage_rate           -- 养护覆盖率（计算字段）
last_curing_time        -- 最近养护时间
```

### v_pendulum_progress — 初摆进度

预 join: `i_pendulum_info` + `p_project`

```sql
-- 核心字段设计
pendulum_id             -- 初摆ID
pendulum_code           -- 初摆编号
pendulum_title          -- 初摆标题
pendulum_status_name    -- 初摆状态（草稿/待审批/初摆中/完成/驳回/作废）
project_name            -- 项目名称
applicant_name          -- 申请人
applicant_date          -- 申请日期（默认业务时间）
start_date              -- 计划开始
end_date                -- 计划结束
total_budget_cost       -- 预算金额
actual_cost             -- 实际花费
balance_cost            -- 余额
year_rent               -- 年租金
```

## 实现方式

- 视图以 SQL VIEW 形式创建在 `copilot_analytics` 库中
- 通过 analytics 的外部数据源连接（MySQL FDW 或 JDBC）读取业务库
- 用 Liquibase changeset 管理视图的创建和变更
- 视图 DDL 的 COMMENT 作为模型理解字段的上下文

## 完成标准

- [ ] 7 个业务视图全部可查询
- [ ] 所有状态码字段均有对应的 `*_name` 中文翻译字段
- [ ] 每个视图有一个明确的默认业务时间字段
- [ ] 视图字段有 COMMENT 注释
- [ ] 现有 metadata sync 可识别视图并同步字段元数据
- [ ] 视图查询性能：单视图响应 < 3s（万级数据量）
