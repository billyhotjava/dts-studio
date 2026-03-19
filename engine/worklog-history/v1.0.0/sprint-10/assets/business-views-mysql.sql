-- =============================================================================
-- BG-02: Business View Layer - MySQL Views for rs_cloud_flower
-- =============================================================================
-- Target database: rs_cloud_flower (MySQL)
-- Purpose: Pre-joined business views with status code translation to Chinese.
--          These views serve as the sole data interface for NL2SQL queries
--          via the analytics JDBC external data source connection.
--
-- Usage: Execute this script against the rs_cloud_flower MySQL database.
-- Idempotent: All views use CREATE OR REPLACE VIEW.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. v_project_overview -- 项目总览
-- ---------------------------------------------------------------------------
-- Pre-join: p_project + p_contract + p_customer + green/position aggregation
-- Default business time: p.start_time (项目开始时间)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_project_overview AS
SELECT
    p.id                        AS project_id,
    p.project_name              AS project_name,
    p.project_code              AS project_code,
    CASE p.status
        WHEN 1 THEN '正常'
        WHEN 2 THEN '停用'
        ELSE CONCAT('未知(', IFNULL(p.status, 'NULL'), ')')
    END                         AS project_status_name,
    CASE p.type
        WHEN 1 THEN '租摆'
        WHEN 2 THEN '节日摆'
        ELSE CONCAT('未知(', IFNULL(p.type, 'NULL'), ')')
    END                         AS project_type_name,
    c.title                     AS contract_title,
    CASE c.status
        WHEN 1 THEN '草稿'
        WHEN 2 THEN '执行中'
        WHEN 3 THEN '已结束'
        ELSE CONCAT('未知(', IFNULL(c.status, 'NULL'), ')')
    END                         AS contract_status_name,
    c.start_date                AS contract_start_date,
    c.end_date                  AS contract_end_date,
    CASE c.settlement_type
        WHEN 1 THEN '按实摆结算'
        WHEN 2 THEN '固定月租'
        ELSE CONCAT('未知(', IFNULL(c.settlement_type, 'NULL'), ')')
    END                         AS settlement_type_name,
    c.month_settlement_money    AS month_settlement_money,
    c.discount_ratio            AS discount_ratio,
    cu.customer_name            AS customer_name,
    cu.customer_code            AS customer_code,
    p.manager_name              AS manager_name,
    p.supervisor_name           AS supervisor_name,
    p.biz_user_name             AS biz_user_name,
    p.curing_director_name      AS curing_director_name,
    p.address                   AS address,
    p.area                      AS area,
    p.budget_amount             AS budget_amount,
    IFNULL(pos_cnt.position_count, 0)   AS position_count,
    IFNULL(green_agg.green_count, 0)    AS green_count,
    IFNULL(green_agg.total_rent, 0)     AS total_rent,
    p.start_time                AS project_start_time,
    p.end_time                  AS project_end_time
FROM p_project p
LEFT JOIN p_contract c    ON p.contract_id = c.id
LEFT JOIN p_customer cu   ON c.customer_id = cu.id
LEFT JOIN (
    SELECT project_id, COUNT(*) AS position_count
    FROM p_position
    WHERE status = 0
    GROUP BY project_id
) pos_cnt ON pos_cnt.project_id = p.id
LEFT JOIN (
    SELECT project_id,
           COUNT(*)    AS green_count,
           SUM(rent)   AS total_rent
    FROM p_project_green
    WHERE status = 1
    GROUP BY project_id
) green_agg ON green_agg.project_id = p.id;


-- ---------------------------------------------------------------------------
-- 2. v_flower_biz_detail -- 报花业务明细
-- ---------------------------------------------------------------------------
-- Pre-join: t_flower_biz_info + t_flower_biz_item + p_project
-- Default business time: bi.apply_time (发起时间)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_flower_biz_detail AS
SELECT
    bi.id                       AS biz_id,
    bi.biz_code                 AS biz_code,
    CASE bi.biz_type
        WHEN 1  THEN '换花'
        WHEN 2  THEN '加花'
        WHEN 3  THEN '减花'
        WHEN 4  THEN '调花'
        WHEN 5  THEN '售花'
        WHEN 6  THEN '坏账'
        WHEN 7  THEN '销售'
        WHEN 8  THEN '内购'
        WHEN 11 THEN '加盆架'
        WHEN 12 THEN '减盆架'
        ELSE CONCAT('未知(', IFNULL(bi.biz_type, 'NULL'), ')')
    END                         AS biz_type_name,
    CASE bi.status
        WHEN -1 THEN '作废'
        WHEN 1  THEN '审核中'
        WHEN 2  THEN '备货中'
        WHEN 3  THEN '核算中'
        WHEN 4  THEN '待结算'
        WHEN 5  THEN '已完成'
        WHEN 20 THEN '草稿'
        WHEN 21 THEN '驳回'
        ELSE CONCAT('未知(', IFNULL(bi.status, 'NULL'), ')')
    END                         AS biz_status_name,
    p.project_name              AS project_name,
    item.position_name          AS position_name,
    item.position_full_name     AS position_full_name,
    bi.apply_user_name          AS apply_user_name,
    bi.apply_time               AS apply_time,
    bi.finish_time              AS finish_time,
    CASE bi.urgent
        WHEN 1 THEN '是'
        WHEN 2 THEN '否'
        ELSE '否'
    END                         AS is_urgent,
    item.green_name             AS green_name,
    item.good_name              AS good_name,
    item.good_norms             AS good_norms,
    item.plant_number           AS plant_number,
    item.rent                   AS rent,
    item.cost                   AS cost,
    bi.total_rent               AS biz_total_rent,
    bi.total_cost               AS biz_total_cost,
    CASE bi.bear_cost_type
        WHEN 1 THEN '养护人'
        WHEN 2 THEN '领导'
        WHEN 3 THEN '公司'
        WHEN 4 THEN '客户'
        ELSE CONCAT('未知(', IFNULL(bi.bear_cost_type, 'NULL'), ')')
    END                         AS bear_cost_type_name,
    bi.manager_name             AS manager_name,
    bi.curing_user_name         AS curing_user_name,
    DATE_FORMAT(bi.apply_time, '%Y-%m') AS biz_month
