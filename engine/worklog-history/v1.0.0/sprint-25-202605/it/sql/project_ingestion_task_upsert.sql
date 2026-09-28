-- Sprint-25 project datasurface ingestion task.
--
-- Idempotently creates or refreshes a DTS ingestion task that reuses the
-- existing ptr_mysql_flow source/destination connection settings while narrowing
-- the table mapping to the project-domain sources required by Sprint-25.
-- The SQL does not contain credentials; secure source and target settings stay
-- in the platform runtime tables cloned from ptr_mysql_flow.

\set ON_ERROR_STOP on

BEGIN;

WITH required(source_table, target_table, ord) AS (
  VALUES
    ('p_project', 'ods_ptr_mysql_p_project', 1),
    ('p_customer', 'ods_ptr_mysql_p_customer', 2),
    ('p_contract', 'ods_ptr_mysql_p_contract', 3),
    ('p_position', 'ods_ptr_mysql_p_position', 4),
    ('p_floor_layer', 'ods_ptr_mysql_p_floor_layer', 5),
    ('p_floor_number', 'ods_ptr_mysql_p_floor_number', 6),
    ('b_goods', 'ods_ptr_mysql_b_goods', 7),
    ('b_goods_price', 'ods_ptr_mysql_b_goods_price', 8),
    ('p_project_green', 'ods_ptr_mysql_p_project_green', 9),
    ('p_position_adjustment', 'ods_ptr_mysql_p_position_adjustment', 10),
    ('p_position_adjustment_item', 'ods_ptr_mysql_p_position_adjustment_item', 11)
),
template AS (
  SELECT *
  FROM public.ingestion_task
  WHERE name = 'ptr_mysql_flow'
  ORDER BY id DESC
  LIMIT 1
),
payload AS (
  SELECT
    'sprint25_project_datasurface'::varchar AS name,
    'Sprint-25 project/shared-dimension ODS full refresh cloned from ptr_mysql_flow without embedding credentials.'::text AS description,
    template.source_type,
    jsonb_set(
      jsonb_set(
        COALESCE(template.source_config, '{}'::jsonb),
        '{table}',
        (SELECT jsonb_agg(source_table ORDER BY ord) FROM required),
        true
      ),
      '{column}',
      '["*"]'::jsonb,
      true
    ) AS source_config,
    template.destination_type,
    jsonb_set(
      COALESCE(template.destination_config, '{}'::jsonb),
      '{connection,0,table}',
      (SELECT jsonb_agg(target_table ORDER BY ord) FROM required),
      true
    ) AS destination_config,
    template.source_data_source_id,
    template.sync_mode,
    template.sync_schedule,
    template.sync_config,
    template.addax_config,
    template.airflow_enabled,
    template.airflow_dag_id,
    (
      SELECT jsonb_agg(
        jsonb_build_object('source', source_table, 'target', target_table)
        ORDER BY ord
      )
      FROM required
    ) AS table_mapping
  FROM template
),
updated AS (
  UPDATE public.ingestion_task t
  SET
    description = payload.description,
    source_type = payload.source_type,
    source_config = payload.source_config,
    destination_type = payload.destination_type,
    destination_config = payload.destination_config,
    source_data_source_id = payload.source_data_source_id,
    sync_mode = payload.sync_mode,
    sync_schedule = payload.sync_schedule,
    sync_config = payload.sync_config,
    addax_config = payload.addax_config,
    airflow_enabled = payload.airflow_enabled,
    airflow_dag_id = payload.airflow_dag_id,
    table_mapping = payload.table_mapping,
    addax_job_path = NULL,
    status = 'active',
    last_modified_by = 'codex-sprint25',
    last_modified_date = now()
  FROM payload
  WHERE t.name = payload.name
  RETURNING t.id
)
INSERT INTO public.ingestion_task (
  name,
  description,
  source_type,
  source_config,
  destination_type,
  destination_config,
  sync_mode,
  sync_schedule,
  table_mapping,
  addax_config,
  airflow_enabled,
  airflow_dag_id,
  status,
  created_by,
  created_date,
  last_modified_by,
  last_modified_date,
  source_data_source_id,
  sync_config,
  quality_pre_check_enabled
)
SELECT
  payload.name,
  payload.description,
  payload.source_type,
  payload.source_config,
  payload.destination_type,
  payload.destination_config,
  payload.sync_mode,
  payload.sync_schedule,
  payload.table_mapping,
  payload.addax_config,
  payload.airflow_enabled,
  payload.airflow_dag_id,
  'active',
  'codex-sprint25',
  now(),
  'codex-sprint25',
  now(),
  payload.source_data_source_id,
  payload.sync_config,
  false
FROM payload
WHERE NOT EXISTS (SELECT 1 FROM updated);

COMMIT;

SELECT id, name, status, jsonb_array_length(table_mapping) AS mapped_tables
FROM public.ingestion_task
WHERE name = 'sprint25_project_datasurface'
ORDER BY id DESC
LIMIT 1;
