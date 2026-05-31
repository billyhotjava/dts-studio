# T01: 盘点并锁定真实 signals 数据契约

**优先级**: P1
**状态**: DONE
**依赖**: F1-T02

## 目标

确认前端信号视图应该消费哪条真实数据链路,避免继续用 `buildPlaceholderSignals()` 伪造业务预警。

## 技术设计

- 采用 `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/AiCopilotResource.java` 的 `GET /api/ai/copilot/signals?domain=flowerbiz`。
- 后端从 `OntologyService.load(domain)` 与 `OntologyModel.buildSignalPlans()` 生成 `SignalSummary`。
- 前端 DTO 字段包含 `id`、`title`、`severity`、`description`、`source`、`objectName`、`linkedActions`。
- 明确不采用 `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/AlertResource.java` 的 `/api/alert`,该接口是资产告警配置面。

## 影响范围

- `dts-copilot-webapp/src/api/types.ts`
- `dts-copilot-webapp/src/api/analyticsApi.ts`
- `dts-copilot-webapp/src/components/copilot/signals/*`
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/AiCopilotResource.java`
- `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/web/rest/AiCopilotResourceTest.java`
- `worklog/v1.0.0/sprint-28-202605/assets/signals-contract.md`

## 验证

- [x] 后端 API 或 planner contract 有 curl / 单测证据。
- [x] `signals-contract.md` 写清采用链路和不采用链路。

## 完成标准

- [x] 数据源唯一。
- [x] 字段契约明确。
- [x] 无真实数据时的产品策略明确。
