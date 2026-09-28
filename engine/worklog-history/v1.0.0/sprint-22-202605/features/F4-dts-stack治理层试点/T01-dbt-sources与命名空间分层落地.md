# T01: dbt sources 与命名空间 / 分层规范落地

**优先级**: P0
**状态**: READY
**依赖**: F0-T03（口径决策完成）

**仓**: `dts-stack`

## 目标

在 dts-stack 的 `services/dts-dbt/` 项目中建立花卉业务的 `xycyl` 命名空间与五层规范（ods / stg / dwd / dws / ads），并把 `rs_cloud_flower` 库的报花域源表显式声明为 dbt sources。

## 技术设计

### 1. 命名空间隔离规范

参考 dts-stack 现有 `fin_*` / `pm_*` 命名空间，花卉业务命名规则：

| 资源 | 命名 | 示例 |
|---|---|---|
| 模型路径 | `models/xycyl/<layer>/...` | `models/xycyl/ads/xycyl_ads_flowerbiz_settlement_summary.sql` |
| 模型名（文件名 + dbt 模型名） | `xycyl_<layer>_<domain>_<topic>` | `xycyl_ads_flowerbiz_customer_ar_rank_daily` |
| Source name | `xycyl_<layer>` | `xycyl_ods` |
| Schema YAML | `xycyl_<domain>_schema.yml` / `xycyl_<domain>_sources.yml` | `xycyl_flowerbiz_schema.yml` |
| dbt tags | `['xycyl', 'xycyl-flowerbiz', '<topic>']` | `['xycyl', 'xycyl-flowerbiz', 'settlement']` |
| 物化目标 schema | `xycyl_<layer>` | `xycyl_ads`（与基金 / PM 业务的 `public` 隔离） |
| OpenMetadata service | `xycyl_flowerbiz_dbt` | （在 ingestion config 中区分） |

> 完整规范见 `assets/dts-stack-dbt-conventions.md`。

### 2. 目录结构

```
services/dts-dbt/models/xycyl/
├── sources/
│   └── xycyl_ods_flowerbiz_sources.yml   # rs_cloud_flower 报花域源表声明
├── stg/
│   ├── xycyl_flowerbiz_stg.yml           # stg 层 schema 测试
│   ├── xycyl_stg_flower_biz_info.sql
│   ├── xycyl_stg_flower_biz_item.sql
│   ├── xycyl_stg_flower_biz_item_detailed.sql
│   ├── xycyl_stg_change_info.sql
│   ├── xycyl_stg_recovery_info.sql
│   ├── xycyl_stg_recovery_info_item.sql
│   ├── xycyl_stg_flower_rent_time_log.sql
│   ├── xycyl_stg_project.sql             # 项目基础（最小可用）
│   ├── xycyl_stg_customer.sql
│   ├── xycyl_stg_position.sql
│   └── xycyl_stg_personnel.sql
├── dwd/
│   ├── xycyl_flowerbiz_dwd.yml
│   ├── xycyl_dwd_flowerbiz_main.sql
│   ├── xycyl_dwd_flowerbiz_item.sql
│   ├── xycyl_dwd_flowerbiz_change.sql
│   ├── xycyl_dwd_flowerbiz_recovery.sql
│   ├── xycyl_dwd_flowerbiz_rent_time_change.sql
│   ├── xycyl_dim_flowerbiz_status_alias.sql       # 7 状态 → 中文
│   ├── xycyl_dim_flowerbiz_biztype_alias.sql      # 13 bizType → 中文 + biz_category + amount_direction
│   ├── xycyl_dim_flowerbiz_recovery_type_alias.sql # 3 回收去处
│   └── xycyl_dim_flowerbiz_change_type_alias.sql  # 4 变更类型
├── dws/
│   ├── xycyl_flowerbiz_dws.yml
│   ├── xycyl_dws_flowerbiz_project_monthly.sql
│   ├── xycyl_dws_flowerbiz_customer_monthly.sql
│   └── xycyl_dws_flowerbiz_curing_user_monthly.sql
└── ads/
    ├── xycyl_flowerbiz_ads.yml
    ├── xycyl_ads_flowerbiz_lease_summary.sql
    ├── xycyl_ads_flowerbiz_lease_detail.sql
    ├── xycyl_ads_flowerbiz_pending.sql
    ├── xycyl_ads_flowerbiz_sale_summary.sql
    ├── xycyl_ads_flowerbiz_baddebt_summary.sql
    ├── xycyl_ads_flowerbiz_change_log.sql
    ├── xycyl_ads_flowerbiz_recovery_detail.sql
    └── xycyl_ads_flowerbiz_curing_workload.sql
```

