# 业务域盘点清单

## 一、主题域一：项目履约

### 核心实体

| 业务对象 | 主表 | 表前缀 | 关键外键 | 关键状态字段 | 默认业务时间 |
|----------|------|--------|----------|-------------|-------------|
| 客户 | `p_customer` | p_ | - | `status` (1=有效,2=其他) | `create_time` |
| 合同 | `p_contract` | p_ | `customer_id` | `status` (1=草稿,2=执行中,3=已结束) | `signing_time` |
| 项目点 | `p_project` | p_ | `contract_id` | `status` (1=正常,2=停用), `type` (1=租摆,2=节日摆) | `start_time` |
| 楼栋 | `p_floor_number` | p_ | `project_id` | - | - |
| 楼层 | `p_floor_layer` | p_ | `floor_number_id` | - | - |
| 摆位 | `p_position` | p_ | `project_id` | `status` (0=正常,1=停用), `type` (1=室内,2=室外) | `create_time` |
| 项目绿植 | `p_project_green` | p_ | `project_id`, `position_id` | `status` (1=摆放中,2=换花中,3=加花中,4=减花中,5=调花中,6=坏账,7=已结束) | `pose_time` |
| 养护记录 | `t_curing_record` | t_ | `project_id` | `record_type` (1=养护记录,2=工作日报) | `curing_time` |
| 养护位置 | `p_curing_position` | p_ | `project_id`, `position_id` | `status` (1=养护中,2=已转出) | `start_time` |
| 月度结算 | `a_month_accounting` | a_ | `project_id` | 结算状态 | 结算月份 |
| 收款记录 | `a_collection_record` | a_ | 关联项目/合同 | 收款状态 | `pay_time` |

### 关键人员字段

| 角色 | p_project 字段 | 说明 |
|------|---------------|------|
| 项目经理 | `manager_id` / 关联 sys_user | 审批报花、管理项目 |
| 监理 | `supervisor_id` | 质量检查 |
| 业务经理 | `biz_user_id` | 销售、客户关系 |
| 养护主管 | `curing_director` | 管理养护团队 |
| 养护人 | `p_curing_position.curing_user_id` | 现场维护执行 |

### 核心 Join 路径

```
p_customer → p_contract (customer_id)
  → p_project (contract_id)
    → p_position (project_id)
      → p_project_green (project_id + position_id)
    → a_month_accounting (project_id)
    → t_curing_record (project_id)
```

---

## 二、主题域二：现场业务

### 核心实体

| 业务对象 | 主表 | 表前缀 | 关键外键 | 关键状态字段 | 默认业务时间 |
|----------|------|--------|----------|-------------|-------------|
| 报花业务 | `t_flower_biz_info` | t_ | `project_id` | `biz_type` (1~12), `status` (-1~21) | `apply_time` |
| 报花明细 | `t_flower_biz_item` | t_ | `flower_biz_id` | `biz_type` (1~5,10) | `put_time` |
| 报花日志 | `t_flower_biz_log` | t_ | `flower_biz_id` | - | `create_time` |
| 业务变更 | `t_change_info` | t_ | `flower_biz_id` | `change_type` (1~4) | `create_time` |
| 额外费用 | `t_flower_extra_cost` | t_ | `flower_biz_item_id` | - | - |
| 初摆信息 | `i_pendulum_info` | i_ | `project_id` | `status` (1~6) | `applicant_date` |
| 初摆预算 | `i_pendulum_budget` | i_ | `pendulum_id` | - | - |
| 初摆采购 | `i_pendulum_purchase` | i_ | `pendulum_id` | `status` (1~3) | `plan_purchase_time` |
| 日常任务 | `t_daily_task_info` | t_ | `project_id` | `status` (-1,1,2,10), `task_type` (1~6) | `launch_time` |
| 任务明细 | `t_daily_task_item` | t_ | `daily_task_id` | `status` | - |

### 业务类型编码 (t_flower_biz_info.biz_type)

