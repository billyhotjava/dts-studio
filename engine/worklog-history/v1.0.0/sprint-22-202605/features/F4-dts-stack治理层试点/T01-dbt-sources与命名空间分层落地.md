# T01: dbt sources 与命名空间 / 分层规范落地

**优先级**: P0
**状态**: READY
**依赖**: F1-T01

**仓**: `dts-stack`

## 目标

在 dts-stack 的 `services/dts-dbt/` 项目中建立花卉业务的 `xycyl` 命名空间与五层规范（ods / stg / dwd / dws / ads），并把 `rs_cloud_flower` 库的财务源表显式声明为 dbt sources。

## 技术设计

### 1. 命名空间隔离规范

参考 dts-stack 现有 `fin_*` / `pm_*` 命名空间，花卉业务命名规则：

| 资源 | 命名 | 示例 |
|---|---|---|
| 模型路径 | `models/xycyl/<layer>/...` | `models/xycyl/ads/xycyl_ads_finance_settlement_summary.sql` |
| 模型名（文件名 + dbt 模型名） | `xycyl_<layer>_<domain>_<topic>` | `xycyl_ads_finance_customer_ar_rank_daily` |
| Source name | `xycyl_<layer>` | `xycyl_ods` |
| Schema YAML | `xycyl_<domain>_schema.yml` / `xycyl_<domain>_sources.yml` | `xycyl_finance_schema.yml` |
| dbt tags | `['xycyl', 'xycyl-finance', '<topic>']` | `['xycyl', 'xycyl-finance', 'settlement']` |
| 物化目标 schema | `xycyl_<layer>` | `xycyl_ads`（与基金 / PM 业务的 `public` 隔离） |
| OpenMetadata service | `xycyl_finance_dbt` | （在 ingestion config 中区分） |

> 完整规范见 `assets/dts-stack-dbt-conventions.md`。

### 2. 目录结构

```
services/dts-dbt/models/xycyl/
├── sources/
│   └── xycyl_ods_sources.yml         # rs_cloud_flower 财务源表声明
├── stg/
│   ├── xycyl_finance_stg.yml         # stg 层 schema 测试
│   ├── xycyl_stg_settlement_info.sql
│   ├── xycyl_stg_collection_record.sql
│   ├── xycyl_stg_pay_record.sql
│   ├── xycyl_stg_advance_info.sql
│   ├── xycyl_stg_expense_account_info.sql
│   ├── xycyl_stg_invoice_info.sql
│   └── xycyl_stg_month_accounting.sql
├── dwd/
│   ├── xycyl_finance_dwd.yml
│   ├── xycyl_dwd_finance_settlement.sql
│   ├── xycyl_dwd_finance_collection.sql
│   ├── xycyl_dwd_finance_payment.sql
│   ├── xycyl_dwd_finance_advance.sql
│   ├── xycyl_dwd_finance_reimbursement.sql
│   ├── xycyl_dwd_finance_invoice.sql
│   └── dim_finance_*_alias.sql       # 状态码 → 中文映射
├── dws/
│   ├── xycyl_finance_dws.yml
│   ├── xycyl_dws_finance_project_monthly.sql
│   └── xycyl_dws_finance_customer_monthly.sql
└── ads/
    ├── xycyl_finance_ads.yml
    ├── xycyl_ads_finance_settlement_summary.sql
    ├── xycyl_ads_finance_receivable_overview.sql
    ├── xycyl_ads_finance_pending_receipts_detail.sql
    ├── xycyl_ads_finance_pending_payment_approval.sql
    ├── xycyl_ads_finance_project_collection_progress.sql
    ├── xycyl_ads_finance_advance_request_status.sql
    ├── xycyl_ads_finance_reimbursement_status.sql
    ├── xycyl_ads_finance_reimbursement_list.sql
    ├── xycyl_ads_finance_invoice_reconciliation.sql
    └── xycyl_ads_finance_customer_ar_rank_daily.sql
```

> 本 task 只建 `sources/` + 占位空模型文件 + schema.yml 骨架。具体 stg/dwd/dws/ads SQL 在 T02 实现。

### 3. xycyl_ods_sources.yml 示例

参照现有 `fin_sources.yml` / `ods_sources.yml`：

