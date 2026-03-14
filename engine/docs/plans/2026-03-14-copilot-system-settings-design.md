# Copilot System Settings Design

**Date:** 2026-03-14

**Goal:** Add an admin-only system settings area that manages site settings, LLM provider configuration, and copilot client API keys from the `dts-copilot-webapp`.

## Scope

- Add a new admin page in `dts-copilot-webapp`
- Manage site-level settings through `dts-copilot-analytics`
- Manage LLM provider settings through `dts-copilot-analytics` server-side proxying to `dts-copilot-ai`
- Manage copilot client API keys through `dts-copilot-analytics` server-side proxying to `dts-copilot-ai`

## Constraints

- `dts-copilot-analytics` already has session-cookie login and superuser checks
- `dts-copilot-ai` is stateless and API-key authenticated
- `COPILOT_ADMIN_SECRET` must stay server-side and never be exposed to the browser
- LLM provider API keys must be entered manually in the web UI, then stored server-side
- Provider API keys must not be returned in clear text after save

## Architecture

### Webapp

- Add route `/admin/settings/copilot`
- Add a sidebar entry under the existing admin section
- Render three panels:
  - Site settings
  - LLM provider settings
  - Copilot API key management

### Analytics Service

- Add admin-only aggregate REST endpoints under `/api/admin/copilot/**`
- Reuse `AnalyticsSessionService` to enforce authenticated superuser access
- Reuse `AnalyticsSettingRepository` for site settings
- Proxy provider and API key operations to `dts-copilot-ai`

### AI Service

- Keep persistence in `ai_provider_config` and `api_key`
- Harden provider responses so list/get endpoints do not return full API keys
- Support update semantics where blank API key input means “keep existing”

## Data Flow

### Site Settings

1. Browser calls `GET /api/admin/copilot/settings/site`
2. Analytics reads `analytics_setting`
3. Browser updates with `PUT /api/admin/copilot/settings/site`

### Provider Settings

1. Browser calls `GET /api/admin/copilot/providers`
2. Analytics validates superuser session
3. Analytics calls AI provider endpoints with service-side admin secret
4. Browser renders masked provider state
5. Browser submits create/update form with manual API key input
6. Analytics forwards request to AI service
7. AI service stores the API key but only returns masked state

### Copilot API Keys

1. Browser calls `GET /api/admin/copilot/api-keys`
2. Analytics proxies to AI service
3. Browser shows prefix and metadata only
4. Browser creates or rotates a key
5. Raw key is shown once in the success response only

## Security

- Browser never receives `COPILOT_ADMIN_SECRET`
- Provider list responses expose `hasApiKey` and `apiKeyMasked`, not raw secrets
- Update requests keep the old stored key when the submitted value is blank
- Only analytics superusers can access the management endpoints
- UI must not persist provider API keys to `localStorage`

## Error Handling

- Provider form keeps entered values when save fails
- “Test connection” returns provider reachability and backend error text
- Proxy failures return a consistent “Copilot configuration service unavailable” message
- Unauthorized users receive `403`

## Testing Strategy

### Backend

- `dts-copilot-ai`
  - provider list/get responses are masked
  - blank API key on update preserves the existing stored value
- `dts-copilot-analytics`
  - superuser-only access
  - site settings read/write
  - provider/API key proxy happy path and failure path

### Frontend

- settings API normalization helpers
- provider form behavior for create/update
- API key one-time reveal behavior
- page render and admin navigation visibility

