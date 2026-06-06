# xycyl finance dbt package v1

## Scope

This delivery package contains the dbt model chain for xycyl finance yearly/monthly and voucher statistics:

- ODS source declarations for `a_month_accounting`, `a_collection_record`, `f_voucher`, and `f_voucher_item`
- STG cleanup models
- DWD finance fact models
- DWS monthly finance and voucher summaries
- ADS query surfaces:
  - `xycyl_ads_finance_month_settlement`
  - `xycyl_ads_finance_collection`
  - `xycyl_ads_finance_voucher_monthly`

The package follows the warehouse boundary: application MySQL tables are only ODS ingestion sources. dbt models read from dts-stack warehouse ODS tables and produce DWS/ADS.

## Build Selector

Use this selector when triggering dbt from the UI:

```text
+xycyl_ads_finance_month_settlement +xycyl_ads_finance_collection +xycyl_ads_finance_voucher_monthly
```

The leading `+` includes upstream STG/DWD/DWS dependencies.

## Required ODS Tables

Before clicking build, these ODS tables must exist in `public`:

- `public.ods_ptr_mysql_a_month_accounting`
- `public.ods_ptr_mysql_a_collection_record`
- `public.ods_ptr_mysql_f_voucher`
- `public.ods_ptr_mysql_f_voucher_item`

The DDL used to create these tables is included in [ods-finance-ddl.sql](ods-finance-ddl.sql). It has been executed locally against `dts-stack-dts-pg-1/biadmin`; the voucher ODS tables were loaded by the `ptr_mysql_flow` ingestion task and used to build the voucher ADS output.

See [ods-table-info.md](ods-table-info.md) for source table mapping, field meanings, and model usage.

## Artifacts

- `xycyl-finance-dbt-models-v1.zip`: deployable dbt package for the finance model chain.
- `models.tsv`: model import manifest required by the platform ZIP batch importer.
- `ods-finance-ddl.sql`: non-destructive ODS table DDL (`CREATE TABLE IF NOT EXISTS`).
- `ods-table-info.md`: ODS table contract and business field notes.

## Verification

Validated locally with:

```bash
bash tests/test_xycyl_finance_dbt_model_contract.sh
docker exec -i dts-stack-dts-pg-1 psql -U biadmin -d biadmin -v ON_ERROR_STOP=1 < worklog/prs/v1/ods-finance-ddl.sql
docker exec dts-dbt sh -lc 'dbt parse --profiles-dir /opt/dbt/profiles --project-dir /opt/dbt'
docker exec dts-dbt sh -lc 'dbt compile --profiles-dir /opt/dbt/profiles --project-dir /opt/dbt --select +xycyl_ads_finance_month_settlement +xycyl_ads_finance_collection +xycyl_ads_finance_voucher_monthly'
```

Note: `dbt_project.yml` still emits an existing `log-path` deprecation warning; it is not introduced by this finance package.
