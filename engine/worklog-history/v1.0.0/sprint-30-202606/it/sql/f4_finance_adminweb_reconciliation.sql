-- Sprint-30 F4 finance mart reconciliation
-- 目的: 对齐 adminweb 租摆月报 / 开票 / 收款口径与 dts-dbt ADS。
-- 运行环境: PostgreSQL public schema。

WITH rental_monthly_adminweb AS (
  SELECT
    COALESCE(year_and_month, settlement_year || '-' || lpad(settlement_month::text, 2, '0')) AS business_month,
    project_id::bigint AS project_id,
    ROUND(SUM(NULLIF(folding_after_total_amount, '')::numeric), 2) AS adminweb_folding_after_amount,
    ROUND(SUM(NULLIF(total_amount, '')::numeric), 2) AS adminweb_collected_amount
  FROM public.ods_ptr_mysql_a_month_accounting
  GROUP BY 1, 2
),
month_ads AS (
  SELECT
    "业务月份" AS business_month,
    "项目id" AS project_id,
    SUM("折后实收") AS ads_folding_after_amount,
    SUM("已回款") AS ads_collected_amount
  FROM public.xycyl_ads_finance_month_settlement
  GROUP BY 1, 2
)
SELECT
  'rentalMonthlyReport' AS check_name,
  COALESCE(a.business_month, b.business_month) AS business_month,
  COALESCE(a.project_id, b.project_id) AS project_id,
  a.adminweb_folding_after_amount,
  b.ads_folding_after_amount,
  ROUND((b.ads_folding_after_amount - a.adminweb_folding_after_amount)::numeric, 2) AS folding_after_diff,
  a.adminweb_collected_amount,
  b.ads_collected_amount,
  ROUND((b.ads_collected_amount - a.adminweb_collected_amount)::numeric, 2) AS collected_diff
FROM rental_monthly_adminweb a
FULL JOIN month_ads b
  ON a.business_month = b.business_month AND a.project_id = b.project_id;

WITH invoice_adminweb AS (
  SELECT
    to_char(COALESCE(NULLIF(invoice_time, '')::timestamp, NULLIF(apply_time, '')::timestamp), 'YYYY-MM') AS invoice_month,
    project_id::bigint AS project_id,
    ROUND(SUM(NULLIF(apply_invoice_amoney, '')::numeric), 2) AS adminweb_apply_amount,
    ROUND(SUM(NULLIF(invoice_amoney, '')::numeric), 2) AS adminweb_invoice_amount,
    ROUND(SUM(NULLIF(pay_total_amount, '')::numeric), 2) AS adminweb_paid_amount
  FROM public.ods_ptr_mysql_a_invoice_info
  GROUP BY 1, 2
),
invoice_ads AS (
  SELECT
    "开票月份" AS invoice_month,
    "项目id" AS project_id,
    SUM("申请开票金额") AS ads_apply_amount,
    SUM("实际开票金额") AS ads_invoice_amount,
    SUM("已回款") AS ads_paid_amount
  FROM public.xycyl_ads_finance_invoice_progress
  GROUP BY 1, 2
)
SELECT
  'invoice_progress' AS check_name,
  COALESCE(a.invoice_month, b.invoice_month) AS invoice_month,
  COALESCE(a.project_id, b.project_id) AS project_id,
  a.adminweb_apply_amount,
  b.ads_apply_amount,
  ROUND((b.ads_apply_amount - a.adminweb_apply_amount)::numeric, 2) AS apply_diff,
  a.adminweb_invoice_amount,
  b.ads_invoice_amount,
  ROUND((b.ads_invoice_amount - a.adminweb_invoice_amount)::numeric, 2) AS invoice_diff,
  a.adminweb_paid_amount,
  b.ads_paid_amount,
  ROUND((b.ads_paid_amount - a.adminweb_paid_amount)::numeric, 2) AS paid_diff
FROM invoice_adminweb a
FULL JOIN invoice_ads b
  ON a.invoice_month = b.invoice_month AND a.project_id = b.project_id;

WITH collection_adminweb AS (
  SELECT
    to_char(NULLIF(r.pay_time, '')::timestamp, 'YYYY-MM') AS collection_month,
    r.project_id::bigint AS project_id,
    ROUND(SUM(NULLIF(i.total_amount, '')::numeric), 2) AS adminweb_collection_amount
  FROM public.ods_ptr_mysql_a_collection_record r
  JOIN public.ods_ptr_mysql_a_collection_item i
    ON i.collection_record_id = r.id
  GROUP BY 1, 2
),
collection_ads AS (
  SELECT
    "收款月份" AS collection_month,
    "项目id" AS project_id,
    SUM("收款金额") AS ads_collection_amount
  FROM public.xycyl_ads_finance_collection
  GROUP BY 1, 2
)
SELECT
  'collection' AS check_name,
  COALESCE(a.collection_month, b.collection_month) AS collection_month,
  COALESCE(a.project_id, b.project_id) AS project_id,
  a.adminweb_collection_amount,
  b.ads_collection_amount,
  ROUND((b.ads_collection_amount - a.adminweb_collection_amount)::numeric, 2) AS collection_diff
FROM collection_adminweb a
FULL JOIN collection_ads b
  ON a.collection_month = b.collection_month AND a.project_id = b.project_id;
