# Login NL2SQL Ad Showcase Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Refine the DTS Copilot login page so the left panel reads as a dynamic NL2SQL product advertisement instead of a literal in-product dashboard.

**Architecture:** Keep the right-side login flow unchanged and replace the left-side static four-card layout with a continuous NL2SQL analysis chain. The new structure should show natural language input, schema/safety reasoning, SQL generation, and result output as one branded visual path, backed by CSS motion and glow effects rather than dashboard metrics.

**Tech Stack:** React, TypeScript, CSS, node:test, Vite

---

### Task 1: Lock the new ad-style layout in a regression test

**Files:**
- Modify: `dts-copilot-webapp/tests/loginPageLayout.test.ts`
- Test: `dts-copilot-webapp/tests/loginPageLayout.test.ts`

**Step 1: Write the failing test**

Add assertions for the new ad-style markers such as `analysis-chain`, `analysis-node--prompt`, `analysis-node--guard`, `analysis-node--sql`, and `analysis-node--result`.

**Step 2: Run test to verify it fails**

Run: `cd dts-copilot-webapp && node node_modules/.pnpm/esbuild@0.25.12/node_modules/esbuild/bin/esbuild tests/loginPageLayout.test.ts --bundle --platform=node --format=esm --outfile=/tmp/loginPageLayout.test.mjs && node --test /tmp/loginPageLayout.test.mjs`

Expected: FAIL because the new showcase structure does not exist yet.

### Task 2: Implement the NL2SQL ad showcase

**Files:**
- Modify: `dts-copilot-webapp/src/pages/auth/LoginPage.tsx`
- Modify: `dts-copilot-webapp/src/pages/auth/auth.css`

**Step 1: Write minimal implementation**

Replace the left-side `nl2sql-showcase` grid with a continuous `analysis-chain` structure and add the corresponding CSS for dynamic beams, nodes, glow surfaces, and capability cards.

**Step 2: Run test to verify it passes**

Run the same bundled node test command from Task 1.

Expected: PASS

### Task 3: Verify the page still builds cleanly

**Files:**
- Modify: `dts-copilot-webapp/src/pages/auth/LoginPage.tsx`
- Modify: `dts-copilot-webapp/src/pages/auth/auth.css`

**Step 1: Run typecheck**

Run: `cd dts-copilot-webapp && ./node_modules/.bin/tsc --noEmit`

Expected: PASS

**Step 2: Run production build**

Run: `cd dts-copilot-webapp && LEGACY_BROWSER_BUILD=0 VITE_CACHE_DIR=.vite-cache ./node_modules/.bin/vite build`

Expected: PASS