```yaml
version: 2

sources:
  - name: "xycyl_ods"
    schema: "rs_cloud_flower"   # 直接读 ODS 镜像（由 Addax / dts-ingestion 同步入湖）
    description: "馨懿诚绿植租摆财务 ODS 源表。ODS 只承载入湖结果与原始口径，业务语义统一经 STG/DWD 收敛。"
    tables:
      - name: "settlement_info"
        identifier: "f_settlement_info"
        description: "结算单主表。每行 = 项目 × 账期 × 客户的一次结算实例。"
        columns:
          - name: "id"
          - name: "project_id"
            description: "p_project.id 外键"
          - name: "customer_id"
          - name: "account_period"
            description: "账期 YYYY-MM"
          - name: "total_rent"
            description: "月租金合计"
          - name: "receivable_amount"
          - name: "received_amount"
          - name: "outstanding_amount"
          - name: "status"
            description: "结算状态枚举：0=草稿 / 1=待审 / 2=已结算 / 3=已收款 / 4=部分收款 / 9=已作废"
          - name: "del_flag"
          - name: "create_time"
          - name: "update_time"

      - name: "collection_record"
        identifier: "a_collection_record"
        description: "回款记录。每行 = 一次到账。"
        # ...

      - name: "pay_record"
        identifier: "f_pay_record"
        description: "付款记录。每行 = 一次出账（含审批流转节点）。"
        # ...

      - name: "advance_info"
        identifier: "t_advance_info"
        description: "员工预支单。"
        # ...

      - name: "expense_account_info"
        identifier: "a_expense_account_info"
        description: "报销单主表。"
        # ...

      - name: "invoice_info"
        identifier: "a_invoice_info"
        description: "客户发票主表。"
        # ...

      - name: "month_accounting"
        identifier: "a_month_accounting"
        description: "月度账务汇总（项目 × 客户 × 账期粒度）。"
        # ...
```

### 4. 与 dts-stack 现有 fin_sources 的关系

dts-stack 现有 `fin_sources.yml` 是基金管理业务的源（`ods_finance_own_fund` / `ods_finance_project_fund` / `ods_finance_aux_balance`）。两者：

- 不同 source name：`xycyl_ods` vs `fin_ods`
- 不同 schema：`rs_cloud_flower` vs `public`（基金业务在 dts-stack 自有库）
- 不同表名前缀：花卉是 `f_*` / `a_*` / `t_*` ；基金是 `ods_finance_*`
- **不会重叠**

### 5. 入湖（ODS）边界

本 task **不**建 Addax / dts-ingestion 的同步任务。前提是：

- `rs_cloud_flower` 库已经被 dts-ingestion / Addax 镜像入 ODS（如有 dwh 专用库）—— **本 task 实施时先确认这件事**
- 若未入湖，dbt sources 直接指向 `rs_cloud_flower` 业务库（read-only），不强求 ODS 物化镜像；T02 stg 模型从 source 读时即穿透到业务库
- 若已入湖（推荐），sources 指向 ODS 库（`xycyl_ods` schema），减少业务库压力

> 决策点：T01 实施开始前需先确认 ODS 入湖现状；如果决定入湖，开 sprint-22 的二级任务 T01.5（不在本 sprint 范围）。

## 影响范围

- `services/dts-dbt/models/xycyl/sources/xycyl_ods_sources.yml` —— 新增
- `services/dts-dbt/models/xycyl/{stg,dwd,dws,ads}/*.yml` —— 新增 schema 骨架（占位）
- `services/dts-dbt/models/xycyl/.gitkeep` 等占位文件
- `worklog/v1.0.0/sprint-22-202605/assets/dts-stack-dbt-conventions.md` —— 新增

## 验证

- [ ] `dbt parse` 通过，`xycyl_ods_sources` 在 `target/manifest.json` 中可见
- [ ] `dbt source freshness --select source:xycyl_ods.*` 可执行（如已入湖则可见 freshness 状态）
- [ ] 命名空间不与现有 `fin_*` / `pm_*` 冲突（`dbt ls --select tag:xycyl-finance` 返回空 / `tag:finance` 返回原有基金模型）
- [ ] `assets/dts-stack-dbt-conventions.md` 文档评审通过

## 完成标准

- [ ] xycyl 命名空间建立，五层目录就位
- [ ] 财务 ODS sources 显式声明，至少覆盖 7 张关键源表
- [ ] 命名规范文档落地，T02 ~ T05 可基于此实施
- [ ] 不影响 dts-stack 既有业务的 dbt 编译