FROM t_flower_biz_info bi
LEFT JOIN t_flower_biz_item item ON item.flower_biz_id = bi.id
LEFT JOIN p_project p            ON p.id = bi.project_id;


-- ---------------------------------------------------------------------------
-- 3. v_project_green_current -- 当前在摆绿植
-- ---------------------------------------------------------------------------
-- Filter: p_project_green.status = 1 (摆放中 only)
-- Pre-join: p_project_green + p_position + p_project
-- Default business time: pg.pose_time (摆放时间)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_project_green_current AS
SELECT
    pg.id                       AS green_id,
    p.project_name              AS project_name,
    pos.position_name           AS position_name,
    CONCAT_WS('-',
        pos.floor_number_name,
        pos.floor_layer_name,
        pos.region
    )                           AS position_full_name,
    CASE pg.green_type
        WHEN 1 THEN '单品'
        WHEN 2 THEN '组合'
        ELSE CONCAT('未知(', IFNULL(pg.green_type, 'NULL'), ')')
    END                         AS green_type_name,
    pg.good_name                AS good_name,
    pg.good_norms               AS good_norms,
    pg.good_specs               AS good_specs,
    pg.good_unit                AS good_unit,
    pg.good_number              AS good_number,
    pg.rent                     AS rent,
    pg.cost                     AS cost,
    pg.pose_time                AS pose_time,
    pg.curing_user_name         AS curing_user_name,
    p.manager_name              AS manager_name,
    pos.floor_number_name       AS floor_number_name,
    pos.floor_layer_name        AS floor_layer_name
FROM p_project_green pg
LEFT JOIN p_position pos  ON pos.id = pg.position_id
LEFT JOIN p_project p     ON p.id = pg.project_id
WHERE pg.status = 1;


-- ---------------------------------------------------------------------------
-- 4. v_monthly_settlement -- 月度结算
-- ---------------------------------------------------------------------------
-- Pre-join: a_month_accounting + p_project + p_contract + p_customer
-- Default business time: ma.settlement_month (结算月份)
--
-- NOTE: The exact schema of a_month_accounting has not been verified against
--       the live database. The column names below are inferred from business
--       context and domain inventory docs. Please verify and adjust column
--       names (especially status codes, amount fields) against the actual
--       a_month_accounting table definition before deploying.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_monthly_settlement AS
SELECT
    ma.id                       AS settlement_id,
    p.project_name              AS project_name,
    cu.customer_name            AS customer_name,
    c.title                     AS contract_title,
    ma.settlement_month         AS settlement_month,
    CASE ma.status
        WHEN 1 THEN '待结算'
        WHEN 2 THEN '已结算'
        ELSE CONCAT('未知(', IFNULL(ma.status, 'NULL'), ')')
    END                         AS settlement_status_name,
    ma.total_rent               AS total_rent,
    ma.total_cost               AS total_cost,
    ma.receivable_amount        AS receivable_amount,
    ma.received_amount          AS received_amount,
    IFNULL(ma.receivable_amount, 0) - IFNULL(ma.received_amount, 0)
                                AS outstanding_amount,
    CASE c.settlement_type
        WHEN 1 THEN '按实摆结算'
        WHEN 2 THEN '固定月租'
        ELSE CONCAT('未知(', IFNULL(c.settlement_type, 'NULL'), ')')
    END                         AS settlement_type_name,
    p.manager_name              AS manager_name,
    p.biz_user_name             AS biz_user_name
FROM a_month_accounting ma
LEFT JOIN p_project p     ON p.id = ma.project_id
LEFT JOIN p_contract c    ON c.id = p.contract_id
LEFT JOIN p_customer cu   ON cu.id = c.customer_id;


