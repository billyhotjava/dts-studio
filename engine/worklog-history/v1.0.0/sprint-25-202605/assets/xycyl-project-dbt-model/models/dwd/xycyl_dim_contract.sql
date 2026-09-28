{{ config(tags=['contract']) }}

SELECT
  c.contract_id,
  c.customer_id,
  cu.customer_code,
  cu.customer_name,
  c.contract_code,
  c.contract_title,
  c.signing_time,
  c.status_raw,
  COALESCE(s.standard_code, 'CON-UNKNOWN') AS status_code,
  COALESCE(s.label, '未知') AS status_label,
  c.start_date,
  c.end_date,
  c.contract_type_raw,
  c.settlement_method_raw,
  c.settlement_type_raw,
  c.verify_type_raw,
  c.verify_day_number,
  c.month_settlement_money,
  c.discount_ratio,
  c.amount_excluding_tax,
  c.amount_including_tax,
  c.invoice_type_raw,
  c.invoicing_tax_rate,
  c.parent_contract_id,
  c.active_child_contract_id,
  c.tenant_id,
  c.created_at,
  c.updated_at,
  c.source_system,
  c.imported_at
FROM {{ ref('xycyl_stg_project_contract') }} c
LEFT JOIN {{ ref('xycyl_dim_customer') }} cu ON cu.customer_id = c.customer_id
LEFT JOIN {{ ref('xycyl_dim_contract_status') }} s ON s.code = COALESCE(c.status_raw::text, 'UNKNOWN')
