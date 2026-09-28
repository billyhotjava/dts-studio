{{ config(tags=['customer']) }}

SELECT
  o.id::bigint AS source_row_id,
  'ods_ptr_mysql_p_customer'::text AS source_table,
  COALESCE({{ nullif_placeholder("o._dts_source_system") }}, 'ptr_mysql') AS source_system,
  o._dts_import_time AS imported_at,

  o.id::bigint AS customer_id,
  {{ nullif_placeholder("o.code") }} AS customer_code,
  {{ nullif_placeholder("o.name") }} AS customer_name,
  {{ nullif_placeholder("o.abbreviation") }} AS customer_abbreviation,
  {{ nullif_placeholder("o.contacts_name") }} AS contacts_name,
  {{ nullif_placeholder("o.contacts_phone") }} AS contacts_phone,
  {{ nullif_placeholder("o.contacts_post") }} AS contacts_post,
  {{ nullif_placeholder("o.type") }} AS customer_type_raw,
  {{ nullif_placeholder("o.status") }} AS status_raw,
  {{ nullif_placeholder("o.source") }} AS customer_source_raw,
  {{ nullif_placeholder("o.address") }} AS customer_address,
  o.tenant_id::bigint AS tenant_id,
  o.create_by::bigint AS created_by,
  o.create_time::timestamp AS created_at,
  o.update_by::bigint AS updated_by,
  o.update_time::timestamp AS updated_at,
  {{ nullif_placeholder("o.remark") }} AS remark
FROM {{ source('xycyl_project_ods', 'p_customer') }} o
WHERE COALESCE({{ nullif_placeholder("o.del_flag") }}, '0') = '0'
