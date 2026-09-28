# BG-05: 语义模型基线与视图元数据标注

**优先级**: P1
**状态**: READY
**依赖**: BG-02

## 目标

在 `dts-copilot-analytics` 现有元数据模型上，为 BG-02 建立的业务视图层补齐语义标注，使 NL2SQL 上下文编译可以自动获取视图的业务含义、字段注释和推荐用法。

## 技术设计

### 复用现有模型

- `analytics_table` — 注册视图为"表"，标注业务域归属
- `analytics_field` — 标注视图字段的中文名、数据类型、是否维度/度量
- `analytics_synonym` — 关联同义词（用户自然语言 → 视图字段名）

### 补充最小语义结构

在 `analytics_table` 上扩展以下字段：

```sql
ALTER TABLE analytics_table ADD COLUMN IF NOT EXISTS
    semantic_domain VARCHAR(32);     -- 业务域：project / flowerbiz / settlement / task / curing / pendulum

ALTER TABLE analytics_table ADD COLUMN IF NOT EXISTS
    default_time_field VARCHAR(128); -- 默认时间字段名

ALTER TABLE analytics_table ADD COLUMN IF NOT EXISTS
    default_sort_field VARCHAR(128); -- 默认排序字段名

ALTER TABLE analytics_table ADD COLUMN IF NOT EXISTS
    semantic_description TEXT;       -- 业务描述（供模型理解该视图的用途）
```

### 视图元数据标注示例

| 视图 | semantic_domain | default_time_field | semantic_description |
|------|----------------|-------------------|---------------------|
| v_project_overview | project | project_start_time | 项目总览视图，包含项目、合同、客户信息及当前绿植和租金统计 |
| v_flower_biz_detail | flowerbiz | apply_time | 报花业务明细视图，包含所有花卉业务单据（加花/换花/减花/调花等）及其项目、摆位关联 |
| v_project_green_current | project | pose_time | 当前在摆绿植清单，仅包含状态为"摆放中"的绿植 |
| v_monthly_settlement | settlement | settlement_month | 月度结算视图，包含各项目各月的应收/已收/未收金额 |
| v_task_progress | task | launch_time | 任务进度视图，包含所有日常任务及其完成率 |
| v_curing_coverage | curing | curing_month | 养护覆盖率视图，按月统计各养护人的养护覆盖情况 |
| v_pendulum_progress | pendulum | applicant_date | 初摆进度视图，包含初摆申请及预算/实际花费 |

### 字段级标注

每个视图字段在 `analytics_field` 中标注：

- `display_name` — 中文显示名
- `field_role` — 维度(dimension) / 度量(measure) / 属性(attribute)
- `is_filterable` — 是否常用筛选字段
- `sample_values` — 示例值（帮助模型理解字段含义）

### 同义词关联

关键同义词入库 `analytics_synonym`：

| 用户说法 | 映射字段 | 所属视图 |
|----------|---------|---------|
| 养护人 | curing_user_name | v_project_green_current, v_curing_coverage |
| 项目经理 | manager_name | v_project_overview, v_flower_biz_detail |
| 业务经理 | biz_user_name | v_project_overview |
| 租金 | total_rent | v_project_overview, v_monthly_settlement |
| 应收 | receivable_amount | v_monthly_settlement |
| 欠款 | outstanding_amount | v_monthly_settlement |

## 与 metadata sync 兼容

- 现有 metadata sync 可继续同步原始业务表
- 视图作为额外数据源同步，不覆盖原始表元数据
- 语义扩展字段为可选，不影响现有流程

## 完成标准

- [ ] `analytics_table` 扩展字段 DDL（Liquibase changeset）
- [ ] 7 个视图元数据标注完成
- [ ] 视图字段级标注完成（display_name, field_role）
- [ ] 同义词入库
- [ ] metadata sync 可正常同步视图
- [ ] NL2SQL 上下文编译可读取语义标注
