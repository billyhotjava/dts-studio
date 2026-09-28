# ER-08: IT 验收与性能基线补齐

**优先级**: P2
**状态**: DONE
**依赖**: ER-03~ER-07

## 问题

`sprint-11/it` 目前只有说明文档，没有可执行测试，导致 ELT 结构、同步与路由问题没有被自动化发现。

## 范围

- `analytics` 模块单测/集测
- `sprint-13/it` 可执行脚本或测试矩阵
- 主题层与视图层的性能对比基线

## 建议最小覆盖

- 表存在与列对齐检查
- 同步执行一次并校验 watermark
- 趋势类问题命中主题层
- 主题层异常时自动回退
- 典型模板 SQL 的响应时间基线

## 验收标准

- [x] `it/` 目录存在可执行测试，而不是仅 README
- [x] 至少覆盖同步、路由、降级三条主链
- [x] 性能基线入口可复现

## 已落地产物

- [test_elt_guardrails.sh](/opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-13/it/test_elt_guardrails.sh)
- [README.md](/opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-13/it/README.md)

## 说明

当前可执行 guardrail 分两层：

1. Maven 回归：
   - `ConversationPlannerServiceTest`
   - `AssetBackedPlannerPolicyTest`
   - `AgentExecutionServiceTest`
   - `AgentChatServiceTest`
   - `InternalAgentChatResourceTest`
   - `IntentRouterServiceTest`
   - `FieldOperationSyncJobTest`
   - `ProjectFulfillmentSyncJobTest`
   - `EltSyncWatermarkMappingTest`
   - `EltWatermarkServiceTest`
   - `EltMonitorResourceTest`
2. 可选 live smoke：
   - 当本地 `analytics` 以 `dts.elt.enabled=true` 暴露 `/api/analytics/elt/*` 时，脚本会继续验证 status 和 trigger 路径
   - 当本地服务没有开启该能力时，脚本会明确输出 `SKIP`
