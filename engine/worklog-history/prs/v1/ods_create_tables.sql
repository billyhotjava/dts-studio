-- ============================================================
-- 馨懿诚绿植租摆 报花域 ODS 建表 DDL  v3  (2026-05-02)
-- 命名空间: xycyl   |   schema: xycyl_ods
-- 来源: rs_cloud_flower 业务库（MySQL 5.7）入湖镜像
-- 字段全部 varchar — 类型转换由 dbt STG 层处理
--
-- 用法:
--   psql -h <pg-host> -U <user> -d <db> -f ods_create_tables.sql
--
-- 注意:
--   1) 列名严格对齐生产 information_schema.COLUMNS（39.106.43.56:3307 review 后修正）
--   2) 仅 t_flower_biz_info / t_flower_biz_item / t_flower_extra_cost 真有 del_flag
--   3) 项目→客户关系经 p_contract 中转，不是 p_project 直接挂
--   4) 全部 CREATE TABLE IF NOT EXISTS，幂等可重复执行，不破坏已有数据
--   5) 如需重置某张表数据：手工 TRUNCATE TABLE xycyl_ods.ods_xxx
-- ============================================================

CREATE SCHEMA IF NOT EXISTS xycyl_ods;
SET search_path TO xycyl_ods, public;

-- ─── 1. 报花单主表（t_flower_biz_info）── 32k 行 ──────────────
CREATE TABLE IF NOT EXISTS xycyl_ods.ods_flower_biz_info (
    id                    varchar(64),    -- BIGINT PK
    project_id            varchar(64),
    project_name          varchar(255),
    code                  varchar(64),    -- 单据编号
    title                 varchar(512),
    biz_type              varchar(16),    -- 1换/2加/3减/4调/6坏账/7售/8赠/10配料/11加盆架/12减盆架
    apply_use_id          varchar(64),    -- 注意: 字段名是 use_id 不是 user_id
    apply_use_name        varchar(255),
    apply_time            varchar(40),
    status                varchar(16),    -- -1作废/1审核中/2备货中/3核算中/4待结算/5已结束/20草稿/21驳回
    urgent                varchar(16),
    create_by             varchar(64),
    create_time           varchar(40),
    update_by             varchar(64),
    update_time           varchar(40),
    remark                varchar(2000),
    del_flag              varchar(4),     -- '0' 有效；生产观测全部 '0'
    rent_update_type      varchar(16),
    change_flower_type    varchar(16),
    tenant_id             varchar(64),
    lease_term            varchar(16),
    curing_user_id        varchar(64),
    curing_user_name      varchar(255),
    changer_type          varchar(16),
    finish_time           varchar(40),
    project_manage_id     varchar(64),    -- 注意: 字段名 manage 不是 manager
    project_manage_name   varchar(255),
    biz_manage_id         varchar(64),
    biz_manage_name       varchar(255),
    draft_item_json       text,
    reject_reason         varchar(512),
    reject_time           varchar(40),
    reject_user_id        varchar(64),
    reject_user_name      varchar(255),
    bad_debt_type         varchar(16),
    sign_time             varchar(40),
    sign_user_id          varchar(64),
    sign_user_name        varchar(255),
    give_describe         varchar(512),
    sale_describe         varchar(512),
    customer_name         varchar(255),   -- 反范式：直接挂主表（不通过 p_customer FK）
    phone_number          varchar(64),
    address               varchar(512),
    transfer_type         varchar(16),
    source_type           varchar(16),
    print_status          varchar(16),
    print_time            varchar(40),
    print_user_id         varchar(64),
    print_user_name       varchar(255),
    fare                  varchar(40),    -- 运费
    tax_rate              varchar(40),
    plan_finish_time      varchar(40),
    back_reason           varchar(512),
    examine_user_id       varchar(64),
    examine_user_name     varchar(255),
    examine_time          varchar(40),
    rent_discount_ratio   varchar(40),
    labor_cost            varchar(40),
    cleaning_fee          varchar(40),
    total_amount          varchar(40),    -- 销售场景专用（仅 biz_type=7 有值）
    accounting_status     varchar(16),
    biz_total_rent        varchar(40),    -- 已自带正负号；不再乘 amount_direction
    biz_total_cost        varchar(40),
    task_info_id          varchar(64),
    start_lease_time      varchar(40),    -- 起租时间，可被 t_flower_rent_time_log 修改
    custom_hide           varchar(16),
    cut_confirm_status    varchar(16),
    cut_confitm_time      varchar(40),    -- 注意: 生产 typo "confitm" 不是 "confirm"
    total_extra_cost      varchar(40),
    total_extra_price     varchar(40),
    review_user_id        varchar(64),
    review_user_name      varchar(255),
    review_time           varchar(40),
    bear_cost_type        varchar(16),
    settlement_time       varchar(40),
    sales_payment_type    varchar(16),
    document_finish_time  varchar(40),
    batch_code            varchar(32),
    expense_id            varchar(64),    -- sprint-25 财务挂钩
    expense_code          varchar(64),
    task_code             varchar(255),
    task_item_id          varchar(64),
    settle_id             varchar(64),    -- sprint-25 结算挂钩
    settle_code           varchar(64),
    source_system         varchar(64) DEFAULT 'rs_cloud_flower',
    imported_at           timestamp DEFAULT now()
);

