{{ config(tags=['position-adjustment']) }}

SELECT
  i.position_adjustment_item_id,
  h.position_adjustment_id,
  h.adjustment_code,
  h.adjustment_title,
  h.project_id,
  p.project_code,
  COALESCE(p.project_name, h.project_name) AS project_name,
  p.customer_id,
  p.customer_name,
  h.apply_user_id,
  h.apply_user_name,
  h.apply_time,
  h.status_raw AS adjustment_status_raw,
  COALESCE(s.standard_code, 'PAS-UNKNOWN') AS adjustment_status_code,
  COALESCE(s.label, '未知') AS adjustment_status_label,
  h.adjustment_type_raw,
  h.total_adjustment_number,
  h.curing_user_id,
  h.curing_user_name,
  i.old_position_id,
  old_pos.position_full_name AS old_position_full_name,
  i.new_position_id,
  new_pos.position_full_name AS new_position_full_name,
  i.good_price_id,
  goods.goods_id,
  COALESCE(goods.goods_name, i.good_name) AS goods_name,
  COALESCE(goods.specifications, i.good_norms) AS goods_norms,
  COALESCE(goods.goods_att, i.good_specs) AS goods_specs,
  COALESCE(goods.goods_unit, i.good_unit) AS goods_unit,
  i.good_type_raw,
  i.green_type_raw,
  i.adjustment_number,
  i.parent_item_id,
  i.old_project_green_id,
  i.rent_amount_raw,
  i.cost_amount_raw,
  (h.position_adjustment_id IS NULL) AS is_orphan_adjustment,
  (old_pos.position_id IS NULL) AS is_orphan_old_position,
  (new_pos.position_id IS NULL AND i.new_position_id IS NOT NULL) AS is_orphan_new_position,
  (goods.good_price_id IS NULL) AS is_orphan_goods_price,
  h.created_at,
  h.updated_at,
  COALESCE(i.source_system, h.source_system) AS source_system,
  COALESCE(i.imported_at, h.imported_at) AS imported_at
FROM {{ ref('xycyl_stg_project_position_adjustment_item') }} i
LEFT JOIN {{ ref('xycyl_stg_project_position_adjustment') }} h
  ON h.position_adjustment_id = i.position_adjustment_id
LEFT JOIN {{ ref('xycyl_dim_project') }} p ON p.project_id = h.project_id
LEFT JOIN {{ ref('xycyl_dim_position') }} old_pos ON old_pos.position_id = i.old_position_id
LEFT JOIN {{ ref('xycyl_dim_position') }} new_pos ON new_pos.position_id = i.new_position_id
LEFT JOIN {{ ref('xycyl_dim_goods') }} goods ON goods.good_price_id = i.good_price_id
LEFT JOIN {{ ref('xycyl_dim_position_adjustment_status') }} s
  ON s.code = COALESCE(h.status_raw::text, 'UNKNOWN')