-- ---------------------------------------------------------------------------
-- 5. v_task_progress -- 任务进度
-- ---------------------------------------------------------------------------
-- Pre-join: t_daily_task_info + p_project
-- Default business time: t.launch_time (发起时间)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_task_progress AS
SELECT
    t.id                        AS task_id,
    t.task_code                 AS task_code,
    t.task_title                AS task_title,
    CASE t.task_type
        WHEN 1 THEN '销售类'
        WHEN 2 THEN '内购类'
        WHEN 3 THEN '实摆变更类'
        WHEN 4 THEN '增值服务类'
        WHEN 5 THEN '支持类'
        WHEN 6 THEN '初摆类'
        ELSE CONCAT('未知(', IFNULL(t.task_type, 'NULL'), ')')
    END                         AS task_type_name,
    CASE t.status
        WHEN -1 THEN '已作废'
        WHEN 1  THEN '待发起'
        WHEN 2  THEN '进行中'
        WHEN 10 THEN '已结束'
        ELSE CONCAT('未知(', IFNULL(t.status, 'NULL'), ')')
    END                         AS task_status_name,
    p.project_name              AS project_name,
    t.launch_user_name          AS launch_user_name,
    t.leading_user_name         AS leading_user_name,
    t.launch_time               AS launch_time,
    t.start_time                AS start_time,
    t.end_time                  AS end_time,
    t.finish_time               AS finish_time,
    t.total_number              AS total_number,
    t.finish_number             AS finish_number,
    CASE
        WHEN t.total_number > 0
        THEN ROUND(t.finish_number / t.total_number, 2)
        ELSE 0
    END                         AS completion_rate,
    t.total_rent                AS total_rent,
    t.total_budget              AS total_budget
FROM t_daily_task_info t
LEFT JOIN p_project p ON p.id = t.project_id;


-- ---------------------------------------------------------------------------
-- 6. v_curing_coverage -- 养护覆盖
-- ---------------------------------------------------------------------------
-- Aggregation: t_curing_record + p_curing_position + p_project
-- Grouped by: project, curing_user, month
-- Default business time: curing_month (养护月份)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_curing_coverage AS
SELECT
    p.id                        AS project_id,
    p.project_name              AS project_name,
    cr.curing_user_name         AS curing_user_name,
    DATE_FORMAT(cr.curing_time, '%Y-%m') AS curing_month,
    COUNT(cr.id)                AS curing_count,
    IFNULL(pos_total.total_position_count, 0) AS total_position_count,
    COUNT(DISTINCT cr.position_id)            AS covered_position_count,
    CASE
        WHEN IFNULL(pos_total.total_position_count, 0) > 0
        THEN ROUND(
            COUNT(DISTINCT cr.position_id) / pos_total.total_position_count, 2
        )
        ELSE 0
    END                         AS coverage_rate,
    MAX(cr.curing_time)         AS last_curing_time
FROM t_curing_record cr
LEFT JOIN p_project p ON p.id = cr.project_id
LEFT JOIN (
    SELECT project_id, curing_user_id,
           COUNT(DISTINCT position_id) AS total_position_count
    FROM p_curing_position
    WHERE status = 1
    GROUP BY project_id, curing_user_id
) pos_total ON pos_total.project_id = cr.project_id
           AND pos_total.curing_user_id = cr.curing_user_id
WHERE cr.record_type = 1
GROUP BY p.id, p.project_name, cr.curing_user_name, cr.curing_user_id,
         DATE_FORMAT(cr.curing_time, '%Y-%m'), pos_total.total_position_count;


-- ---------------------------------------------------------------------------
-- 7. v_pendulum_progress -- 初摆进度
-- ---------------------------------------------------------------------------
-- Pre-join: i_pendulum_info + p_project
-- Default business time: pi.applicant_date (申请日期)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_pendulum_progress AS
SELECT
    pi.id                       AS pendulum_id,
    pi.pendulum_code            AS pendulum_code,
    pi.pendulum_title           AS pendulum_title,
    CASE pi.status
        WHEN 1 THEN '草稿'
        WHEN 2 THEN '待审批'
        WHEN 3 THEN '初摆中'
        WHEN 4 THEN '已完成'
        WHEN 5 THEN '审批驳回'
        WHEN 6 THEN '已作废'
        ELSE CONCAT('未知(', IFNULL(pi.status, 'NULL'), ')')
    END                         AS pendulum_status_name,
    p.project_name              AS project_name,
    pi.applicant_name           AS applicant_name,
    pi.applicant_date           AS applicant_date,
    pi.start_date               AS start_date,
    pi.end_date                 AS end_date,
    pi.total_budget_cost        AS total_budget_cost,
    pi.actual_cost              AS actual_cost,
    pi.balance_cost             AS balance_cost,
    pi.year_rent                AS year_rent
FROM i_pendulum_info pi
LEFT JOIN p_project p ON p.id = pi.project_id;
