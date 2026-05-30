-- Sprint-26 F2/T04
-- Reconcile ontology bad-debt signal hits with the certified adminweb fixed report
-- PRS-FLOWERBIZ-FINANCE-COST / prs-flowerbiz-finance-cost-v1.
--
-- Runtime defaults match the sprint-26 local evidence window.
-- Override by editing params or wrapping this SQL from a psql script with
-- an equivalent params CTE.

WITH params AS (
    SELECT
        DATE '2025-05-01' AS date_from,
        DATE '2026-05-30' AS date_to,
        NULL::text AS project_name,
        NULL::text AS customer_name,
        0.005::numeric AS max_error_rate
),
signal_hits AS (
    SELECT
        b."业务月份",
        b."项目",
        b."客户",
        SUM(b."坏账成本全口径") AS baddebt_cost,
        SUM(b."坏账租金损失全口径") AS baddebt_rent_loss
    FROM public.xycyl_ads_flowerbiz_baddebt_summary b
    CROSS JOIN params p
    WHERE b."业务月份" >= to_char(p.date_from, 'YYYY-MM')
      AND b."业务月份" <= to_char(p.date_to, 'YYYY-MM')
      AND (p.project_name IS NULL OR b."项目" = p.project_name)
      AND (p.customer_name IS NULL OR b."客户" = p.customer_name)
    GROUP BY b."业务月份", b."项目", b."客户"
    HAVING SUM(b."坏账租金损失全口径")
           / NULLIF(SUM(b."坏账成本全口径") + SUM(b."坏账租金损失全口径"), 0) > 0.15
       AND SUM(b."坏账租金损失全口径") > 0
),
adminweb_fixed_report_slice AS (
    SELECT
        f."业务月份",
        f."项目",
        f."客户",
        SUM(f."坏账成本全口径") AS baddebt_cost,
        SUM(f."坏账租金损失全口径") AS baddebt_rent_loss
    FROM public.xycyl_ads_flowerbiz_finance_cost f
    CROSS JOIN params p
    WHERE f."业务月份" >= to_char(p.date_from, 'YYYY-MM')
      AND f."业务月份" <= to_char(p.date_to, 'YYYY-MM')
      AND (p.project_name IS NULL OR f."项目" = p.project_name)
      AND (p.customer_name IS NULL OR f."客户" = p.customer_name)
    GROUP BY f."业务月份", f."项目", f."客户"
    HAVING SUM(f."坏账租金损失全口径")
           / NULLIF(SUM(f."坏账成本全口径") + SUM(f."坏账租金损失全口径"), 0) > 0.15
       AND SUM(f."坏账租金损失全口径") > 0
),
metric_rows AS (
    SELECT
        '坏账风险命中项目客户数' AS metric_name,
        COUNT(*)::numeric AS signal_value,
        (SELECT COUNT(*)::numeric FROM adminweb_fixed_report_slice) AS adminweb_value
    FROM signal_hits
    UNION ALL
    SELECT
        '坏账成本全口径',
        COALESCE(SUM(baddebt_cost), 0),
        (SELECT COALESCE(SUM(baddebt_cost), 0) FROM adminweb_fixed_report_slice)
    FROM signal_hits
    UNION ALL
    SELECT
        '坏账租金损失全口径',
        COALESCE(SUM(baddebt_rent_loss), 0),
        (SELECT COALESCE(SUM(baddebt_rent_loss), 0) FROM adminweb_fixed_report_slice)
    FROM signal_hits
),
diffs AS (
    SELECT
        metric_name,
        signal_value,
        adminweb_value,
        ABS(signal_value - adminweb_value) AS diff_abs,
        CASE
            WHEN ABS(adminweb_value) = 0 THEN
                CASE WHEN ABS(signal_value) = 0 THEN 0::numeric ELSE 1::numeric END
            ELSE ABS(signal_value - adminweb_value) / ABS(adminweb_value)
        END AS diff_rate,
        (SELECT max_error_rate FROM params) AS max_error_rate
    FROM metric_rows
)
SELECT
    metric_name,
    ROUND(signal_value, 2) AS signal_value,
    ROUND(adminweb_value, 2) AS adminweb_value,
    ROUND(diff_abs, 2) AS diff_abs,
    ROUND(diff_rate * 100, 4) AS diff_pct,
    CASE WHEN diff_rate <= max_error_rate THEN 'PASS' ELSE 'FAIL' END AS status
FROM diffs
ORDER BY metric_name;