-- ─── 2. 报花明细（t_flower_biz_item）── 196k 行 ───────────────
CREATE TABLE IF NOT EXISTS xycyl_ods.ods_flower_biz_item (
    id                          varchar(64),
    flower_biz_id               varchar(64),    -- → ods_flower_biz_info.id
    position_id                 varchar(64),
    position_name               varchar(255),
    position_full_name          varchar(255),
    biz_type                    varchar(16),
    plant_type                  varchar(16),
    status                      varchar(16),
    green_name                  varchar(120),
    good_price_id               varchar(64),
    good_norms                  varchar(255),
    good_specs                  varchar(255),
    good_unit                   varchar(255),
    good_type                   varchar(16),
    project_green_id            varchar(64),
    plant_number                varchar(16),
    rent                        varchar(40),
    cost                        varchar(40),
    put_time                    varchar(40),
    parent_id                   varchar(64),
    combination_to_scene        varchar(16),
    force_change_flowers        varchar(16),
    source                      varchar(255),
    reduction_flower_way        varchar(16),
    create_by                   varchar(64),
    create_time                 varchar(40),
    update_by                   varchar(64),
    update_time                 varchar(40),
    remark                      varchar(2000),
    del_flag                    varchar(4),
    frm_loss_number             varchar(16),
    buyback_number              varchar(16),
    keep_number                 varchar(16),
    confirm_put_user_id         varchar(64),
    confirm_put_time            varchar(40),
    net_receipts_number         varchar(16),
    total_number                varchar(16),
    reject_number               varchar(16),
    transfer_cut_green_id       varchar(64),
    transfer_type               varchar(16),
    transfer_number             varchar(16),
    distribute_purchase_number  varchar(16),
    distribute_base_number      varchar(16),
    real_purchase_number        varchar(16),
    real_purchase_price         varchar(40),
    real_out_number             varchar(16),
    distribute_store_house_id   varchar(64),
    real_out_price              varchar(40),
    start_time                  varchar(40),
    end_time                    varchar(40),
    old_green_id                varchar(64),
    distribute_slow_number      varchar(16),
    distribute_slow_house_id    varchar(64),
    real_slow_price             varchar(40),
    good_number                 varchar(16),
    back_storehouse_id          varchar(64),
    rent_des                    varchar(512),
    bad_debt_reason             varchar(512),
    expense_status              varchar(16),
    sort                        varchar(64),
    finish_number               varchar(16),
    finish_distribute_number    varchar(16),
    source_system               varchar(64) DEFAULT 'rs_cloud_flower',
    imported_at                 timestamp DEFAULT now()
);

-- ─── 3. 报花明细分配表（t_flower_biz_item_detailed）── 225k 行 ────
-- 实际 8 字段，是 item ↔ project_green_item 的分配/出库 junction
-- 25.5% 孤儿（flower_biz_item_id 在 t_flower_biz_item 中查无）— 历史 hard-delete
CREATE TABLE IF NOT EXISTS xycyl_ods.ods_flower_biz_item_detailed (
    id                       varchar(64),
    flower_biz_item_id       varchar(64),    -- → ods_flower_biz_item.id (25.5% 孤儿)
    project_green_item_id    varchar(64),
    status                   varchar(16),
    source                   varchar(16),
    price                    varchar(40),
    allocate_time            varchar(40),
    plan_purchase_info_id    varchar(64),
    source_system            varchar(64) DEFAULT 'rs_cloud_flower',
    imported_at              timestamp DEFAULT now()
);

