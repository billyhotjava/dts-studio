-- ============================================================
-- Sprint-10 业务视图层 V2 (基于数据库扫描修正)
-- 目标库: rs_cloud_flower (MySQL)
-- 更新: 2026-03-20 基于 db.weitaor.com 实际 schema 修正
-- ============================================================

-- ============================================================
-- 1. v_project_overview — 项目总览
-- 默认业务时间: start_time
-- 修正: JOIN p_project_role 获取人员姓名; 加 del_flag 过滤
-- ============================================================
CREATE OR REPLACE VIEW v_project_overview AS
SELECT
    p.id                            AS project_id,
    p.name                          AS project_name,
    p.code                          AS project_code,
    CASE p.status
        WHEN 1 THEN '正常'
        WHEN 2 THEN '停用'
        ELSE CONCAT('未知(', IFNULL(p.status,''), ')')
    END                             AS project_status_name,
    CASE p.type
        WHEN 1 THEN '租摆'
        WHEN 2 THEN '节日摆'
        ELSE CONCAT('未知(', IFNULL(p.type,''), ')')
    END                             AS project_type_name,
    ct.title                        AS contract_title,
    ct.code                         AS contract_code,
    CASE ct.status
        WHEN 1 THEN '草稿'
        WHEN 2 THEN '执行中'
        WHEN 3 THEN '已结束'
        ELSE CONCAT('未知(', IFNULL(ct.status,''), ')')
    END                             AS contract_status_name,
    ct.start_date                   AS contract_start_date,
    ct.end_date                     AS contract_end_date,
    CASE ct.settlement_type
        WHEN 1 THEN '按实摆结算'
        WHEN 2 THEN '固定月租'
        ELSE CONCAT('未知(', IFNULL(ct.settlement_type,''), ')')
    END                             AS settlement_type_name,
    ct.month_settlement_money       AS month_settlement_money,
    ct.discount_ratio               AS discount_ratio,
    cu.name                         AS customer_name,
    cu.code                         AS customer_code,
    -- 人员: 从 p_project_role 获取
    pm.user_name                    AS manager_name,
    bm.user_name                    AS biz_user_name,
    sv.user_name                    AS supervisor_name,
    p.curing_director_name          AS curing_director_name,
    p.address                       AS address,
    p.area                          AS area,
    p.budget_amount                 AS budget_amount,
    p.start_time                    AS project_start_time,
    p.end_time                      AS project_end_time,
    p.last_month_total_rent         AS last_month_total_rent,
    CASE p.check_cycle
        WHEN 1 THEN '每月' WHEN 2 THEN '双月' WHEN 3 THEN '季度'
        WHEN 6 THEN '半年' WHEN 12 THEN '年度'
        ELSE CONCAT('未知(', IFNULL(p.check_cycle,''), ')')
    END                             AS check_cycle_name,
    -- 统计子查询
    IFNULL(pos.position_count, 0)   AS position_count,
    IFNULL(gr.green_count, 0)       AS green_count,
    IFNULL(gr.total_rent, 0)        AS total_rent
FROM p_project p
LEFT JOIN p_contract ct ON p.contract_id = ct.id AND ct.del_flag = '0'
LEFT JOIN p_customer cu ON ct.customer_id = cu.id AND cu.del_flag = '0'
-- 项目经理
LEFT JOIN (
    SELECT project_id, user_name
    FROM p_project_role
    WHERE project_manage = 1 AND status = 0
) pm ON pm.project_id = p.id
-- 业务经理
LEFT JOIN (
    SELECT project_id, user_name
    FROM p_project_role
    WHERE biz_manage = 1 AND status = 0
) bm ON bm.project_id = p.id
-- 监理
LEFT JOIN (
    SELECT project_id, user_name
    FROM p_project_role
    WHERE supervise = 1 AND status = 0
) sv ON sv.project_id = p.id
-- 摆位数
LEFT JOIN (
    SELECT project_id, COUNT(*) AS position_count
    FROM p_position
    WHERE status = 0 AND del_flag = '0'
    GROUP BY project_id
) pos ON pos.project_id = p.id
-- 在摆绿植数和租金
LEFT JOIN (
    SELECT project_id,
           COUNT(*) AS green_count,
           IFNULL(SUM(rent), 0) AS total_rent
    FROM p_project_green
    WHERE status = 1 AND del_flag = '0'
    GROUP BY project_id
) gr ON gr.project_id = p.id
WHERE p.del_flag = '0';