| 编码 | 中文 | 说明 |
|------|------|------|
| 1 | 换花 | 替换现有绿植 |
| 2 | 加花 | 新增绿植 |
| 3 | 减花 | 退租移除绿植 |
| 4 | 调花 | 项目间转移绿植 |
| 5 | 售花 | 销售绿植 |
| 6 | 坏账 | 损坏核销 |
| 7 | 销售 | 销售（含组合） |
| 8 | 内购 | 内部采购 |
| 11 | 加盆架 | 新增花盆/花架 |
| 12 | 减盆架 | 移除花盆/花架 |

### 业务状态流转 (t_flower_biz_info.status)

```
草稿(20) → 审核中(1) → 备货中(2) → 核算中(3) → 待结算(4) → 已完成(5)
                ↓
            驳回(21)
                ↓
            作废(-1)
```

---

## 三、易混淆业务词汇

| 用户说法 | 实际含义 | 对应表/字段 | 易混淆项 |
|----------|---------|-----------|---------|
| 报花 | 所有花卉业务的统称 | `t_flower_biz_info` | ≠ 某一种 biz_type |
| 减花 | 退租移除绿植 | `biz_type=3` | ≠ 剪花（修剪养护） |
| 剪花 | 养护修剪 | `t_curing_record` | ≠ 减花 |
| 调花 | 项目间转移绿植 | `biz_type=4` | ≠ 摆位调整 |
| 摆位调整 | 同项目内移动位置 | `p_position_adjustment` | ≠ 调花 |
| 实摆 | 当前在位的绿植 | `p_project_green` | - |
| 初摆 | 新项目首次布置 | `i_pendulum_info` | ≠ 实摆 |
| 项目点 | 客户现场物理地点 | `p_project` | ≠ 摆位 |
| 摆位 | 项目点内放花的位置 | `p_position` | ≠ 项目点 |
| 租金 | 月度应收租赁费 | `a_month_accounting` | ≠ green.rent（单价） |

---

## 四、结算相关特殊逻辑

| 逻辑 | 涉及字段 | 说明 |
|------|---------|------|
| 按实摆结算 | `p_contract.settlement_type=1` | 租金 = Σ(绿植 × 单价)，由 Java 计算 |
| 固定月租 | `p_contract.settlement_type=2` | `month_settlement_money` |
| 折扣率 | `p_contract.discount_ratio` | 默认 1.0 |
| 结算周期 | `p_contract.verify_type` | 1=自然月, 2=固定结算日 |
| 费用承担 | `t_flower_biz_info.bear_cost_type` | 1=养护人,2=领导,3=公司,4=客户 |

**结论：结算类问题必须查 `a_month_accounting` 已计算结果，不能从绿植数据重算。**

---

## 五、角色与高频问句

### 项目经理
1. XX项目目前有多少在摆绿植？
2. XX项目这个月换花了多少次？
3. 我负责的项目中，哪个换花率最高？
4. XX项目上个月租金是多少？
5. XX项目有多少待审批的报花单？

### 业务经理
6. 哪些合同快到期了（90天内）？
7. 上月应收款还有多少没收齐？
8. XX客户下面有几个项目？
9. 本月新签了几份合同？
10. 哪些项目的收款确认还在待处理？

### 运营总监
11. 当前在服项目一共多少个？
12. 各项目加花次数排行（本月）
13. 养护人均负责多少摆位？
14. 本月报花业务中，加花/换花/减花各多少？
15. 哪些项目的绿植数量变化最大？

### 养护主管
16. 今天有多少待处理的任务？
17. XX养护人负责哪些摆位？
18. 本月哪些摆位还没做过养护？
19. 进行中的初摆任务有几个？
20. XX项目的减花回收情况怎么样？

### 财务
21. 上月未结算的项目有哪些？
22. XX客户累计欠款多少？
23. 本月已开票金额是多少？
24. 哪些项目的结算方式是固定月租？
25. 上季度各项目租金收入排名？
