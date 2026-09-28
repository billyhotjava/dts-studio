# Data Source Entry Design

**Date:** 2026-03-15

**Goal:** Restore the intended "add data source" capability in the `Data` module by supporting manual data-source creation alongside the existing import flow.

## Scope

- Keep `/data/new` as the single entry for adding databases in analytics
- Add a manual data-source creation form in `dts-copilot-webapp`
- Add datasource CRUD and connection-test APIs in `dts-copilot-ai`
- Let `dts-copilot-analytics` import AI-managed datasources into `analytics_database`
- Preserve existing metadata sync behavior after a datasource is imported

## Problem

The current `Data` flow only supports importing a pre-existing platform datasource:

- `DatabaseNewPage` lists platform datasources and imports one into analytics
- `DatabaseResource.create()` rejects non-platform payloads
- Users cannot create a datasource from the web UI even though the sprint design and downstream Copilot flows assume the capability exists

## Architecture

### AI Service

- Introduce datasource persistence owned by `dts-copilot-ai`
- Expose datasource CRUD and connection-test APIs
- Store connection details server-side
- Return masked secrets where needed

### Analytics Service

- Stop treating datasource import as platform-only
- Read available datasource summaries from AI
- Import a selected datasource into `analytics_database`
- Resolve JDBC details through AI when validating connections and syncing metadata

### Webapp

- Convert `/data/new` into a dual-mode page:
  - `Manual`
  - `Import Existing`
- `Manual` creates the datasource in AI first, then imports it into analytics and runs schema sync
- `Import Existing` keeps the current pick-and-import behavior

## Data Model

### AI datasource

- `id`
- `name`
- `type`
- `jdbcUrl`
- `username`
- `password`
- `description`
- `status`
- `props`
- `secrets`
- timestamps

### Analytics database

- Keep `analytics_database` as the metadata cache
- `detailsJson` stores `dataSourceId` instead of `platformDataSourceId`
- Continue syncing tables and fields into `analytics_table` and `analytics_field`

## API Contract

### AI

- `GET /api/ai/copilot/datasources`
- `POST /api/ai/copilot/datasources`
- `GET /api/ai/copilot/datasources/{id}`
- `PUT /api/ai/copilot/datasources/{id}`
- `DELETE /api/ai/copilot/datasources/{id}`
- `POST /api/ai/copilot/datasources/{id}/test`

### Analytics

- `GET /api/platform/data-sources` becomes a generic list of importable datasources
- `POST /api/database` accepts `details.dataSourceId`
- `POST /api/database/validate` accepts `details.dataSourceId`

## UX

### `/data`

- Keep the existing `添加数据源` button

### `/data/new`

- Add mode switch tabs:
  - `手动添加`
  - `导入已有`
- Manual form fields:
  - name
  - type/engine
  - host
  - port
  - database/schema name
  - username
  - password
  - optional description
- Actions:
  - `测试连接`
  - `创建并导入`

## Security

- Password stays server-side after submit
- List responses do not echo raw passwords
- Only analytics superusers may create/import datasources

## Error Handling

- AI datasource create failure stays on the manual form
- Analytics import failure does not silently discard the AI datasource; the UI should show the import error explicitly
- Connection test errors surface backend messages directly

## Testing Strategy

### AI

- datasource create/list/get/update/delete
- connection-test success and failure

### Analytics

- import create accepts `dataSourceId`
- legacy `platformDataSourceId` remains readable for compatibility
- schema sync resolves JDBC through AI datasource details

### Webapp

- `/data/new` mode switch
- manual payload shaping
- create -> import -> sync happy path
- import-existing still works
