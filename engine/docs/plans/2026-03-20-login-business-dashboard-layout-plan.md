# Login NL2SQL Showcase Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Rebuild the login page into an `NL2SQL`-centered product landing page for DTS Copilot while preserving the existing login behavior.

**Architecture:** Keep the current session POST flow untouched and replace only the page structure and auth styles. The left side becomes a `自然语言 -> SQL -> 结果可视化` showcase, with schema awareness and SQL safety highlighted, so the page communicates the actual DTS Copilot product positioning instead of a business dashboard.

**Tech Stack:** React 19, TypeScript, CSS, node:test, esbuild, Vite

---

### Task 1: Add a failing layout regression test

**Files:**
- Create: `dts-copilot-webapp/tests/loginPageLayout.test.ts`

**Step 1: Write the failing test**

Assert that `LoginPage.tsx` contains:
- `DTS 智能数据分析助手`
- `AI-Native 智能数据分析平台`
- NL2SQL showcase section markers
- four NL2SQL flow labels
- three capability card labels
- short platform description

**Step 2: Run test to verify it fails**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && node node_modules/.pnpm/esbuild@0.25.12/node_modules/esbuild/bin/esbuild tests/loginPageLayout.test.ts --bundle --platform=node --format=esm --outfile=/tmp/loginPageLayout.test.mjs && node --test /tmp/loginPageLayout.test.mjs`
Expected: FAIL because the current login page still contains business-showcase content instead of the NL2SQL product flow.

### Task 2: Rebuild LoginPage structure

**Files:**
- Modify: `dts-copilot-webapp/src/pages/auth/LoginPage.tsx`

**Step 1: Replace the simple centered card**

Build a structure with:
- top centered product heading and subtitle
- left NL2SQL showcase area
- right login panel
- bottom-left short description

**Step 2: Keep behavior untouched**

Preserve:
- `fetch(${basePath}/api/session)`
- session storage username persistence
- redirect to `${basePath}/`

**Step 3: Re-run test**

Run the test from Task 1.
Expected: PASS.

### Task 3: Implement the NL2SQL showcase and auth styles

**Files:**
- Modify: `dts-copilot-webapp/src/pages/auth/auth.css`

**Step 1: Add page-level layout styles**

Implement:
- dark background
- top title rail
- main grid
- left showcase stage
- right auth side panel

**Step 2: Add showcase styles**

Style:
- NL2SQL conversion flow
- prompt card
- SQL preview card
- result preview card
- three capability cards
- short platform description

**Step 3: Add responsive behavior**

Collapse to one column on narrower widths while keeping the title visible and login usable.

### Task 4: Full verification

**Files:**
- Verify: `dts-copilot-webapp/src/pages/auth/LoginPage.tsx`
- Verify: `dts-copilot-webapp/src/pages/auth/auth.css`
- Verify: `dts-copilot-webapp/tests/loginPageLayout.test.ts`

**Step 1: Run regression test**

Run the Task 1 command again.
Expected: PASS.

**Step 2: Run typecheck**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && ./node_modules/.bin/tsc --noEmit`
Expected: PASS.

**Step 3: Run build**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && LEGACY_BROWSER_BUILD=0 VITE_CACHE_DIR=.vite-cache ./node_modules/.bin/vite build`
Expected: PASS.
