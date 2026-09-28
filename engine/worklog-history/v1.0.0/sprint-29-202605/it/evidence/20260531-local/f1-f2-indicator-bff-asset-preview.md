# 2026-05-31 Local Evidence: F1/F2 Indicator BFF + Asset Preview

## Scope

- F1 analytics BFF: platform indicator catalog/value client, auth config, degraded response.
- F2 asset library: platform indicators tab, detail preview rendered as `type:'indicator'` artifact, Agent handoff URL.
- F4 partial: assistant messages with `trace.metricCaliber` show platform indicator badge.

## Commands

```bash
mvn -pl dts-copilot-analytics -Dtest=PlatformIndicatorClientTest,PlatformIndicatorResourceTest test
```

Result: PASS, 5 tests.

```bash
mvn -pl dts-copilot-analytics test
```

Result: PASS, 104 tests.

```bash
pnpm test -- src/pages/MetricAssetsPanel.test.tsx src/types/artifact.test.ts src/components/canvas/ArtifactCanvas.test.tsx src/components/copilot/MessageList.platformIndicator.test.tsx
```

Result: PASS, 65 files / 262 tests.

```bash
pnpm test
```

Result: PASS, 65 files / 262 tests.

```bash
pnpm typecheck
```

Result: PASS.

```bash
pnpm build
```

Result: PASS. Vite build completed; only existing chunk-size warning.

```bash
mvn test
```

Result: PASS. Reactor modules `dts-copilot-ai` and `dts-copilot-analytics` succeeded; backend tests total 173 + 104.

## Notes

- Live contract against real dts-platform is closed in `f1-f5-indicator-routing-drilldown.md` with the `codex_sprint29_live_metric` local published indicator fixture.
- The Sprint-28 single-window decision is preserved: asset preview renders inline with `ArtifactCanvas`; no right-side preview pane is reintroduced.
