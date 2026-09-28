# Analysis Workspace Peer Entry Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Productize the peer-entry workflow between `查询` and `AI Copilot` so they behave like one coherent analysis workspace instead of two loosely connected pages.

**Architecture:** Keep the current `analysis_draft` workflow as the backbone, but add a shared provenance layer, unify multi-surface source handling, and turn `查询` into a true analysis asset center. Reuse existing Query/Card, Dashboard, Screen, and Report Factory models instead of creating parallel assets.

**Tech Stack:** React 19 + Vite + TypeScript frontend, existing analytics REST APIs, sprint worklog structure under `worklog/v1.0.0/`, node-based contract tests, pnpm typecheck/build verification.

---

### Task 1: Extract shared analysis provenance model

**Files:**
- Create: `dts-copilot-webapp/src/pages/analysisProvenanceModel.ts`
- Test: `dts-copilot-webapp/tests/analysisProvenanceModel.test.ts`

**Step 1: Write the failing test**

Cover:
- draft -> dashboard seed card resolution
- draft -> report source resolution
- draft -> screen prompt generation
- common label/action summary resolution

**Step 2: Run test to verify it fails**

Run:
`node --experimental-strip-types --test dts-copilot-webapp/tests/analysisProvenanceModel.test.ts`

Expected:
missing module or missing helper failure

**Step 3: Write minimal implementation**

Implement pure helpers for:
- card seed selection
- report source resolution
- screen prompt generation
- provenance labels

**Step 4: Run test to verify it passes**

Run the same command and confirm PASS.

### Task 2: Add reusable provenance panel component

**Files:**
- Create: `dts-copilot-webapp/src/components/analysis/AnalysisProvenancePanel.tsx`
- Modify: `dts-copilot-webapp/src/components/analysis/AnalysisProvenancePanel.css`
- Test: `dts-copilot-webapp/tests/analysisProvenancePanel.test.ts`

**Step 1: Write the failing test**

Check that the component source exposes:
- title
- summary
- source badges
- action area

**Step 2: Run test to verify it fails**

Run:
`node --experimental-strip-types --test dts-copilot-webapp/tests/analysisProvenancePanel.test.ts`

**Step 3: Write minimal implementation**

Create a reusable panel with:
- title
- description
- optional raw question
- badges
- slot-like action area

**Step 4: Run test to verify it passes**

### Task 3: Replace per-page draft/fixed-report banners

**Files:**
- Modify: `dts-copilot-webapp/src/pages/CardEditorPage.tsx`
- Modify: `dts-copilot-webapp/src/pages/DashboardEditorPage.tsx`
- Modify: `dts-copilot-webapp/src/pages/ReportFactoryPage.tsx`
- Modify: `dts-copilot-webapp/src/pages/screens/ScreensPage.tsx`
- Test: `dts-copilot-webapp/tests/analysisDraftSurfaceEntry.test.ts`

**Step 1: Write the failing test**

Extend the source-level test to require all pages to import or use the shared provenance component.

**Step 2: Run test to verify it fails**

Run:
`node --experimental-strip-types --test dts-copilot-webapp/tests/analysisDraftSurfaceEntry.test.ts`

**Step 3: Write minimal implementation**

Replace duplicate banner blocks with the shared component.

**Step 4: Run test to verify it passes**

### Task 4: Deepen query asset center behavior

**Files:**
- Modify: `dts-copilot-webapp/src/pages/queryAssetCenterModel.ts`
- Modify: `dts-copilot-webapp/src/pages/CardsPage.tsx`
- Test: `dts-copilot-webapp/tests/queryAssetCenterModel.test.ts`

**Step 1: Write the failing test**

Add coverage for:
- source filter
- status filter
- recent analysis ordering
- draft lifecycle action visibility

**Step 2: Run test to verify it fails**

**Step 3: Write minimal implementation**

Add richer asset-center tabs/filters without changing route shape.

**Step 4: Run test to verify it passes**

### Task 5: Strengthen Copilot/query roundtrip

**Files:**
- Modify: `dts-copilot-webapp/src/components/copilot/InlineSqlPreview.tsx`
- Modify: `dts-copilot-webapp/src/components/copilot/CopilotChat.tsx`
- Modify: `dts-copilot-webapp/src/pages/CardEditorPage.tsx`
- Test: `dts-copilot-webapp/tests/queryDraftHandoff.test.ts`
- Test: `dts-copilot-webapp/tests/copilotAnalysisDraft.test.ts`

**Step 1: Write the failing test**

Require explicit back-link and status handoff behavior between copilot answer, draft editor, and saved query.

**Step 2: Run test to verify it fails**

**Step 3: Write minimal implementation**

Add:
- return-to-chat link
- draft/save status sync
- consistent action labels

**Step 4: Run test to verify it passes**

### Task 6: Multi-surface provenance persistence

**Files:**
- Modify: `dts-copilot-webapp/src/pages/DashboardEditorPage.tsx`
- Modify: `dts-copilot-webapp/src/pages/ReportFactoryPage.tsx`
- Modify: `dts-copilot-webapp/src/pages/screens/ScreensPage.tsx`
- Modify: `dts-copilot-webapp/src/pages/analysisDraftSurfaceEntry.ts`
- Test: `dts-copilot-webapp/tests/analysisDraftSurfaceEntry.test.ts`
- Test: `dts-copilot-webapp/tests/analysisProvenanceModel.test.ts`

**Step 1: Write the failing test**

Require:
- dashboard seed card reuse
- report source auto-selection
- screen prompt inheritance
- visible source actions

**Step 2: Run test to verify it fails**

**Step 3: Write minimal implementation**

Keep each surface backed by its existing object model, but persist enough source context to maintain traceability.

**Step 4: Run test to verify it passes**

### Task 7: IT validation and sprint closeout

**Files:**
- Modify: `worklog/v1.0.0/sprint-20-202603/it/test_analysis_workspace_peer_entry.sh`
- Modify: `worklog/v1.0.0/sprint-20-202603/it/acceptance-matrix.md`
- Modify: `worklog/v1.0.0/sprint-20-202603/README.md`
- Modify: `worklog/v1.0.0/sprint-queue.md`

**Step 1: Write the failing smoke expectation**

Document the exact commands and expected outcomes.

**Step 2: Run the smoke commands**

- node tests
- `pnpm --dir dts-copilot-webapp run typecheck`
- `pnpm --dir dts-copilot-webapp run build`
- sprint IT shell script

**Step 3: Update sprint docs**

Move statuses to `DONE` only after verification is green.

**Step 4: Commit**

Use focused Chinese commit messages by feature slice.
