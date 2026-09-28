# F6 v1 screen-backed asset runtime sync

Scope: replace temporary Sprint-27 dashboard/report assets with the reviewed PRS v1 screen prototypes from `worklog/prs/v1/screens`.

Reviewed source prototypes:

- `worklog/prs/v1/screens/*.json`: 12 files.
- Component counts: overview 24; the remaining board pages 22; drill pages 20.
- Source SQL references include `public.xycyl_ads_flowerbiz_*` ADS models and `public.xycyl_dwd_flowerbiz_main`.

Runtime sync result on local `copilot` database:

```text
analytics_dashboard: dashboards=0, sprint27_dashboards=0
analytics_dashboard_card: dashcards=0
analytics_report_template: active_prs=12, active_splits=0, active_screens=12
analytics_report_template: stale_prs_split_rows=0
```

The active PRS assets now point to `DBT_SCREEN` targets such as `screen.prs-flowerbiz-overview-v1` and keep the source path under `worklog/prs/v1/screens/<slug>.json`.

2026-06-01 P1c update:

```text
Liquibase changelog
0064-prs-flowerbiz-copilot-screen-records applied

analytics_screen:
prs_screen_count=12
min_id=290001
max_id=290012
component_count=258
placeholders=0

sample screen rows:
290001 PRS 租赁经营总览       components=24 variables=10 theme=legacy-dark
290006 PRS 项目客户经营看板   components=22 variables=10 theme=legacy-dark
290012 PRS 审批操作链路钻取   components=20 variables=10 theme=legacy-dark

analytics_report_template:
PRS-FLOWERBIZ-OVERVIEW target_object=screen.prs-flowerbiz-overview-v1
screenPreviewPath=/screens/290001/preview
```

Authenticated API check through the webapp proxy:

```text
GET /api/screens/290006?mode=draft
290006 PRS 项目客户经营看板 22 draft
```

Verification:

```text
pnpm test -- DashboardsPage fixedReportCatalogModel prsScreenShortcuts
Test Files 68 passed
Tests 276 passed

pnpm test -- fixedReportCatalogModel DashboardsPage specV2 copilotFixedReportMessage artifact ArtifactCanvas
Test Files 69 passed
Tests 277 passed

pnpm typecheck
passed

mvn -pl dts-copilot-analytics -DskipTests compile
BUILD SUCCESS

git diff --check
passed

docker compose build copilot-analytics copilot-webapp
passed

docker compose up -d copilot-analytics copilot-webapp
copilot-analytics healthy, copilot-webapp healthy

Liquibase changelog
0063-1-remove-temporary-sprint27-dashboard applied
0063-2-prs-flowerbiz-v1-screen-assets applied
0064-prs-flowerbiz-copilot-screen-records applied

docker compose ps
dts-copilot-analytics healthy
dts-copilot-webapp healthy

host health
http://localhost:50092/actuator/health => {"status":"UP"}
http://localhost:50080/ => webapp-ok
```

2026-06-01 P1d logical datasource update:

```text
Goal
PRS v1 screen assets no longer bind to environment-specific database ids.  They bind to
logical alias prs.flowerbiz.federated so local, multi-warehouse, and federated-query
environments can route the same screen JSON through configured database aliases.

Backend
Added AnalyticsDatabaseAliasResolver.
/api/dataset accepts database="prs.flowerbiz.federated" and resolves it to analytics_database.id=8.
Screen warmup resolves databaseAlias before databaseId / connectionId.

Governance/config
DTS dbt模型库 details_json:
{"dataSourceId": 17, "logicalSourceAliases": ["prs.flowerbiz.federated", "prs.flowerbiz.mart"]}

Liquibase
0064 keeps a validCheckSum for the checksum already applied in local runtime:
9:45fe6734ac0d2919b388c8a9b0957182
0065-prs-flowerbiz-screen-logical-datasource applied successfully.

Runtime binding check
12 PRS screens found, ids 290001-290012.
All SQL components use databaseAlias=prs.flowerbiz.federated.
databaseId_components=0 for every PRS screen.

Sample API checks
GET /api/screens/290006?mode=draft
290006 PRS 项目客户经营看板 22 draft
aliases=['prs.flowerbiz.federated']

POST /api/dataset
body.database=prs.flowerbiz.federated
result: database_id=8, status=completed, rows=[[1]]

Browser route check
http://localhost:50080/assets renders the 12 PRS screen assets.
Clicking PRS 项目客户经营看板 opens:
http://localhost:50080/screens/290006/preview

Screenshot evidence
/opt/prod/prs/source/asset-library-authenticated-ok.png
/opt/prod/prs/source/prs-screen-290006-preview-loaded.png

Residual note
When opening screen 290006 in the reused browser profile, stale filter values
project=B422室 and customer=000 produced empty business results. The link, screen route,
logical datasource resolution, and subsequent dataset calls are healthy; default filter
value cleanup remains a separate UX/data-default task.
```

