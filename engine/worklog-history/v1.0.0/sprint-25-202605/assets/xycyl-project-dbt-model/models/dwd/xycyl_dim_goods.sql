{{ config(tags=['goods']) }}

SELECT
  gp.good_price_id,
  gp.goods_id,
  COALESCE(g.goods_code, gp.goods_code) AS goods_code,
  COALESCE(g.goods_name, gp.goods_name) AS goods_name,
  COALESCE(g.goods_type_raw, gp.goods_type_raw) AS goods_type_raw,
  COALESCE(g.goods_unit, gp.goods_unit) AS goods_unit,
  COALESCE(g.specifications, gp.specifications) AS specifications,
  gp.goods_att,
  COALESCE(g.goods_category, gp.goods_category) AS goods_category,
  g.goods_classify_id,
  gp.guidance_price,
  gp.cost_price,
  gp.status_raw AS price_status_raw,
  g.status_raw AS goods_status_raw,
  gp.created_at,
  gp.source_system,
  gp.imported_at
FROM {{ ref('xycyl_stg_project_goods_price') }} gp
LEFT JOIN {{ ref('xycyl_stg_project_goods') }} g ON g.goods_id = gp.goods_id
