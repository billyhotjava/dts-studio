# T02: 报花 mart 模型实现（stg → dwd → dws → ads）

**优先级**: P0
**状态**: READY
**依赖**: T01

**仓**: `dts-stack`

## 目标

在 dts-stack dbt `models/xycyl/` 命名空间下从 0 实现报花域完整 5 层模型，覆盖 13 种 bizType 和 7 个状态。最终产出 8 张 ads mart，可被 dts-copilot 通过 datasource 直读。

## 技术设计

### 0. 5 个口径决策（来自 F0-T03，是本 task 的硬前置）

实施前必须拿到下列决策的明确结论（否则模型设计无法定稿）：

| # | 决策 | 推荐值 | 影响范围 |
|---|---|---|---|
| 1 | 报花的"业务时间" | `start_lease_time`（起租）—— 月度归属用 | 所有 dws 按月 GROUP BY 的字段 |
| 2 | 13 bizType 分组方式 | 按"租赁/销售/坏账/调拨/辅料"5 类 | dwd 加 `biz_category` 字段；ads 拆分模型 |
| 3 | 异步联动滞后处理 | 分两个 ads（含未结算 / 仅已结算） | mart 命名加 `_pending` / `_settled` 后缀 |
| 4 | 金额符号规范化 | 统一变正 + `amount_direction` 字段（IN/OUT） | dwd 层 `ABS(bizTotalRent)` + 标记方向 |
| 5 | 销售/赠花是否进财务 | 独立 ads + 跨 mart UNION 视图 | `xycyl_ads_flowerbiz_lease_*` + `xycyl_ads_flowerbiz_sale_*` |

> 本文档**假设按推荐值**实施。如业务方决策不同，所有 mart 设计需相应调整。

### 1. 8 张 ads mart 模型清单

| # | mart 名 | 物化策略 | bizType 范围 | 关键字段 | 主要回答的问句 |
|---|---|---|---|---|---|
| 1 | `xycyl_ads_flowerbiz_lease_summary` | table（每日） | 1/2/3/4 (换/加/减/调) | project, customer, biz_month, lease_in_amount, lease_out_amount, net_lease | "本月加摆撤摆净增减"、"项目租金变动" |
| 2 | `xycyl_ads_flowerbiz_lease_detail` | view | 1/2/3/4 | flower_biz_id, biz_type_name, status_name, project, position, plant_name, plant_number, amount, apply_time, finish_time | "万象城最近的报花单"、"养护人 X 经手的报花" |
| 3 | `xycyl_ads_flowerbiz_pending` | view | 全部 | flower_biz_id, status_name, days_in_status, project, applicant, amount | "审核中超过 7 天"、"待结算的报花单" |
| 4 | `xycyl_ads_flowerbiz_sale_summary` | table（每日） | 7/8 (售/赠) | project, customer, biz_month, sale_amount, gift_amount, count_sale, count_gift | "本月销售金额"、"赠送了多少花" |
| 5 | `xycyl_ads_flowerbiz_baddebt_summary` | table（每月） | 6 (坏账) | project, customer, biz_month, baddebt_amount, baddebt_type_name | "本月坏账"、"哪些客户坏账多" |
| 6 | `xycyl_ads_flowerbiz_change_log` | view | 全部（来自 t_change_info） | flower_biz_id, change_type_name, before_amount, after_amount, before_lease_time, after_lease_time, change_time | "起租期变更记录"、"金额变更" |
| 7 | `xycyl_ads_flowerbiz_recovery_detail` | view | 3/4 (减/调) 触发的回收 | recovery_id, flower_biz_id, recovery_type_name, recovery_number, real_recovery_number, store_house, recovery_user | "本月回收清单"、"报损了多少花" |
| 8 | `xycyl_ads_flowerbiz_curing_workload` | table（每月） | 全部 | curing_user, biz_month, biz_count, biz_count_by_type | "养护人 X 本月经手多少报花单" |

### 2. 分层职责

#### stg 层（7 张 + 4 张基础数据）

