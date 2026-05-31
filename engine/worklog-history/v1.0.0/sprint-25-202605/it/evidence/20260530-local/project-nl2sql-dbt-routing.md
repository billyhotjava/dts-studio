# Sprint-25 Project NL2SQL dbt Routing Evidence

**Date**: 2026-05-30  
**Scope**: project-domain semantic pack, report catalog, business-object catalog, runtime query templates

## Unit Tests

```bash
mvn -pl dts-copilot-ai -Dtest=SemanticPackServiceTest,AgentBiReportCatalogServiceTest,BusinessObjectCatalogServiceTest test
```

Result:

```text
Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Covered expectations:

- Project semantic pack points to `public.xycyl_ads_project_overview`, `public.xycyl_dws_project_green_monthly`, and `public.xycyl_ads_contract_expiry_alert`.
- Project report catalog routes project overview, contract expiry, and status distribution questions to Sprint-25 dbt marts.
- Project business-object catalog exposes project site, contract, position, and green snapshot objects with source refs.

## Runtime DB Template Verification

Applied the project dbt routing SQL from `v1_0_0_021__project_dbt_query_templates.xml` to the local `dts-copilot-postgres` runtime DB. The `copilot-ai` container was rebuilt and restarted; Liquibase recorded the changeset in `copilot_ai.databasechangelog` and the container returned to `healthy`.

Active project templates:

```text
 template_code |                  target_view                  | is_active
---------------+-----------------------------------------------+-----------
 TPL-44        | public.xycyl_ads_project_overview             | t
 TPL-45        | public.xycyl_ads_contract_expiry_alert        | t
 TPL-46        | public.xycyl_ads_project_status_dist          | t
 TPL-47        | public.xycyl_ads_project_green_change_monthly | t
 TPL-48        | public.xycyl_dws_project_green_monthly        | t
 TPL-49        | public.xycyl_dwd_position_adjustment          | t
 TPL-50        | public.xycyl_ads_project_overview             | t
 TPL-51        | public.xycyl_dwd_project_green_snapshot       | t
```

Routing rules:

```text
 domain  |              primary_view               |                                                      secondary_views
---------+-----------------------------------------+----------------------------------------------------------------------------------------------------------------------------
 green   | public.xycyl_dwd_project_green_snapshot | ["public.xycyl_ads_project_overview","public.xycyl_dws_project_green_monthly"]
 project | public.xycyl_ads_project_overview       | ["public.xycyl_dws_project_green_monthly","public.xycyl_ads_project_status_dist","public.xycyl_ads_contract_expiry_alert"]
```

Legacy project templates were deactivated:

```text
 template_code |          target_view           | is_active
---------------+--------------------------------+-----------
 TPL-01        | v_project_green_current        | f
 TPL-02        | v_project_overview             | f
 TPL-03        | v_project_overview             | f
 TPL-04        | v_project_overview             | f
 TPL-20        | v_project_overview             | f
 TPL-21        | mart_project_fulfillment_daily | f
 TPL-24        | mart_project_fulfillment_daily | f
 TPL-30        | mart_project_fulfillment_daily | f
```

## 2026-05-31 Adminweb Template Update

After adding explicit adminweb reconciliation fields, `copilot-ai` was rebuilt and restarted again. Liquibase applied `v1_0_0_022__project_adminweb_query_templates.xml` successfully:

```text
Running Changeset: config/liquibase/changelog/v1_0_0_022__project_adminweb_query_templates.xml::v1.0.0-022-project-adminweb-reconcile-query-templates::dts-copilot-prs
Liquibase: Update has been successful. Rows affected: 5
```

Runtime DB verification:

```text
id
-------------------------------------------------------
v1.0.0-022-project-adminweb-reconcile-query-templates

template_code | is_active | uses_adminweb
--------------+-----------+--------------
TPL-44        | t         | t
TPL-47        | t         | t
TPL-48        | t         | t
TPL-50        | t         | t
```

The `dts-copilot-ai` container health check returned `healthy`.
