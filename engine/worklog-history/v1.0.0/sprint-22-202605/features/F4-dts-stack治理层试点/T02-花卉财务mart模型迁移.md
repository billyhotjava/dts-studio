# T02: 花卉财务 mart 模型迁移到 dbt

**优先级**: P0
**状态**: READY
**依赖**: T01

**仓**: `dts-stack`

## 目标

把 sprint-21 在 dts-copilot 内 PG 落地的 10 张财务 authority/mart 视图，迁移成 dts-stack 的 dbt 模型，分层落到 stg → dwd → dws → ads 四层，复用 dts-stack 已有的工具宏，dbt schema.yml 测试覆盖关键字段。

## 技术设计

### 1. 迁移映射表

参考 `assets/finance-authority-catalog.md` 中的 dbt 模型映射列：

| 当前（dts-copilot PG） | 未来（dts-stack dbt） | 物化策略 | tags | 上游依赖 |
|---|---|---|---|---|
| `authority.finance.settlement_summary` | `xycyl_ads_finance_settlement_summary` | table | `xycyl, xycyl-finance, settlement` | dws_project_monthly + dwd_settlement |
| `authority.finance.receivable_overview` | `xycyl_ads_finance_receivable_overview` | table（每日刷新） | `xycyl, xycyl-finance, ar, kpi` | dws_customer_monthly |
| `authority.finance.pending_receipts_detail` | `xycyl_ads_finance_pending_receipts_detail` | view（实时性高） | `xycyl, xycyl-finance, ar, detail` | dwd_settlement + dwd_collection |
| `authority.finance.pending_payment_approval` | `xycyl_ads_finance_pending_payment_approval` | view | `xycyl, xycyl-finance, payment` | dwd_payment |
| `authority.finance.project_collection_progress` | `xycyl_ads_finance_project_collection_progress` | table | `xycyl, xycyl-finance, collection` | dws_project_monthly |
| `authority.finance.advance_request_status` | `xycyl_ads_finance_advance_request_status` | view | `xycyl, xycyl-finance, advance` | dwd_advance |
| `authority.finance.reimbursement_status` | `xycyl_ads_finance_reimbursement_status` | view | `xycyl, xycyl-finance, reimbursement` | dwd_reimbursement |
| `authority.finance.reimbursement_list` | `xycyl_ads_finance_reimbursement_list` | view | `xycyl, xycyl-finance, reimbursement, detail` | dwd_reimbursement |
| `authority.finance.invoice_reconciliation` | `xycyl_ads_finance_invoice_reconciliation` | view | `xycyl, xycyl-finance, invoice` | dwd_invoice |
| `mart.finance.customer_ar_rank_daily` | `xycyl_ads_finance_customer_ar_rank_daily` | incremental（按 snapshot_date） | `xycyl, xycyl-finance, ar, kpi, snapshot` | dws_customer_monthly |

### 2. 分层职责（参照 dts-stack 已有规范）

#### stg 层职责

- 重命名（`f_settlement_info` → `xycyl_stg_settlement_info`）
- 类型转换（VARCHAR → DATE / NUMERIC，使用 `parse_date_safe` / `parse_numeric_safe` 宏）
- 占位符收敛（用 `nullif_placeholder` 宏）
- 软删过滤（`del_flag = '0'`）
- **不做**业务派生

#### dwd 层职责

- 业务键拼接（如 settlement 拼上 project / customer 信息）
- 状态码 → 中文（用 `dim_*_alias` 维度表 LEFT JOIN，参考现有 `dim_quality_status_alias`）
- 数据质量过滤（如 `status <> -1`）
- **不**跨业务对象做汇总

#### dws 层职责

- 项目 × 月汇总（`xycyl_dws_finance_project_monthly`）
- 客户 × 月汇总（`xycyl_dws_finance_customer_monthly`）
- 应收 / 已收 / 未收的累计计算

#### ads 层职责

- 直接对应 sprint-21 的 mart 视图
- 字段中文化（视图列名直接是 `应收金额` / `已收金额` / `欠款金额` / `账期`）
- KPI 派生（`collection_rate` / `aging_bucket`）

### 3. 模型示例：`xycyl_ads_finance_customer_ar_rank_daily.sql`