Verification:

```text
mvn -pl dts-copilot-analytics -Dtest=AnalyticsDatabaseAliasResolverTest,PrsFlowerbizScreenLogicalDatasourceSeedTest,DatabaseResourceTest#updatePreservesConfiguredLogicalDatasourceAliasesWhenRefreshingConnectionDetails test
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0

pnpm --dir dts-copilot-webapp test src/pages/screens/hooks/useCardDataSource.test.ts src/pages/databaseEntryModel.test.ts src/pages/screens/specV2.test.ts
Test Files 3 passed
Tests 5 passed

pnpm --dir dts-copilot-webapp typecheck
passed

mvn clean package -DskipTests
BUILD SUCCESS

docker compose build copilot-analytics copilot-webapp
passed

docker compose up -d --force-recreate copilot-analytics copilot-webapp copilot-proxy
dts-copilot-analytics healthy
dts-copilot-webapp healthy
dts-copilot-proxy started

curl http://localhost:50092/actuator/health
{"status":"UP"}
```

2026-06-01 P1e PRS screen runtime bugfix:

```text
Issue
Screen 290009 could render initial data and then collapse to zero because dynamic filter-select
components auto-selected the first loaded option. The same screen also showed
ERROR: relation "public.xycyl_ads_flowerbiz_lease_detail" does not exist because the PRS
ADS detail views existed in dts-stack model files but were not materialized in the target dbt DB.

Datasource
databaseAlias=prs.flowerbiz.federated resolves to analytics_database.id=8, name=DTS dbt模型库.

Runtime dbt mart repair
Created/refreshed these public views in the target dbt database:
xycyl_ads_flowerbiz_lease_detail     rows=152211
xycyl_ads_flowerbiz_pending          rows=1326
xycyl_ads_flowerbiz_change_log       rows=1202
xycyl_ads_flowerbiz_recovery_detail  rows=20963
xycyl_ads_flowerbiz_audit_trail      rows=293283

Frontend fix
filter-select keeps empty runtime value as "全部" unless an explicit defaultValue is configured.

Verification
pnpm vitest run src/pages/screens/renderers/shared/chartUtils.test.ts src/pages/screens/hooks/useCardDataSource.test.ts
Test Files 2 passed
Tests 5 passed

mvn -pl dts-copilot-analytics -Dtest=PrsFlowerbizScreenLogicalDatasourceSeedTest,AnalyticsDatabaseAliasResolverTest test
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0

pnpm typecheck
passed

pnpm build
passed

docker compose build copilot-webapp && docker compose up -d copilot-webapp
dts-copilot-webapp healthy

POST /api/dataset database=prs.flowerbiz.federated
result: status=completed, database_id=8, rows=[[152211]]

Browser check
/screens/290009/preview after 5 seconds:
project select value="", customer select value="", relation error=false, KPI non-zero.
Screenshot artifact: prs-screen-290009-after-fix.png

Residual
dbt run for the selected PRS models is currently blocked before execution by an unrelated
stale generated model: models/dws/semantic/dws_s27_1777792772_order.sql references missing
ods_s27_1777792772_orders.
```
