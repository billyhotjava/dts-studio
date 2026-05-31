# Sprint-25 Project dbt Build After Ingestion

**Date**: 2026-05-30  
**Environment**: `dts-dbt:1.10.0` container, local DTS PostgreSQL `biadmin.public`

## Command

```bash
DTS_PG_PASSWORD_VALUE="$(docker exec v223-dts-pg-1 sh -lc 'printf %s "$POSTGRES_PASSWORD"')"
docker run --rm --network container:v223-dts-pg-1 \
  -e DTS_PG_PASSWORD="$DTS_PG_PASSWORD_VALUE" \
  -v "$PWD/worklog/v1.0.0/sprint-25-202605/assets/xycyl-project-dbt-model:/project" \
  -v "$PWD/worklog/v1.0.0/sprint-25-202605/it/profiles:/profiles:ro" \
  dts-dbt:1.10.0 build \
  --project-dir /project \
  --profiles-dir /profiles \
  --target-path /tmp/dts-copilot-sprint25-project-dbt-build-target \
  --log-path /tmp/dts-copilot-sprint25-project-dbt-build-logs \
  --select tag:xycyl-project
```

## Result

```text
Finished running 16 table models, 50 data tests, 11 view models in 0 hours 0 minutes and 6.34 seconds (6.34s).
Completed with 1 warning:
Warning in test relationships_xycyl_dwd_project_green_snapshot_project_id__project_id__ref_xycyl_dim_project_
Got 286 results, configured to warn if != 0
Done. PASS=76 WARN=1 ERROR=0 SKIP=0 NO-OP=0 TOTAL=77
```

## 2026-05-31 Adminweb Fields Rerun

After adding explicit `*_adminweb_*` fields for ProjectSummary reconciliation, the same build was rerun against local DTS PostgreSQL:

```text
Finished running 16 table models, 50 data tests, 11 view models in 0 hours 0 minutes and 6.59 seconds (6.59s).
Completed with 1 warning:
Warning in test relationships_xycyl_dwd_project_green_snapshot_project_id__project_id__ref_xycyl_dim_project_
Got 286 results, configured to warn if != 0
Done. PASS=76 WARN=1 ERROR=0 SKIP=0 NO-OP=0 TOTAL=77
```

## Model Row Counts

```text
                 model                  | row_count
----------------------------------------+-----------
 xycyl_ads_contract_expiry_alert        |       284
 xycyl_ads_project_green_change_monthly |       239
 xycyl_ads_project_overview             |       222
 xycyl_ads_project_status_dist          |         4
 xycyl_dim_contract                     |       284
 xycyl_dim_customer                     |       165
 xycyl_dim_goods                        |      6517
 xycyl_dim_position                     |     14820
 xycyl_dim_project                      |       222
 xycyl_dwd_position_adjustment          |     21009
 xycyl_dwd_project_green_snapshot       |     36295
 xycyl_dws_project_green_monthly        |       239
```

## Notes

- The only warning is an expected soft relationship warning: 286 `p_project_green.project_id` values do not resolve to active `xycyl_dim_project`.
- This proves the Sprint-25 dbt package builds on non-empty project-domain ODS data.
- P0 business decisions remain open for rent/cost multiplication and group-count semantics.
