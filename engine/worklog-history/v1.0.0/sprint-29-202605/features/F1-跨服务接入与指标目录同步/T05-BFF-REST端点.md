# T05: BFF REST 端点(给 webapp)

**优先级**: P0
**状态**: DONE
**依赖**: T04

## 目标

为 dts-copilot-webapp 暴露平台指标目录与取值 BFF,保持现有 webapp → analytics 的调用边界,不让前端直连 dts-platform。

## 实现

- 新增 `PlatformIndicatorResource`:
  - `GET /api/platform/indicators`
  - `GET /api/platform/indicators/dashboard?days=`
  - `GET /api/platform/indicators/{indicatorId}/detail?days=`
  - `GET /api/platform/indicators/{indicatorId}/drilldown?dimension=&period=`
- 端点复用 `MetabaseAuth.requireUser`,与 analytics 既有 API 授权方式一致。
- webapp 通过 dev/proxy 前缀消费为 `/api/analytics/platform/indicators*`。

## 设计说明

原计划写作 `copilot-ai` BFF,但当前运行链路中 webapp 的 BI/资产 API 已集中走 `dts-copilot-analytics`。本 Task 先落 analytics BFF,避免引入 analytics ↔ ai 反向调用。copilot-ai 的目录同步和指标优先路由仍由 F1/T02-T03 与 F3 处理。

## 验证

- `mvn -pl dts-copilot-analytics -Dtest=PlatformIndicatorClientTest,PlatformIndicatorResourceTest test`
- `mvn -pl dts-copilot-analytics test`

## 完成标准

- [x] 目录与三类取值端点可被 webapp 消费。
- [x] 鉴权缺失时按 analytics 既有授权返回。
- [x] 响应契约与 `PlatformIndicatorCatalogResponse` / `PlatformIndicatorValueResponse` 对齐。
- [x] Live contract 已补:BFF 目录、dashboard、detail、drilldown 返回 HTTP 200 且 `degraded=false`;local fixture 下 dashboard 1 行、detail 2 行、drilldown 3 行。
