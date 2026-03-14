# NL2SQL Chat-to-Visualization Pipeline — Design Spec

## Problem

DTS Copilot has all the building blocks for NL2SQL (schema recall, SQL generation, safety checks, query execution, chart rendering, AI chat), but they are not wired together into a usable end-to-end flow. Users cannot ask a natural language question and get a visualization.

## Decision Record

| Question | Answer |
|----------|--------|
| Interaction model | Chat + jump: CopilotChat generates SQL, one-click jump to CardEditorPage for visualization |
| Database access | C: Pre-configured default DB + UI for adding more sources |
| Schema recall | Token matching + synonyms (current), no vector search needed for 50-200 tables |
| Auto-execute on jump | Yes: CardEditorPage auto-runs SQL when `autorun=1` URL param present |
| SQL complexity (phase 1) | Single table + simple JOIN (2-3 tables), aggregations |
| ETL/ELT | Not in phase 1. Phase 2: ELT with incremental sync to analytics DB |

## Architecture

```
User asks question in CopilotChat (with datasource selector)
    ↓
POST /api/ai/agent/chat/send { userMessage, datasourceId }
    ↓
Agent orchestration:
  1. schema_lookup tool → table/column metadata
  2. Nl2SqlSemanticRecallService → schema candidates + synonyms + few-shot
  3. Nl2SqlService → LLM generates SQL
  4. SqlSafetyChecker → validate read-only
  5. execute_query tool → preview results (optional)
    ↓
CopilotChat renders: text explanation + SQL code block + "Create Visualization" button
    ↓
Button click → CardEditorPage?sql=<encoded>&db=<id>&name=<name>&autorun=1
    ↓
CardEditorPage auto-executes SQL → ChartRenderer renders results
    ↓
User adjusts chart type → saves to dashboard
```

## Changes Required

### Backend

#### 1. Default Database Auto-Registration

New service: `DefaultDatabaseInitService`

On startup, reads pre-configured databases from `application.yml`:

```yaml
dts:
  analytics:
    default-databases:
      - name: "园林业务库"
        engine: postgresql
        host: ${BIZ_DB_HOST:localhost}
        port: ${BIZ_DB_PORT:5432}
        db: ${BIZ_DB_NAME:garden}
        user: ${BIZ_DB_USER:readonly}
        password: ${BIZ_DB_PASSWORD:}
        auto-sync-metadata: true
```

If the database name is not already registered in `analytics_database`, creates the record and triggers metadata sync (table/field scan).

#### 2. Agent Chat Context — datasourceId Pass-Through

`AgentChatResource.send()` must accept `datasourceId` from the request body and pass it into the `ToolContext` so that `schema_lookup` and `execute_query` tools operate on the correct database.

Current: `ToolContext(userId, sessionId, dataSourceId)` — dataSourceId may be null or hardcoded.
Change: populate from request body's `datasourceId` field.

#### 3. NL2SQL Agent Orchestration Enhancement

The agent's system prompt needs to include the NL2SQL workflow:
1. When user asks a data question, call `schema_lookup` first
2. Use schema context + `Nl2SqlSemanticRecallService` for recall
3. Generate SQL via `Nl2SqlService`
4. Optionally preview via `execute_query`
5. Return SQL in a structured format the frontend can extract

### Frontend

#### 4. CopilotChat — Datasource Selector

Add a dropdown at the top of CopilotChat that lists available databases from `analyticsApi.listDatabases()`. Selected `datasourceId` is sent with every `aiAgentChatSend()` call.

Default: first database (or pre-configured default).

#### 5. CopilotChat — "Create Visualization" Button

When agent response contains SQL (detected by code block or structured tool output):
- Extract the SQL string
- Show a button: "SQL 创建可视化"
- On click: `window.location.href = /questions/new?sql=${encodeURIComponent(sql)}&db=${datasourceId}&name=${encodeURIComponent(title)}&autorun=1`

This pattern already exists in `Nl2SqlEvalPage.tsx` and should be extracted into a shared utility.

#### 6. CardEditorPage — `autorun=1` Support

Current: CardEditorPage pre-populates SQL from URL params but requires manual "Run" click.
Change: When `autorun=1` is present, automatically trigger query execution after mount.

### Not In Scope (Phase 1)

- ETL/ELT pipeline
- Cross-database JOIN
- Vector-based semantic recall (pgvector)
- Complex SQL: window functions, 4+ table JOINs
- Inline chart rendering inside CopilotChat
- Streaming SQL generation
- Password-reset UI for analytics login

## Evolution Path

| Phase | Scope |
|-------|-------|
| Phase 1 (this spec) | Direct business DB query, simple JOIN, chat → jump visualization |
| Phase 2 | ELT: incremental sync to analytics DB, pre-aggregation views, cross-DB JOIN |
| Phase 3 | dbt/Airflow integration, vector recall, inline chat charts |
