# Sprint-10 IT

本目录用于保存 `Sprint-10: 园林业务语义层与 NL2SQL 落地` 的集成测试计划和验收矩阵。

## 目标

验证以下链路在 `adminapi/adminweb + dts-copilot` 环境中可工作且受控：

- 预制模板匹配 → 参数化 SQL 执行
- 规则路由 → 业务域判定 → 视图选择
- NL2SQL → 基于视图 DDL 生成 SQL
- 权限桥接 → analytics 执行 + 白名单拦截

## 覆盖范围

- 6 个业务域路由：project / flowerbiz / green / settlement / task / curing / pendulum
- 7 个业务视图：v_project_overview / v_flower_biz_detail / v_project_green_current / v_monthly_settlement / v_task_progress / v_curing_coverage / v_pendulum_progress
- 20+ 预制查询模板
- 枚举词典翻译
- 权限控制：视图白名单、结算域角色限制

## 测试资产清单

- `BG-10-dual-channel-nl2sql-matrix.md` -- 验收矩阵（7维度 40+ 用例）
- `test-execution-plan.md` -- 分阶段测试执行计划
- `IntentRouterServiceTest.java` -- 路由引擎单元测试（dts-copilot-ai）
- `TemplateMatcherServiceTest.java` -- 模板匹配单元测试（dts-copilot-ai）
- `CopilotQueryServiceTest.java` -- 权限验证单元测试（dts-copilot-analytics）

## 执行命令

### Phase 1: 单元测试

```bash
# 路由引擎 + 模板匹配测试
cd dts-copilot-ai && mvn test -pl . -Dtest="IntentRouterServiceTest,TemplateMatcherServiceTest"

# 权限验证测试
cd dts-copilot-analytics && mvn test -pl . -Dtest="CopilotQueryServiceTest"
```

## 约束

- 不把全部历史业务场景都纳入 v1
- 优先覆盖高价值、可验证、可复现的问句
- 结算类问题只验证已结算结果，不验证租金计算逻辑
