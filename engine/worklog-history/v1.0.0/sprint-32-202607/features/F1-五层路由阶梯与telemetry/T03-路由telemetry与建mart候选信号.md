# F1/T03 路由 telemetry 与建 mart 候选信号

**状态**: DONE
**日期**: 2026-06-03

## 背景

类似 `public.xycyl_ads_*` 在 Trino 联邦入口报 `catalog.schema.table`、低库存预警命中资产但资产库不可见、Agent 提示去旧固定报表页这类问题，本质都不是单点 SQL 错误，而是路由弱路径和资产真实性之间缺少可观测闭环。

Sprint-32 F1/T02 已经让每次回答带上 `routeTrace`。T03 在此基础上把最终路由层级和用户问题写入 telemetry，后续可以统计哪些问题反复落到 Tier 4/5，从而决定优先建 ADS、资产或正式业务对象视图。

## 实现

- `RouteTelemetryService`
  - 写入 assistant message 的 `trace.telemetry`。
  - 记录 `question`、`finalTier`、`finalTarget`、`domain`、`responseKind`、`target`、`dataSurface`。
  - 将 `TIER_4_GUARDRAIL_FEDERATED` 与 `TIER_5_DIRECT_DETAIL` 标记为 `weakPath=true`。
  - 聚合输出 `tierCounts` 和 `martCandidateSignals`。
- `AgentChatService`
  - 同步与流式回答持久化时，都在 `CopilotChatContract.applyToMessage(...)` 后补充 telemetry。
- AI 接口
  - `GET /api/ai/agent/chat/route-telemetry`
  - `GET /internal/agent/chat/route-telemetry`
- Analytics 代理
  - `GET /api/copilot/chat/route-telemetry`
  - 通过内部 admin secret 代理到 AI 服务，前端/运维侧不直接接触内部密钥。

## 验证

```bash
bash worklog/v1.0.0/sprint-32-202607/it/test_f1_route_telemetry.sh
```

覆盖：

- telemetry 写入问题、最终 tier、最终 target 和 weakPath。
- Tier 分布和建 mart 候选信号聚合。
- `AgentChatService` 调用 telemetry 写入。
- AI internal route telemetry 接口。
- analytics `/api/copilot/chat/route-telemetry` 代理接口。
- 高可信预制模板命中时，模板 domain/target 覆盖泛化意图路由，避免“SQL 对、路由元数据错”再次把 Agent 带到弱路径。

## 后续使用方式

把 `martCandidateSignals` 中高频且同一业务对象/目标反复出现的问题，纳入下一轮 ADS 或资产沉淀。这样“报表是否有用”的判断从人工猜测变为真实提问频次和弱路径证据。
