# F1 route telemetry 证据

**日期**: 2026-06-03
**环境**: local `/opt/prod/prs/source/dts-copilot`
**范围**: Sprint-32 F1/T03

## 根因背景

类似问题的共同根因是：Agent、资产库、固定报表和 screen JSON 的路由/执行口径没有统一可观测闭环。典型表现：

- Agent 生成 `public.xycyl_ads_*` SQL，但当前入口选择 Trino 联邦库，执行层要求 `catalog.schema.table`。
- Agent 命中 `WH-LOW-STOCK-ALERT` 等资产/模板，但资产库没有真实可见资产或仍保留旧“固定报表”话术。
- 用户问题落到业务对象明细或联邦查询后，没有沉淀为“该建 ADS/mart”的证据。

F1/T03 的处理方式是把每次回答的 routeTrace 聚合为 telemetry，标记 Tier 4/5 弱路径，后续用真实问题频次决定建模优先级。

## GREEN

重跑命令：

```bash
bash worklog/v1.0.0/sprint-32-202607/it/test_f1_route_telemetry.sh
```

结果：

- `RouteTelemetryServiceTest` 通过。
- `AgentChatServiceTest` 通过。
- `InternalAgentChatResourceTest` 通过。
- `CopilotChatResourceTest` 通过。
- Maven exit code: `0`。

## 运行时验证

重建并重启 `copilot-ai`、`copilot-analytics` 后：

```bash
curl -fsS http://127.0.0.1:50091/actuator/health
curl -fsS http://127.0.0.1:50092/actuator/health
```

结果均为 `{"status":"UP"}`。

使用本地临时 analytics session 调用：

```bash
curl -fsS 'http://127.0.0.1:50092/api/copilot/chat/route-telemetry?days=7&limit=5'
```

结果返回 `days=7`、`totalMessages=57`，`tierCounts` 当前为空。原因是历史消息没有 `trace.telemetry`，从本次版本之后的新回答开始累计。

## 覆盖点

| 覆盖点 | 说明 |
|--------|------|
| telemetry 写入 | assistant trace 增加 `telemetry.question/finalTier/finalTarget/weakPath` |
| 弱路径识别 | `TIER_4_GUARDRAIL_FEDERATED`、`TIER_5_DIRECT_DETAIL` 标记为弱路径 |
| 候选聚合 | 按 tier/domain/responseKind/target/dataSurface 聚合 `martCandidateSignals` |
| AI internal API | `GET /internal/agent/chat/route-telemetry` 受 admin secret 保护 |
| Analytics proxy | `GET /api/copilot/chat/route-telemetry` 使用登录态代理到 AI 服务 |

## 结论

F1/T03 已能把“类似问题”的弱路径暴露为可查询 telemetry。后续遇到低库存、销售情况、采购/库存明细等问题时，不只看单次 SQL 是否成功，而可以看它是否长期落在弱路径，从而决定补 ADS、资产或正式业务对象视图。
