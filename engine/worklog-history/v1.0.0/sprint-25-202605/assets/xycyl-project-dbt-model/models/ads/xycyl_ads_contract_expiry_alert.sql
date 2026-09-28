{{ config(tags=['contract', 'expiry']) }}

SELECT
  c.contract_id,
  c.contract_code,
  c.contract_title,
  c.customer_id,
  c.customer_name,
  c.status_code AS contract_status_code,
  c.status_label AS contract_status_label,
  c.start_date,
  c.end_date,
  (c.end_date::date - current_date) AS days_to_end_date,
  CASE
    WHEN c.end_date IS NULL THEN 'EXP-UNKNOWN'
    WHEN c.end_date::date < current_date THEN 'EXP-EXPIRED'
    WHEN c.end_date::date <= current_date + interval '30 days' THEN 'EXP-30D'
    WHEN c.end_date::date <= current_date + interval '90 days' THEN 'EXP-90D'
    ELSE 'EXP-NORMAL'
  END AS expiry_alert_code,
  CASE
    WHEN c.end_date IS NULL THEN '未知'
    WHEN c.end_date::date < current_date THEN '已过期'
    WHEN c.end_date::date <= current_date + interval '30 days' THEN '30天内到期'
    WHEN c.end_date::date <= current_date + interval '90 days' THEN '90天内到期'
    ELSE '正常'
  END AS expiry_alert_label,
  c.month_settlement_money,
  c.amount_including_tax,
  c.imported_at
FROM {{ ref('xycyl_dim_contract') }} c
