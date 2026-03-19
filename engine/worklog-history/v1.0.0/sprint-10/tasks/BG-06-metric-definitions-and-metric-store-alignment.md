# BG-06: 业务语义包（项目履约 + 现场业务）

**优先级**: P1
**状态**: READY
**依赖**: BG-05

## 目标

为项目履约和现场业务两个主题域建立统一的业务语义包，包含对象字典、同义词集、few-shot 问句样本和 eval case，作为 NL2SQL 上下文编译的输入。

## 设计说明

原方案将两个主题域拆为独立任务（BG-03/BG-04），合并原因：
- 两个域共享大量基础实体（project, position, goods）
- 视图层已承担了原来语义包的表结构映射职责
- 本任务聚焦于"帮助模型理解业务语言"，不再重复表结构定义

## 技术设计

### 1. 对象字典

基于 BG-01 盘点结果，为每个业务对象建立结构化描述：

**项目履约域**
```yaml
objects:
  - name: 项目点
    view: v_project_overview
    description: 客户现场布置绿植的物理地点，是所有业务操作的归属单位
    key_dimensions: [project_name, customer_name, manager_name, project_status_name]
    key_measures: [green_count, total_rent, position_count]
    common_filters: [project_status_name, settlement_type_name, contract_end_date]

  - name: 在摆绿植
    view: v_project_green_current
    description: 当前实际在位的绿植清单，每棵植物有独立的摆位、租金和养护人
    key_dimensions: [project_name, position_name, good_name, curing_user_name]
    key_measures: [good_number, rent, cost]
    common_filters: [project_name, curing_user_name, good_name]

  - name: 月度结算
    view: v_monthly_settlement
    description: 每月每项目的应收/已收/未收金额，是财务查询的核心数据源
    key_dimensions: [project_name, customer_name, settlement_month]
    key_measures: [total_rent, receivable_amount, received_amount, outstanding_amount]
    common_filters: [settlement_status_name, settlement_month, customer_name]
```

**现场业务域**
```yaml
objects:
  - name: 报花业务
    view: v_flower_biz_detail
    description: 所有花卉业务单据（加花/换花/减花/调花等），是现场运营的核心流水
    key_dimensions: [project_name, biz_type_name, biz_status_name, apply_user_name, biz_month]
    key_measures: [plant_number, biz_total_rent, biz_total_cost]
    common_filters: [biz_type_name, biz_status_name, project_name, biz_month]

  - name: 日常任务
    view: v_task_progress
    description: 各类型工作任务（销售/内购/实摆变更/支持/初摆），追踪执行进度
    key_dimensions: [project_name, task_type_name, task_status_name, leading_user_name]
    key_measures: [total_number, finish_number, completion_rate, total_rent]
    common_filters: [task_status_name, task_type_name, project_name]

  - name: 养护覆盖
    view: v_curing_coverage
    description: 各养护人的养护覆盖率统计，按月汇总
    key_dimensions: [project_name, curing_user_name, curing_month]
    key_measures: [curing_count, total_position_count, covered_position_count, coverage_rate]
    common_filters: [curing_user_name, curing_month]

  - name: 初摆
    view: v_pendulum_progress
    description: 新项目首次布置的进度追踪，含预算和实际花费
    key_dimensions: [project_name, pendulum_status_name, applicant_name]
    key_measures: [total_budget_cost, actual_cost, balance_cost, year_rent]
    common_filters: [pendulum_status_name]
```

### 2. Few-shot 问句样本

每个视图配 5-8 个 few-shot（问句 → SQL），覆盖不同查询模式：

**v_flower_biz_detail 示例**
```
Q: 本月加花总共多少次？
A: SELECT count(*) as 加花次数 FROM v_flower_biz_detail WHERE biz_type_name = '加花' AND biz_month = '2026-03'

Q: 上个月哪个项目换花最多？
A: SELECT project_name, count(*) as 换花次数 FROM v_flower_biz_detail WHERE biz_type_name = '换花' AND biz_month = '2026-02' GROUP BY project_name ORDER BY 换花次数 DESC LIMIT 1

Q: 张三发起的报花单有哪些？
A: SELECT biz_code, biz_type_name, project_name, biz_status_name, apply_time FROM v_flower_biz_detail WHERE apply_user_name LIKE '%张三%' ORDER BY apply_time DESC

Q: 紧急的换花单目前什么状态？
A: SELECT biz_code, project_name, biz_status_name, apply_time FROM v_flower_biz_detail WHERE biz_type_name = '换花' AND is_urgent = '是' AND biz_status_name NOT IN ('已完成', '作废') ORDER BY apply_time DESC
```

### 3. Eval Case 矩阵

每个视图 10+ 条 eval case，用于自动化评估 NL2SQL 准确率：

| 问句 | 期望 SQL 关键片段 | 期望列 | 通过条件 |
|------|-------------------|--------|---------|
| 当前有多少在服项目 | `FROM v_project_overview WHERE project_status_name = '正常'` | count | 精确匹配 |
| XX项目有多少绿植 | `FROM v_project_green_current WHERE project_name LIKE '%XX%'` | count/sum | 包含关键词 |
| 上月各项目租金 | `FROM v_monthly_settlement WHERE settlement_month = '...'` | project_name, total_rent | 返回正确月份 |

## 完成标准

- [ ] 两个域的对象字典 JSON 完成
- [ ] 每个视图 5+ 条 few-shot 样本
- [ ] 每个视图 10+ 条 eval case
- [ ] 同义词集覆盖各角色常用说法
- [ ] 语义包可被 NL2SQL 上下文编译服务加载