-- ─── 4. 变更单（t_change_info）── 1180 行 ────────────────────
-- 真实 schema 包含 BEFORE/AFTER 货物完整对（good_price_id/name/type/norms/specs/unit）
-- 没有 del_flag 字段
CREATE TABLE IF NOT EXISTS xycyl_ods.ods_change_info (
    id                       varchar(64),
    code                     varchar(64),
    title                    varchar(255),
    apply_user_id            varchar(64),
    apply_user_name          varchar(255),
    apply_time               varchar(40),
    status                   varchar(16),
    change_type              varchar(16),    -- 实测仅 1/2/3 三个值
    biz_id                   varchar(64),
    change_number            varchar(16),
    before_total_amount      varchar(40),
    after_total_amount       varchar(40),
    before_settlement_time   varchar(40),
    after_settlement_time    varchar(40),
    before_good_price_id     varchar(64),
    before_good_name         varchar(255),
    before_good_type         varchar(16),
    before_good_norms        varchar(255),
    before_good_specs        varchar(255),
    before_good_unit         varchar(16),
    after_good_price_id      varchar(64),
    after_good_name          varchar(255),
    after_good_type          varchar(16),
    after_good_norms         varchar(255),
    after_good_specs         varchar(255),
    after_good_unit          varchar(16),
    confirmed_user_id        varchar(64),
    confirmed_user_name      varchar(255),
    confirmed_time           varchar(40),
    create_by                varchar(64),
    create_time              varchar(40),
    update_time              varchar(40),
    update_by                varchar(64),
    remark                   varchar(512),
    source_system            varchar(64) DEFAULT 'rs_cloud_flower',
    imported_at              timestamp DEFAULT now()
);

-- ─── 5. 回收主表（t_recovery_info）── 2527 行 ────────────────
-- 没有 del_flag 字段
CREATE TABLE IF NOT EXISTS xycyl_ods.ods_recovery_info (
    id                       varchar(64),
    biz_info_id              varchar(64),    -- → ods_flower_biz_info.id
    project_id               varchar(64),
    project_name             varchar(255),
    distribution_user_id     varchar(64),
    distribution_user_name   varchar(255),
    distribution_time        varchar(40),
    recovery_user_id         varchar(64),
    recovery_user_name       varchar(255),
    recovery_time            varchar(40),
    status                   varchar(16),
    store_house_name         varchar(255),
    store_house_id           varchar(64),
    create_by                varchar(64),
    create_time              varchar(40),
    update_by                varchar(64),
    update_time              varchar(40),
    remark                   varchar(512),
    tenant_id                varchar(64),
    plan_recovery_time       varchar(40),
    source_system            varchar(64) DEFAULT 'rs_cloud_flower',
    imported_at              timestamp DEFAULT now()
);

-- ─── 6. 回收明细（t_recovery_info_item）── 20k 行 ────────────
-- 没有 del_flag 字段
CREATE TABLE IF NOT EXISTS xycyl_ods.ods_recovery_info_item (
    id                       varchar(64),
    recovery_info_id         varchar(64),    -- → ods_recovery_info.id
    biz_item_id              varchar(64),    -- → ods_flower_biz_item.id
    goods_price_id           varchar(64),
    good_name                varchar(255),
    good_norms               varchar(255),
    good_specs               varchar(255),
    good_unit                varchar(255),
    good_type                varchar(16),
    recovery_type            varchar(16),    -- 1报损 / 2回购 / 3留用 (实测三值)
    recovery_number          varchar(16),
    real_recovery_number     varchar(16),
    good_cost                varchar(40),
    status                   varchar(16),
    recovery_time            varchar(40),
    source_system            varchar(64) DEFAULT 'rs_cloud_flower',
    imported_at              timestamp DEFAULT now()
);

-- ─── 7. 报花单操作日志（t_flower_biz_log）── 295k 行 ─────────
-- 没有 del_flag、create_time、update_time 字段
-- biz_type 列 98% NULL，不是父表 biz_type — 命名为 log_biz_type 避免误用
CREATE TABLE IF NOT EXISTS xycyl_ods.ods_flower_biz_log (
    id                       varchar(64),
    sorts                    varchar(16),
    biz_type                 varchar(16),    -- 在 STG 重命名为 log_biz_type_raw
    biz_id                   varchar(64),    -- → ods_flower_biz_info.id
    status                   varchar(16),
    operation_title          varchar(255),
    operation_user_id        varchar(64),
    operation_user_name      varchar(255),
    operation_time           varchar(40),
    operation_content        varchar(2000),
    source_system            varchar(64) DEFAULT 'rs_cloud_flower',
    imported_at              timestamp DEFAULT now()
);

