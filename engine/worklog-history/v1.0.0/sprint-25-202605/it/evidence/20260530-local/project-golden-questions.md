# Sprint-25 F3/T01 Project Golden Questions

**Date**: 2026-05-30  
**Scope**: project-domain Golden Questions baseline for Sprint-25 dbt mart/query-template routing.

## Result

- Golden Questions: 15/15 present.
- Mart fast path: 12/15.
- Unique fast templates: 8/8 (`TPL-44` through `TPL-51`).
- Guardrails: 1 object-profile question and 1 clarification-guard question keep unresolved P0 amount/caliber decisions out of silent SQL aggregation.

## Verification Commands

```bash
bash worklog/v1.0.0/sprint-25-202605/it/test_project_golden_questions.sh
RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_golden_questions.sh
```

## Live Mart Surface

The live run verifies every `public.xycyl_*` target in `it/sql/project_golden_questions.tsv` exists in the local DTS PostgreSQL mart database. Populated target counts at verification time:

```text
public.xycyl_ads_project_overview                 222
public.xycyl_ads_contract_expiry_alert            284
public.xycyl_ads_project_status_dist              4
public.xycyl_ads_project_green_change_monthly     239
public.xycyl_dws_project_green_monthly            239
public.xycyl_dwd_position_adjustment              21009
public.xycyl_dwd_project_green_snapshot           36295
```

## Boundary

The Golden set validates routing and mart readiness. It does not mark `p_project_green` snapshot grain, rent/cost multiplication, active-only filtering, or group-count semantics as business-final; those remain in `assets/project-caliber-decisions.md` until the business owner fills `RESOLVED` decisions.
