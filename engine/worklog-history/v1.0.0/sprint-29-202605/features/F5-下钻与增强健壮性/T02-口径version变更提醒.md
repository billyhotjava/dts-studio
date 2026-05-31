# T02: 口径 version 变更提醒

**优先级**: P2
**状态**: DONE
**依赖**: F1、F2

## 落地

- 后端 `IndicatorCatalogSyncService` 比对 `code -> version`,将变化写入 `SyncResult.caliberChangedCodes`。
- 前端资产库用 `localStorage` 记录上次看到的平台指标版本;再次加载目录时发现同一指标 version 变化,在资产卡与 Indicator 产物口径条显示「口径已更新」。

## 验证

- `mvn -pl dts-copilot-ai -Dtest=IndicatorCatalogSyncServiceTest test` PASS。
- `pnpm test -- src/pages/MetricAssetsPanel.test.tsx` PASS。