| stg 模型 | 来源 source | 职责 |
|---|---|---|
| `xycyl_stg_flower_biz_info` | `xycyl_ods.flower_biz_info` (`t_flower_biz_info`) | 重命名、`del_flag='0'` 过滤、用 `parse_date_safe` 转时间 |
| `xycyl_stg_flower_biz_item` | `xycyl_ods.flower_biz_item` | 同上 + `bizType` 字段保留 |
| `xycyl_stg_flower_biz_item_detailed` | `xycyl_ods.flower_biz_item_detailed` | 同上 |
| `xycyl_stg_change_info` | `xycyl_ods.change_info` | 同上 |
| `xycyl_stg_recovery_info` | `xycyl_ods.recovery_info` | 同上 |
| `xycyl_stg_recovery_info_item` | `xycyl_ods.recovery_info_item` | 同上 |
| `xycyl_stg_flower_rent_time_log` | `xycyl_ods.flower_rent_time_log` | 同上，时间字段必清洗 |
| `xycyl_stg_project` | `xycyl_ods.project` (`p_project`) | 项目基础（仅必要字段） |
| `xycyl_stg_customer` | `xycyl_ods.customer` (`p_customer`) | 客户基础 |
| `xycyl_stg_position` | `xycyl_ods.position` (`p_position`) | 摆位基础 |
| `xycyl_stg_personnel` | `xycyl_ods.personnel` (`u_personnel`) | 养护人 / 项目经理 / 业务经理 / 销售 |

**禁止**：业务派生、JOIN 多表、状态翻译。

#### dwd 层（5 张 + 4 张 dim 维度）

| dwd 模型 | 来源 | 职责 |
|---|---|---|
| `xycyl_dwd_flowerbiz_main` | stg_flower_biz_info + dim 表 | JOIN 项目/客户基础 + 翻译 status / bizType / biz_category |
| `xycyl_dwd_flowerbiz_item` | stg_flower_biz_item + main | JOIN 主单 + 翻译状态 + 计算 `amount_direction` |
| `xycyl_dwd_flowerbiz_change` | stg_change_info | 翻译 change_type；变更前后金额展开 |
| `xycyl_dwd_flowerbiz_recovery` | stg_recovery_info + stg_recovery_info_item | JOIN 主从；翻译 recovery_type |
| `xycyl_dwd_flowerbiz_rent_time_change` | stg_flower_rent_time_log | 翻译 rent_time_type；展开 old / new |
| **dim 维度（重要）**：| | |
| `xycyl_dim_flowerbiz_status_alias` | (硬编码 7 状态) | -1/1/2/3/4/5/20/21 → 中文 + is_active 标记 |
| `xycyl_dim_flowerbiz_biztype_alias` | (硬编码 13 类型) | 1-12 → 中文 + biz_category（lease/sale/gift/baddebt/material）+ amount_direction（in/out/neutral）|
| `xycyl_dim_flowerbiz_recovery_type_alias` | (硬编码 3 类) | 1 报损 / 2 回购 / 3 留用 |
| `xycyl_dim_flowerbiz_change_type_alias` | (硬编码 4 类) | 销售金额变更 / 库房物品类型变更 / 成本变更 / 起租减租变更 |

**禁止**：跨业务对象汇总（不在 dwd 做 SUM）。

#### dws 层（3 张）

| dws 模型 | 粒度 | 关键字段 |
|---|---|---|
| `xycyl_dws_flowerbiz_project_monthly` | 项目 × 月 × biz_category | biz_count, total_amount_in, total_amount_out, net_amount |
| `xycyl_dws_flowerbiz_customer_monthly` | 客户 × 月 × biz_category | 同上 |
| `xycyl_dws_flowerbiz_curing_user_monthly` | 养护人 × 月 × biz_category | biz_count, total_amount |

**强制规则**：所有 dws 必须按 `biz_category` 分组，禁止跨 category SUM。

#### ads 层（8 张，见上表）

直接对应业务问句，字段名中文化（`项目` / `客户` / `净增金额` / `审核状态`）。

### 3. dim 维度示例：bizType 翻译

```sql
-- xycyl/dwd/xycyl_dim_flowerbiz_biztype_alias.sql
{{ config(materialized='table', tags=['xycyl', 'xycyl-flowerbiz', 'dim']) }}

SELECT * FROM (
    VALUES
        (1,  '换花',     'lease',    'neutral'),
        (2,  '加花/加摆', 'lease',    'in'),
        (3,  '减花/撤摆', 'lease',    'out'),
        (4,  '调花/调拨', 'lease',    'neutral'),
        (6,  '坏账',     'baddebt',  'out'),
        (7,  '售花/销售', 'sale',     'in'),
        (8,  '赠花',     'gift',     'neutral'),
        (10, '配料',     'material', 'in'),
        (11, '加盆架',   'material', 'in'),
        (12, '减盆架',   'material', 'out')
) AS t(biz_type_code, biz_type_name, biz_category, amount_direction)
```

### 4. 关键 ads 模型示例：`xycyl_ads_flowerbiz_lease_summary`

