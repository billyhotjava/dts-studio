# Copilot Dynamic Datasource Binding Design

## Problem

Current Copilot chat sessions can carry a `dataSourceId`, but the AI tool layer does not use it.

- `analytics` forwards the selected UI database identifier directly to `copilot-ai`
- `copilot-ai` persists that value on the chat session
- `schema_lookup` and `execute_query` still use the application's primary `DataSource`

This causes NL2SQL and schema inspection to read the wrong database, which is why MySQL business databases can appear as an empty `public` schema.

## Goal

Make Copilot tools execute against the user-selected external datasource, not the application primary datasource.

## Chosen Approach

### 1. Resolve analytics database IDs to AI datasource IDs at the proxy boundary

The web UI currently selects an `analytics_database.id`. That is the correct UI concept, so the webapp does not need to change.

`dts-copilot-analytics` will:

- inspect the selected database's `details_json`
- extract `dataSourceId`
- forward the resolved AI datasource ID to `copilot-ai`

If the selected database is not linked to an AI datasource, the API should return `400` instead of forwarding a wrong ID.

### 2. Add a dedicated AI tool datasource registry

`dts-copilot-ai` will add a small runtime registry that:

- looks up `AiDataSource` by `ToolContext.dataSourceId`
- creates or reuses a JDBC pool for that datasource
- returns a connection for `schema_lookup` and `execute_query`

The registry must reject missing datasource selections explicitly. Tools should no longer fall back to the application primary datasource.

### 3. Keep the initial scope narrow

This fix is only for generic external-datasource tools:

- `schema_lookup`
- `execute_query`

Garden-specific built-in tools are left unchanged in this pass.

## Error Handling

- No database selected: return a clear tool failure telling the user to choose a database first.
- Analytics database exists but is not linked to an AI datasource: return `400`.
- AI datasource missing or deleted: tool returns a clear failure.
- Unsupported driver: fail fast with the driver error.

## Testing

### Analytics

- selected analytics database ID resolves to linked AI datasource ID
- raw numeric IDs still pass through when they are already AI datasource IDs
- linked database without `dataSourceId` fails instead of forwarding the wrong ID

### AI

- `schema_lookup` fails when no external datasource is selected
- `schema_lookup` reads table metadata from the external datasource returned by the registry
- `execute_query` reads query results from the external datasource returned by the registry

## Notes

- `dts-copilot-ai` needs a MySQL runtime driver because the tool execution now happens inside the AI service.
- This design does not change the webapp selector UX.
