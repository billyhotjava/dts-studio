# Analytics Independent Login Design

## Problem

Analytics module cannot work standalone: `AnalyticsApiKeyAuthFilter` only accepts Bearer tokens, no login/setup UI exists, and `SetupStateService.isSetupCompleted()` is hardcoded to `true`.

## Two Auth Modes

1. **Standalone** — analytics has its own login/setup pages, session cookie auth
2. **Platform integration** — Bearer API Key from garden platform, no login UI needed

## Changes

### Backend

| File | Change |
|------|--------|
| `SetupStateService.isSetupCompleted()` | Query DB instead of `return true` |
| `AnalyticsApiKeyAuthFilter` | (done) Session cookie fallback + whitelist `/api/setup/**`, `/api/session/**` |
| `AnalyticsSecurityConfiguration` | (done) Whitelist setup/session paths |
| `application.yml` | (done) Default `platform-auth.enabled=false` |

### Frontend

| File | Change |
|------|--------|
| `routes.tsx` | Add `/auth/login` and `/auth/setup` fullscreen routes |
| `pages/auth/LoginPage.tsx` | Email/password form → `POST /api/session` → cookie → redirect `/` |
| `pages/auth/SetupPage.tsx` | Site name + admin account → `POST /api/setup` → auto-login |
| `pages/auth/auth.css` | Shared styles for auth pages |
| `AppLayout.tsx` | On mount: `GET /api/session/properties` to check `has-user-setup`; no session → redirect login |
| `analyticsApi.ts` `redirectToLogin()` | Change from console.warn to `window.location.href = basePath + "/auth/login"` |
| `i18n.ts` | Add login/setup i18n keys |

### Auth Guard Logic (AppLayout)

```
1. Has platform Bearer token? → platform mode, proceed
2. No token → GET /api/session/properties
   - Success + has-user-setup=false → redirect /auth/setup
   - Success + has-user-setup=true → proceed (has valid session cookie)
   - 401 → redirect /auth/login
```

### Login Page Flow

```
POST /api/session { username, password }
  → 200 + Set-Cookie: metabase.SESSION → redirect /
  → 401 → show error
```

### Setup Page Flow

```
GET /api/session/properties → extract setup-token
POST /api/setup { token, prefs: { site_name }, user: { email, first_name, last_name, password } }
  → 200 + Set-Cookie: metabase.SESSION → redirect /
  → 400 → show validation errors
```

## Non-Goals

- No OIDC/SSO for standalone mode (existing OidcAuthResource is separate)
- No password-reset UI (backend endpoints exist, UI deferred)
- No changes to platform integration mode