-- ============================================================
-- 2. v_flower_biz_detail — 报花业务明细
-- 默认业务时间: apply_time
-- 修正: 补充 biz_type=10(辅料); 加 del_flag 过滤
-- ============================================================
CREATE OR REPLACE VIEW v_flower_biz_detail AS
SELECT
    bi.id                           AS biz_id,
    bi.code                         AS biz_code,
    CASE bi.biz_type
        WHEN 1  THEN '换花'
        WHEN 2  THEN '加花'
        WHEN 3  THEN '减花'
        WHEN 4  THEN '调花'
        WHEN 5  THEN '售花'
        WHEN 6  THEN '坏账'
        WHEN 7  THEN '销售'
        WHEN 8  THEN '内购'
        WHEN 10 THEN '辅料'
        WHEN 11 THEN '加盆架'
        WHEN 12 THEN '减盆架'
        ELSE CONCAT('未知(', IFNULL(bi.biz_type,''), ')')
    END                             AS biz_type_name,
    CASE bi.status
        WHEN -1 THEN '作废'
        WHEN 1  THEN '审核中'
        WHEN 2  THEN '备货中'
        WHEN 3  THEN '核算中'
        WHEN 4  THEN '待结算'
        WHEN 5  THEN '已完成'
        WHEN 20 THEN '草稿'
        WHEN 21 THEN '驳回'
        ELSE CONCAT('未知(', IFNULL(bi.status,''), ')')
    END                             AS biz_status_name,
    bi.project_id                   AS project_id,
    bi.project_name                 AS project_name,
    item.position_name              AS position_name,
    item.position_full_name         AS position_full_name,
    bi.apply_use_name               AS apply_user_name,
    bi.apply_time                   AS apply_time,
    bi.finish_time                  AS finish_time,
    CASE bi.urgent
        WHEN 1 THEN '是'
        WHEN 2 THEN '否'
        ELSE '否'
    END                             AS is_urgent,
    item.green_name                 AS green_name,
    item.good_norms                 AS good_norms,
    item.good_specs                 AS good_specs,
    item.good_unit                  AS good_unit,
    IFNULL(item.plant_number, 0)    AS plant_number,
    item.rent                       AS item_rent,
    item.cost                       AS item_cost,
    bi.biz_total_rent               AS biz_total_rent,
    bi.biz_total_cost               AS biz_total_cost,
    CASE bi.bear_cost_type
        WHEN 1 THEN '养护人'
        WHEN 2 THEN '领导'
        WHEN 3 THEN '公司'
        WHEN 4 THEN '客户'
        ELSE ''
    END                             AS bear_cost_type_name,
    bi.project_manage_name          AS manager_name,
    bi.curing_user_name             AS curing_user_name,
    DATE_FORMAT(bi.apply_time, '%Y-%m') AS biz_month
FROM t_flower_biz_info bi
LEFT JOIN t_flower_biz_item item ON item.flower_biz_id = bi.id AND item.del_flag = '0'
WHERE bi.del_flag = '0';


-- ============================================================
-- 3. v_project_green_current — 当前在摆绿植
-- 默认业务时间: pose_time
-- 修正: 加 del_flag; position_full_name 直接用 p_project_green 已有字段
-- ============================================================
CREATE OR REPLACE VIEW v_project_green_current AS
SELECT
    pg.id                           AS green_id,
    pg.project_id                   AS project_id,
    pg.project_name                 AS project_name,
    pg.position_id                  AS position_id,
    pg.position_name                AS position_name,
    pg.position_full_name           AS position_full_name,
    CASE pg.green_type
        WHEN 1 THEN '单品'
        WHEN 2 THEN '组合'
        ELSE CONCAT('未知(', IFNULL(pg.green_type,''), ')')
    END                             AS green_type_name,
    pg.good_name                    AS good_name,
    pg.good_norms                   AS good_norms,
    pg.good_specs                   AS good_specs,
    pg.good_unit                    AS good_unit,
    IFNULL(pg.good_number, 1)       AS good_number,
    pg.rent                         AS rent,
    pg.cost                         AS cost,
    pg.pose_time                    AS pose_time,
    -- 养护人从 p_curing_position 获取
    cp.curing_user_name             AS curing_user_name
FROM p_project_green pg
LEFT JOIN p_curing_position cp
    ON cp.position_id = pg.position_id
    AND cp.project_id = pg.project_id
    AND cp.status = 1
WHERE pg.status = 1
  AND pg.del_flag = '0';


-- ============================================================
-- 4. v_monthly_settlement — 月度结算
-- 默认业务时间: year_and_month
-- 修正: 使用实际字段名; a_collection_record 为空不依赖它
-- ============================================================
CREATE OR REPLACE VIEW v_monthly_settlement AS
SELECT
    ma.id                           AS settlement_id,
    ma.project_id                   AS project_id,
    ma.project_name                 AS project_name,
    ma.company_name                 AS customer_name,
    ma.year_and_month               AS settlement_month,
    CASE ma.status
        WHEN 1 THEN '待结算'
        WHEN 2 THEN '已结算'
        ELSE CONCAT('未知(', IFNULL(ma.status,''), ')')
    END                             AS settlement_status_name,
    CASE ma.rent_type
        WHEN 1 THEN '按实摆结算'
        WHEN 2 THEN '固定月租'
        ELSE ''
    END                             AS settlement_type_name,
    ma.receivable_total_amount      AS total_rent,
    ma.net_receipt_total_amount     AS received_amount,
    IFNULL(ma.receivable_total_amount, 0) - IFNULL(ma.net_receipt_total_amount, 0)
                                    AS outstanding_amount,
    ma.folding_after_total_amount   AS discounted_amount,
    ma.regular_rent                 AS regular_rent,
    ma.discount_rate                AS discount_rate,
    -- 明细金额
    ma.period_total_amount          AS period_total_amount,
    ma.add_total_amount             AS add_total_amount,
    ma.cut_total_amount             AS cut_total_amount,
    ma.sale_total_amount            AS sale_total_amount,
    ma.total_day                    AS total_day,
    -- 人员
    ma.project_manage_user_name     AS manager_name,
    ma.biz_user_name                AS biz_user_name,
    ma.start_time                   AS period_start_time,
    ma.end_time                     AS period_end_time,
    ma.settlement_year              AS settlement_year,
    ma.settlement_month             AS settlement_month_num