-- ─── 8. 报花额外费用（t_flower_extra_cost）── 575 行 ─────────
-- 唯一一个除主表/明细外有 del_flag 的表
CREATE TABLE IF NOT EXISTS xycyl_ods.ods_flower_extra_cost (
    id                       varchar(64),
    biz_id                   varchar(64),    -- → ods_flower_biz_info.id
    biz_type                 varchar(16),
    cost_type                varchar(16),    -- 1运费 / 2人工 / 3税费 / 4垃圾清理 / 5其他
    title                    varchar(255),
    free_amount              varchar(40),    -- 不含税
    price_amount             varchar(40),    -- 含税
    tax_rate                 varchar(40),
    create_by                varchar(64),
    create_time              varchar(40),
    update_by                varchar(64),
    update_time              varchar(40),
    remark                   varchar(2000),
    del_flag                 varchar(4),
    pay_user_id              varchar(64),
    pay_user_name            varchar(255),
    pay_time                 varchar(40),
    expense_id               varchar(64),
    expense_code             varchar(64),
    source_system            varchar(64) DEFAULT 'rs_cloud_flower',
    imported_at              timestamp DEFAULT now()
);

-- ─── 9. 起租期变更日志（t_flower_rent_time_log）── 6k 行 ─────
-- 没有 del_flag、change_reason、update_time 字段
-- rent_time_type 实测两值: 1=起租 / 2=减租
CREATE TABLE IF NOT EXISTS xycyl_ods.ods_flower_rent_time_log (
    id                       varchar(64),
    biz_id                   varchar(64),    -- → ods_flower_biz_info.id
    rent_time_type           varchar(16),
    old_rent_time            varchar(40),
    new_rent_time            varchar(40),
    change_user_id           varchar(64),
    change_user_name         varchar(255),
    change_time              varchar(40),
    source_system            varchar(64) DEFAULT 'rs_cloud_flower',
    imported_at              timestamp DEFAULT now()
);

-- ─── 业务实体支持表（项目/客户）的简化镜像 ────────────────────
-- 注：完整业务实体在 sprint-24 摆放域建模，本 sprint 仅维持下游 JOIN 所需最小列
-- 重要事实：p_project 没有 customer_id；客户经 p_contract 中转
-- sprint-22 用 main.customer_name (反范式) 作为客户维度

CREATE TABLE IF NOT EXISTS xycyl_ods.ods_project (
    id              varchar(64),
    contract_id     varchar(64),    -- → p_contract.id (sprint-23+ 接客户)
    name            varchar(255),
    code            varchar(255),
    abbreviation    varchar(255),
    status          varchar(16),
    type            varchar(16),
    address         varchar(512),
    customer_type   varchar(16),
    biz_user_id     varchar(64),
    manager_id      varchar(64),    -- 注意: 字段名是 manager_id 不是 project_manager_id
    supervisor_id   varchar(64),
    curing_director varchar(64),
    curing_director_name varchar(255),
    start_time      varchar(40),
    end_time        varchar(40),
    settle_start_time varchar(40),
    settle_end_time varchar(40),
    create_time     varchar(40),
    update_time     varchar(40),
    del_flag        varchar(4),
    tenant_id       varchar(64),
    source_system   varchar(64) DEFAULT 'rs_cloud_flower',
    imported_at     timestamp DEFAULT now()
);

CREATE TABLE IF NOT EXISTS xycyl_ods.ods_customer (
    id              varchar(64),
    code            varchar(64),
    name            varchar(255),
    abbreviation    varchar(255),
    contacts_name   varchar(255),
    contacts_phone  varchar(255),
    contacts_post   varchar(255),
    type            varchar(255),     -- 注意: p_customer 的 type 是 varchar
    status          varchar(255),
    source          varchar(255),
    address         varchar(512),
    create_time     varchar(40),
    update_time     varchar(40),
    del_flag        varchar(4),
    tenant_id       varchar(64),
    source_system   varchar(64) DEFAULT 'rs_cloud_flower',
    imported_at     timestamp DEFAULT now()
);

-- ============================================================
-- 加载策略提示（仅 dba 参考，dbt 不依赖）
-- ============================================================
-- 1. ETL 工具读取 MySQL → 写入这些 ODS 表（全量或增量）
-- 2. dbt 通过 source('xycyl_ods', '<table>') 引用
-- 3. STG 视图按 schema 'xycyl_stg' 物化
-- 4. DWD/DWS/ADS 按 'xycyl_dwd' / 'xycyl_dws' / 'xycyl_ads' 物化
