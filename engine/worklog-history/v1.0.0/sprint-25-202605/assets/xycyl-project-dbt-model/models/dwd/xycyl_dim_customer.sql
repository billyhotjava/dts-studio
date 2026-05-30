{{ config(tags=['customer']) }}

SELECT
  customer_id,
  customer_code,
  customer_name,
  customer_abbreviation,
  contacts_name,
  contacts_phone,
  contacts_post,
  customer_type_raw,
  status_raw,
  customer_source_raw,
  customer_address,
  tenant_id,
  created_at,
  updated_at,
  source_system,
  imported_at
FROM {{ ref('xycyl_stg_project_customer') }}
