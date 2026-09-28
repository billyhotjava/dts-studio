# F5: 下钻与增强健壮性(全栈)

**优先级**: P2
**阶段**: P3
**状态**: DONE
**依赖**: F2(指标产物与渲染 / 画布 CanvasActions)、F4(路由结果接入 agent-first UI / 多候选)

## 目标

在 F1–F4 已打通「同步目录 → 优先路由 → 命中取值 → Indicator 产物渲染」闭环之后,补齐设计 §9 P3 的增强能力,把指标联邦从「能用」推到「健壮、好用、可观测」:

1. **交互式下钻**:在指标产物上提供「下钻」动作,调平台 `GET /indicators/{id}/drilldown?dimension=&period=`,在**同 artifactId 原地刷新**产物(复用 sprint-27 F4 upsert),维度/周期取自 `IndicatorDto.dimensionFields/timeGrain`。
2. **口径 version 变更提醒**:F1 同步时检测指标 `version` 变化,对已沉淀的指标卡/产物提示「口径已更新」,不静默用旧口径。
3. **多候选切换 + 取值微缓存**:F4 命中多候选时提供切换 UI;`dashboard/detail` 取值加秒~分钟级微缓存(后端),保护平台负载。
4. **可观测埋点**:指标路由命中率、指标 API 延迟/失败率、降级次数埋点(复用 copilot-ai actuator/Micrometer)。

## 设计依据

- `docs/superpowers/specs/2026-05-31-dts-platform-indicator-federation-design.md`
  - §7 跨服务集成与健壮性(目录同步 version 标记、取值微缓存、降级、可观测)
  - §9 分期 P3(drilldown 下钻 / 口径 version 变更提醒 / 多候选切换 / 取值微缓存 / 可观测埋点)
  - §5.5 时间/维度映射(问句维度/时间词 → `drilldown` 的 `dimension/period`,映射不上则降级不报错)

## 平台既有能力(指标业务零改动,F5 复用)

`GovernanceIndicatorResource`(`@RequestMapping("/api/governance")`,`/opt/prod/prs/source/dts-stack/source/dts-platform/src/main/java/com/yuzhi/dts/platform/web/rest/GovernanceIndicatorResource.java`):

- `GET /indicators/{id}/drilldown?dimension=&period=` → `IndicatorDashboardService.drilldown(id, dimension, period)`,返回 `List<Map>`,每行形如 `{dimension, metric_value, row_count}`;`dimension` 必须匹配 `^[a-zA-Z_][a-zA-Z0-9_]*$`(取自 `dimensionFields`),`period` 可选、按月 `DATE_TRUNC('month', dateColumn)` 过滤。
- `GET /indicators/dashboard?domain=&days=`、`GET /indicators/{id}/detail?days=` → 取值端点(T03 微缓存目标)。
- `GET /indicators/{id}/versions` → 版本列表;`IndicatorDto.version` → version 字段(T02 变更检测依据)。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | drilldown 交互下钻(单窗口产物内原地刷新) | P2 | DONE | F2, F4 |
| T02 | 口径 version 变更提醒(同步检测 → 资产卡提示) | P2 | DONE | F1, F2 |
| T03 | 多候选切换 + 取值微缓存(芯片切换 + 后端微缓存) | P2 | DONE | F3, F4 |
| T04 | 可观测埋点(命中率 / API 延迟·失败率 / 降级次数) | P3 | DONE | F1, F3 |

## 影响范围(汇总)

后端 `dts-copilot-ai`:
- `src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/`(F1 新建的 `PlatformIndicatorClient`、`IndicatorCatalogSyncService` 上叠加微缓存、version 检测、埋点)
- 取值 BFF 端点(F1/F2 暴露的 drilldown 透传端点)

前端 `dts-copilot-webapp`:
- `src/components/canvas/ArtifactCanvas.tsx`、`CanvasActions.tsx`、`src/types/artifact.ts`(单窗口指标下钻与 `drilldown` 动作类型)
- `src/pages/MetricAssetsPanel.tsx`(平台指标 version 变化本地检测与资产卡提示)
- F4 命中结果 UI(多候选切换芯片)

## 完成标准

- [x] 指标产物上有「下钻」动作,选维度/周期后**同 artifactId 原地刷新**,不新增产物、不丢上下文(T01)
- [x] 维度/周期映射不上时显式降级(隐藏非法维度或提示),绝不报错或静默(T01)
- [x] 同步/浏览检测到指标 `version` 变化时,资产卡/产物显示「口径已更新」提示(T02)
- [x] 多候选命中可在 UI 一键切换,切换后口径芯片/溯源同步更新(T03,复用 F4)
- [x] `dashboard/detail/drilldown` 取值有可配置 TTL 微缓存,降低对平台的实时压力(T03)
- [x] 指标路由命中率、指标 API 延迟/失败率、降级次数有 Micrometer 指标并可在 actuator `/actuator/metrics` 暴露(T04)
- [x] dts-platform 指标业务零代码改动(全 F5 仅消费既有端点;认证层只读白名单已补)
- [x] `it/README.md` 有本地/Mock/Live fixture 验证证据;服务认证 live 已通,drilldown 样本已用 `codex_sprint29_live_metric` 对账