FROM a_month_accounting ma;


-- ============================================================
-- 5. v_task_progress — 任务进度
-- 默认业务时间: create_time (launch_time 不存在)
-- t_daily_task_info 有 launch_time 和 launch_user_name
-- ============================================================
CREATE OR REPLACE VIEW v_task_progress AS
SELECT
    t.id                            AS task_id,
    t.code                          AS task_code,
    t.title                         AS task_title,
    CASE t.task_type
        WHEN 1 THEN '销售类'
        WHEN 2 THEN '内购类'
        WHEN 3 THEN '实摆变更类'
        WHEN 4 THEN '增值服务类'
        WHEN 5 THEN '支持类'
        WHEN 6 THEN '初摆类'
        ELSE CONCAT('未知(', IFNULL(t.task_type,''), ')')
    END                             AS task_type_name,
    CASE t.status
        WHEN -1 THEN '已作废'
        WHEN 1  THEN '待发起'
        WHEN 2  THEN '进行中'
        WHEN 10 THEN '已结束'
        ELSE CONCAT('未知(', IFNULL(t.status,''), ')')
    END                             AS task_status_name,
    t.project_id                    AS project_id,
    p.name                          AS project_name,
    t.launch_user_name              AS launch_user_name,
    t.leading_user_name             AS leading_user_name,
    t.launch_time                   AS launch_time,
    t.start_time                    AS start_time,
    t.end_time                      AS end_time,
    t.finish_time                   AS finish_time,
    IFNULL(t.total_number, 0)       AS total_number,
    IFNULL(t.finish_number, 0)      AS finish_number,
    CASE
        WHEN IFNULL(t.total_number, 0) > 0
        THEN ROUND(IFNULL(t.finish_number, 0) / t.total_number, 2)
        ELSE 0
    END                             AS completion_rate,
    t.total_rent                    AS total_rent,
    t.total_budget                  AS total_budget
FROM t_daily_task_info t
LEFT JOIN p_project p ON t.project_id = p.id
WHERE t.del_flag = '0';


-- ============================================================
-- 6. v_curing_coverage — 养护覆盖率
-- 默认业务时间: curing_month
-- ============================================================
CREATE OR REPLACE VIEW v_curing_coverage AS
SELECT
    cr.project_id                   AS project_id,
    p.name                          AS project_name,
    cr.curing_user_name             AS curing_user_name,
    DATE_FORMAT(cr.curing_time, '%Y-%m') AS curing_month,
    COUNT(*)                        AS curing_count,
    cr.total_position_number        AS total_position_count,
    COUNT(DISTINCT cr.id)           AS covered_curing_sessions,
    MAX(cr.curing_time)             AS last_curing_time
FROM t_curing_record cr
LEFT JOIN p_project p ON cr.project_id = p.id
WHERE cr.del_flag = '0'
GROUP BY cr.project_id, p.name, cr.curing_user_name,
         DATE_FORMAT(cr.curing_time, '%Y-%m'), cr.total_position_number;


-- ============================================================
-- 7. v_pendulum_progress — 初摆进度
-- 注意: 当前测试库 i_pendulum_info 数据为空
-- ============================================================
CREATE OR REPLACE VIEW v_pendulum_progress AS
SELECT
    pi.id                           AS pendulum_id,
    pi.code                         AS pendulum_code,
    pi.title                        AS pendulum_title,
    CASE pi.status
        WHEN 1 THEN '草稿'
        WHEN 2 THEN '待审批'
        WHEN 3 THEN '初摆中'
        WHEN 4 THEN '已完成'
        WHEN 5 THEN '审批驳回'
        WHEN 6 THEN '已作废'
        ELSE CONCAT('未知(', IFNULL(pi.status,''), ')')
    END                             AS pendulum_status_name,
    pi.project_id                   AS project_id,
    p.name                          AS project_name,
    pi.applicant_name               AS applicant_name,
    pi.applicant_date               AS applicant_date,
    pi.start_date                   AS start_date,
    pi.end_date                     AS end_date,
    pi.total_budget_cost            AS total_budget_cost,
    pi.actual_cost                  AS actual_cost,
    pi.balance_cost                 AS balance_cost,
    pi.year_rent                    AS year_rent
FROM i_pendulum_info pi
LEFT JOIN p_project p ON pi.project_id = p.id;
