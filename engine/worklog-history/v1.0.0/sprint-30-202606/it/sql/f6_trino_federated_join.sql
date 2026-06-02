-- F6: true cross-database Join through Trino.
-- Catalogs:
--   postgres.public           -> dts-stack PG warehouse
--   mysql.rs_cloud_flower     -> legacy business MySQL
SELECT
    pg.pg_flowerbiz_rows,
    my.mysql_flowerbiz_rows
FROM (
    SELECT count(*) AS pg_flowerbiz_rows
    FROM postgres.public.xycyl_dwd_flowerbiz_main
) pg
JOIN (
    SELECT count(*) AS mysql_flowerbiz_rows
    FROM mysql.rs_cloud_flower.t_flower_biz_info
) my ON true
LIMIT 1;
