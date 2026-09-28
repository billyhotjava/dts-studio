# Data Source Form Hardening Design

**Date:** 2026-03-15

**Goal:** Harden the manual datasource entry flow so users select a supported database type from a dropdown, get immediate field validation, and see the real backend error instead of a generic 502.

## Problem

The current manual datasource form still allows avoidable failures:

- database type is a free-text input
- required MySQL/Postgres fields can be submitted empty
- `analytics` collapses upstream AI validation failures into a generic `创建数据源失败`

This makes the UI look broken even when the root cause is a missing form field.

## Scope

- Replace the manual datasource type text input with a dropdown
- Limit manual entry to database types the current form actually supports
- Add client-side validation for required fields before `test` or `create`
- Preserve the upstream AI validation message when datasource creation fails

## Approach

### Webapp

- Use a dropdown for datasource type
- Support only:
  - `PostgreSQL`
  - `MySQL`
- Add a small form-validation helper for:
  - name
  - type
  - host
  - database
- Show field errors inline and block submit when validation fails

### Analytics

- Stop swallowing the copilot-ai datasource create error
- Parse the upstream error response and surface the message through `/api/platform/data-sources`

## Non-Goals

- Do not add Oracle/DM/custom JDBC form fields in this change
- Do not redesign the datasource page layout
- Do not change analytics import or schema sync contracts

## Testing

- Webapp unit tests for:
  - supported dropdown options
  - manual form validation
- Analytics unit test for:
  - preserving AI datasource creation error details