```sql
{{ config(
    materialized='incremental',
    unique_key=['customer_id', 'snapshot_date'],
    incremental_strategy='delete+insert',
    tags=['xycyl', 'xycyl-finance', 'ar', 'kpi', 'snapshot']
) }}

WITH customer_monthly AS (
    SELECT * FROM {{ ref('xycyl_dws_finance_customer_monthly') }}
    {% if is_incremental() %}
        WHERE account_period >= date_trunc('month', current_date - interval '90 days')::text
    {% endif %}
),

aging_bucketed AS (
    SELECT
        customer_id,
        customer_name,
        current_date AS snapshot_date,
        SUM(outstanding_amount) AS outstanding_amount,
        SUM(CASE WHEN aging_days <= 30 THEN outstanding_amount ELSE 0 END) AS aging_30d,
        SUM(CASE WHEN aging_days BETWEEN 31 AND 60 THEN outstanding_amount ELSE 0 END) AS aging_60d,
        SUM(CASE WHEN aging_days BETWEEN 61 AND 90 THEN outstanding_amount ELSE 0 END) AS aging_90d_in,
        SUM(CASE WHEN aging_days > 90 THEN outstanding_amount ELSE 0 END) AS aging_90d_over
    FROM customer_monthly
    WHERE outstanding_amount > 0
    GROUP BY customer_id, customer_name
)

SELECT * FROM aging_bucketed
```

### 4. schema.yml 测试约束（关键字段）

参照现有 `fin_schema.yml` 的格式：

```yaml
- name: xycyl_ads_finance_customer_ar_rank_daily
  description: "馨懿诚客户欠款排行（每日快照），含分账龄余额。"
  columns:
    - name: customer_id
      tests:
        - not_null:
            config:
              severity: warn
    - name: customer_name
      description: "客户名（来自 p_customer.name）"
    - name: snapshot_date
      tests:
        - not_null
    - name: outstanding_amount
      description: "欠款金额，单位元，DECIMAL(14,2)"
      tests:
        - dbt_utils.expression_is_true:
            arguments:
              expression: ">= 0"
            config:
              severity: warn
    - name: aging_30d
      description: "30 天内待收金额"
```

### 5. 中间维度表（dim_*_alias）

参考 dts-stack 已有 `dim_quality_status_alias` 模式：

```sql
-- xycyl/dwd/dim_finance_settlement_status_alias.sql
{{ config(materialized='table', tags=['xycyl', 'xycyl-finance', 'dim']) }}

SELECT * FROM (
    VALUES
        (0, '草稿', '未提交'),
        (1, '待审核', '已提交，等待财务审核'),
        (2, '已结算', '财务核算完成'),
        (3, '已收款', '应收金额已全部到账'),
        (4, '部分收款', '应收金额未全部到账'),
        (9, '已作废', '单据作废')
) AS t(status_code, status_name, description)
```

dwd 模型 LEFT JOIN 这个维度表，输出 `*_status_name` 中文字段。

### 6. dbt utils / 宏使用

- `truncate_relation`（已存在）：incremental 模型出错时清空重跑
- `parse_date_safe` / `parse_numeric_safe`：stg 层处理 ODS 脏数据
- `nullif_placeholder`：把 `'-'` / `'NULL'` / `'未知'` 等占位字符串归一化为 NULL
- 新建（如有需要）：`xycyl_aging_bucket(due_date, current_date)` 算账龄分桶

### 7. 业务键 / 维度补充

`stg` / `dwd` 必须 JOIN 项目和客户基础信息：

- `xycyl_stg_project` / `xycyl_dwd_project`（`p_project` → 命名空间内）
- `xycyl_stg_customer` / `xycyl_dwd_customer`（`p_customer` → 命名空间内）

注：项目和客户的同步可以单独在后续 sprint-23 由"项目域 dbt 试点"完成；本 sprint **以 finance 域为主**，项目/客户表暂时直接从 source 引用。

## 影响范围

- `services/dts-dbt/models/xycyl/stg/*.sql` —— 新增 7 张 stg 模型
- `services/dts-dbt/models/xycyl/dwd/*.sql` —— 新增 6 + N 张 dwd 模型 + dim 维度
- `services/dts-dbt/models/xycyl/dws/*.sql` —— 新增 2 张 dws 模型
- `services/dts-dbt/models/xycyl/ads/*.sql` —— 新增 10 张 ads 模型
- `services/dts-dbt/models/xycyl/*.yml` —— 新增 4 个 schema.yml（stg/dwd/dws/ads 各一份或合并）

## 验证

- [ ] `dbt run --select tag:xycyl-finance` 在 dev 环境跑通
- [ ] `dbt test --select tag:xycyl-finance` 全部测试通过（warn 可容忍，error 必须修）
- [ ] `dbt docs generate` 生成的文档中 xycyl 命名空间血缘清晰
- [ ] 与 sprint-21 PG 视图的同名查询结果一致性（行数 100% 一致 / 金额差 < 0.01 元）—— 由 IT-T05 脚本验证

## 完成标准

- [ ] 10 张 mart 视图全部由 dbt 模型实现
- [ ] schema.yml 测试覆盖：not_null（关键键）、accepted_values（状态字段）、unique（粒度）
- [ ] 至少 1 张 incremental 模型（`customer_ar_rank_daily`）跑通
- [ ] 与 sprint-21 PG 视图结果一致性验证通过