```sql
{{ config(
    materialized='table',
    tags=['xycyl', 'xycyl-flowerbiz', 'lease', 'summary', 'monthly']
) }}

WITH project_lease AS (
    SELECT
        project_id,
        project_name,
        customer_id,
        customer_name,
        DATE_FORMAT(start_lease_time, '%Y-%m') AS biz_month,
        SUM(CASE WHEN amount_direction = 'in'  THEN amount ELSE 0 END) AS lease_in_amount,
        SUM(CASE WHEN amount_direction = 'out' THEN amount ELSE 0 END) AS lease_out_amount,
        SUM(CASE WHEN amount_direction = 'in'  THEN amount ELSE 0 END)
          - SUM(CASE WHEN amount_direction = 'out' THEN amount ELSE 0 END) AS net_lease,
        COUNT(*) AS biz_count
    FROM {{ ref('xycyl_dwd_flowerbiz_item') }}
    WHERE biz_category = 'lease'
      AND status_code = 5
      AND start_lease_time IS NOT NULL
    GROUP BY project_id, project_name, customer_id, customer_name, biz_month
)
SELECT
    project_name AS 项目,
    customer_name AS 客户,
    biz_month AS 业务月份,
    ROUND(lease_in_amount, 2) AS 加摆金额,
    ROUND(lease_out_amount, 2) AS 撤摆金额,
    ROUND(net_lease, 2) AS 净增金额,
    biz_count AS 报花单数
FROM project_lease
```

### 5. schema.yml 测试约束（关键）

```yaml
- name: xycyl_ads_flowerbiz_lease_summary
  description: "馨懿诚报花租赁汇总（项目 × 月）。仅含 bizType=1/2/3/4 已结束单。"
  columns:
    - name: 项目
      tests:
        - not_null
    - name: 业务月份
      tests:
        - not_null
    - name: 加摆金额
      tests:
        - dbt_utils.expression_is_true:
            arguments:
              expression: ">= 0"
    - name: 撤摆金额
      tests:
        - dbt_utils.expression_is_true:
            arguments:
              expression: ">= 0"
    - name: 报花单数
      tests:
        - dbt_utils.expression_is_true:
            arguments:
              expression: "> 0"
```

### 6. 6 个真实陷阱在模型设计中的应对

| 陷阱 | 模型设计应对 |
|---|---|
| 异步触发 | 每张 ads 加 `data_freshness_minutes` 元数据；OpenMetadata 显示数据时效 |
| 软外键 | dwd 全部 LEFT JOIN（不 INNER）；schema.yml `relationships` 测试用 `severity: warn` |
| 状态竞态 | incremental 模型用 `update_time` 不用 `status` 作 unique_key |
| 金额符号 | dim 表带 `amount_direction`，dwd 用 ABS + 方向；ads 按方向 SUM |
| 链路分叉 | 拆分 lease / sale / baddebt 三个独立 ads；UNION 视图作可选总览 |
| 时间不可靠 | 业务时间字段从 F0-T03 决策（默认 `start_lease_time`）；`create_time` 仅用于审计 |

## 影响范围

- `services/dts-dbt/models/xycyl/stg/*.sql` —— 新增 7+4 张 stg 模型（报花 7 + 项目/客户/摆位/人员 4）
- `services/dts-dbt/models/xycyl/dwd/*.sql` —— 新增 5 张 dwd 模型 + 4 张 dim 维度
- `services/dts-dbt/models/xycyl/dws/*.sql` —— 新增 3 张 dws 模型
- `services/dts-dbt/models/xycyl/ads/*.sql` —— 新增 8 张 ads 模型
- `services/dts-dbt/models/xycyl/*.yml` —— 新增 4 个 schema.yml（stg/dwd/dws/ads 各一份）

## 验证

- [ ] `dbt run --select tag:xycyl-flowerbiz` 在 dev 环境跑通
- [ ] `dbt test --select tag:xycyl-flowerbiz` 全部测试通过（warn 可容忍，error 必须修）
- [ ] `dbt docs generate` 生成文档中报花血缘清晰（ods → stg → dwd → dws → ads）
- [ ] 8 张 ads 模型各能用 1 条业务问句对照 adminweb 现网页面，结果一致（行数 / 数量 / 金额）
- [ ] 13 种 bizType 在 `xycyl_dim_flowerbiz_biztype_alias` 中全覆盖
- [ ] 7 个状态在 `xycyl_dim_flowerbiz_status_alias` 中全覆盖

## 完成标准

- [ ] 报花域 5 层模型 + 4 张 dim 维度 + 8 张 ads mart 全部实现
- [ ] schema.yml 测试覆盖：粒度 unique、关键字段 not_null、accepted_values（状态/bizType）、表达式（金额非负）
- [ ] 至少 1 张 incremental 模型（如 `xycyl_dws_flowerbiz_project_monthly`）跑通
- [ ] 与 adminweb 现网页面结果一致性验证通过