> 本 task 只建 `sources/` + 占位空模型文件 + schema.yml 骨架。具体 stg/dwd/dws/ads SQL 在 T02 实现。

### 3. xycyl_ods_flowerbiz_sources.yml 示例

参照现有 `fin_sources.yml` / `ods_sources.yml`：

```yaml
version: 2

sources:
  - name: "xycyl_ods"
    schema: "rs_cloud_flower"   # 直接读 ODS 镜像（由 Addax / dts-ingestion 同步入湖）
    description: "馨懿诚绿植租摆报花域 ODS 源表。ODS 只承载入湖结果与原始口径，业务语义统一经 STG/DWD 收敛。"
    tables:
      - name: "flower_biz_info"
        identifier: "t_flower_biz_info"
        description: "报花单主表。每行 = 一张报花单（项目 × bizType 实例）。"
        columns:
          - name: "id"
          - name: "code"
            description: "单据编号（业务用）"
          - name: "biz_type"
            description: "业务类型枚举：1 换花 / 2 加花 / 3 减花 / 4 调花 / 6 坏账 / 7 售花 / 8 赠花 / 10 配料 / 11 加盆架 / 12 减盆架"
          - name: "status"
            description: "报花单状态：-1 作废 / 1 审核中 / 2 备货中 / 3 核算中 / 4 待结算 / 5 已结束 / 20 草稿 / 21 驳回"
          - name: "project_id"
            description: "p_project.id"
          - name: "apply_user_id"
            description: "申请人（养护人 / 项目经理 / 业务经理）"
          - name: "apply_time"
            description: "申请时间"
          - name: "plan_finish_time"
            description: "计划完工时间"
          - name: "settlement_time"
            description: "结算完成时间（可能为 NULL）"
          - name: "biz_total_rent"
            description: "报花单总租金（金额方向因 bizType 而异；详见 dim_biztype_alias）"
          - name: "biz_total_cost"
            description: "成本"
          - name: "total_amount"
            description: "总金额（销售场景用）"
          - name: "examine_user_id"
            description: "审核人"
          - name: "sign_user_id"
            description: "签字人"
          - name: "curing_user_id"
            description: "养护人"
          - name: "del_flag"
          - name: "create_time"
          - name: "update_time"

      - name: "flower_biz_item"
        identifier: "t_flower_biz_item"
        description: "报花明细。每行 = 一个摆位上的一棵绿植。多对一至 t_flower_biz_info。"
        columns:
          - name: "id"
          - name: "flower_biz_id"
            description: "t_flower_biz_info.id（软外键）"
          - name: "position_id"
            description: "p_position.id 摆位"
          - name: "biz_type"
            description: "本明细的业务类型（与主表 biz_type 一致或子类型）"
          - name: "status"
            description: "明细状态（在多个 service 同时写，状态竞态）"
          - name: "good_price_id"
            description: "商品价格 ID（决定 rent / cost）"
          - name: "good_name"
          - name: "plant_number"
            description: "数量（加花 +、减花 -）"
          - name: "rent"
            description: "单棵月租金"
          - name: "cost"
          - name: "put_time"
          - name: "start_time"
            description: "起租时间（可被 t_flower_rent_time_log 事后修改）"
          - name: "end_time"
            description: "停租时间"
          - name: "net_receipts_number"
            description: "回收时净收回数量"
          - name: "frm_loss_number"
            description: "回收时报损数量"
          - name: "buyback_number"
            description: "回收时回购数量"
          - name: "keep_number"
            description: "回收时留用数量"
          - name: "del_flag"
          - name: "create_time"
          - name: "update_time"

      - name: "flower_biz_item_detailed"
        identifier: "t_flower_biz_item_detailed"
        description: "报花明细子项。一对多至 t_flower_biz_item。关联实摆 p_project_green_item。"

      - name: "change_info"
        identifier: "t_change_info"
        description: "变更单。报花单状态≥4 时的变更记录（金额/起租期/规格变更）。"
        columns:
          - name: "id"
          - name: "code"
          - name: "biz_id"
            description: "t_flower_biz_info.id（软外键）"
          - name: "change_type"
            description: "1 销售金额变更 / 2 库房物品类型变更 / 3 成本变更 / 4 起租减租变更"
          - name: "before_total_amount"
          - name: "after_total_amount"
          - name: "before_settlement_time"
          - name: "after_settlement_time"
          - name: "status"
            description: "1 确认中 / 2 已结束 / -1 作废"

      - name: "recovery_info"
        identifier: "t_recovery_info"
        description: "回收单主表。报花单结束后的绿植回收流程。"
        columns:
          - name: "id"
          - name: "biz_info_id"
            description: "t_flower_biz_info.id"
          - name: "distribution_user_id"
          - name: "recovery_user_id"
          - name: "store_house_id"
          - name: "plan_recovery_time"
          - name: "recovery_time"
          - name: "status"
            description: "1 待回收 / 2 确认入库 / 3 已结束"

      - name: "recovery_info_item"
        identifier: "t_recovery_info_item"
        description: "回收明细。一对多至 t_recovery_info。"
        columns:
          - name: "recovery_info_id"
          - name: "biz_item_id"
            description: "t_flower_biz_item.id"
          - name: "recovery_type"
            description: "1 报损 / 2 回购 / 3 留用"
          - name: "recovery_number"
          - name: "real_recovery_number"
          - name: "good_cost"

      - name: "flower_rent_time_log"
        identifier: "t_flower_rent_time_log"
        description: "租期变更日志。记录 start_time / end_time 的事后修改。"
        columns:
          - name: "biz_id"
          - name: "rent_time_type"
            description: "1 起租 / 2 减租"
          - name: "old_rent_time"
          - name: "new_rent_time"
```

