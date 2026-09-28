# Copilot Query Analysis Draft Workflow Plan

## Goal

Introduce an `analysis_draft` layer between Copilot chat results and formal query assets so that menu-driven query management and Copilot-driven exploration share one coherent workflow.

## Scope

- `analytics`: draft domain model, repository, service, REST resources
- `webapp`: Copilot actions, query page draft views, editor/source context, promotion flow
- `it`: smoke coverage for `copilot -> draft -> query -> visualization`

## Phases

### Phase 1: Draft Domain

- add `analysis_draft` schema and entity
- add list/detail/create/run/archive/save-card APIs
- keep status model explicit

### Phase 2: Query Asset Center

- extend `查询` page with draft-related filters
- render source badges and raw-question summaries
- allow opening drafts in editor with source banner

### Phase 3: Copilot Handoff

- add `保存草稿`
- add `在查询中打开`
- preserve `sessionId`, `messageId`, `databaseId`, `question`, `sql`, `explanation`

### Phase 4: Promotion Chain

- draft to saved query
- draft to visualization
- reuse in dashboard / screen / report factory

### Phase 5: IT Validation

- smoke `copilot -> save draft`
- smoke `draft -> open query`
- smoke `draft -> save card`
- smoke `draft -> create visualization`

## Risks

- Query/Card existing model may assume every persisted query is formal
- URL-based handoff still exists and should be migrated gradually
- Copilot answers must not create duplicate drafts silently

## Verification Strategy

- backend unit tests for draft model, service, and REST resources
- frontend tests for draft list filters, source banner, handoff actions
- `it/` smoke script for end-to-end draft promotion flow