### 3.1 基础维度 sources（项目 / 客户 / 摆位 / 人员）

```yaml
  - name: "xycyl_ods_basic"
    schema: "rs_cloud_flower"
    description: "馨懿诚绿植租摆基础数据 ODS 源表。本 sprint 仅最小可用版本，sprint-26 完整治理。"
    tables:
      - name: "project"
        identifier: "p_project"
      - name: "customer"
        identifier: "p_customer"
      - name: "position"
        identifier: "p_position"
      - name: "personnel"
        identifier: "u_personnel"
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

- `services/dts-dbt/models/xycyl/sources/xycyl_ods_flowerbiz_sources.yml` —— 新增
- `services/dts-dbt/models/xycyl/{stg,dwd,dws,ads}/*.yml` —— 新增 schema 骨架（占位）
- `services/dts-dbt/models/xycyl/.gitkeep` 等占位文件
- `worklog/v1.0.0/sprint-22-202605/assets/dts-stack-dbt-conventions.md` —— 已存在（归档版本继承），仅微调

## 验证

- [ ] `dbt parse` 通过，`xycyl_ods` 在 `target/manifest.json` 中可见
- [ ] `dbt source freshness --select source:xycyl_ods.*` 可执行（如已入湖则可见 freshness 状态）
- [ ] 命名空间不与现有 `fin_*` / `pm_*` 冲突（`dbt ls --select tag:xycyl-flowerbiz` 返回空 / `tag:finance` 返回原有基金模型）
- [ ] `assets/dts-stack-dbt-conventions.md` 文档评审通过

## 完成标准

- [ ] xycyl 命名空间建立，五层目录就位
- [ ] 报花 ODS sources 显式声明，覆盖 7 张报花关键源表 + 4 张基础维度源表
- [ ] 命名规范文档落地，T02 ~ T05 可基于此实施
- [ ] 不影响 dts-stack 既有业务的 dbt 编译
