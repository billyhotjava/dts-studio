# T02: OntologyService Action 编排 + adminapi 草稿调用

**优先级**: P1
**状态**: DONE
**依赖**: T01

## 目标

让 OntologyService 能编排 action：校验 params → 调 adminapi `saveDraft*` 落草稿。绝不调 commit、绝不直写业务库。

## 技术设计

- 新增 `OntologyActionExecutor`：解析对象属性填充 params → 调用 adminapi REST。
- 只允许调用 action.endpoint.draft；commit 端点不在 copilot 触发范围（由 adminweb 人工完成）。
- 调用失败（鉴权/业务校验拒绝）时把 adminapi 返回的错误透传给用户，不吞错。
- 幂等：同一对象重复发起草稿时提示已有草稿，避免重复单。

## 影响范围

- `OntologyService` 暴露 `getAction`。
- 新增 `OntologyActionExecutor`、`AdminApiActionClient`、`HttpAdminApiActionClient`。
- adminapi 调用走网关，base URL 由 `copilot.action.adminapi.base-url` 配置。

## 验证

- [x] 单测（mock adminapi）：params 正确组装，仅调 draft 端点。
- [x] commit 端点在任何路径下都不被 copilot 调用（用例断言 + 静态脚本）。
- [x] adminapi 返回错误时透传，不静默吞错。
- [x] IT 脚本：`worklog/v1.0.0/sprint-26-202605/it/test_action_executor_safety.sh`。
- [x] 证据：`worklog/v1.0.0/sprint-26-202605/it/evidence/20260530-local/action-safety.md`。

## 完成标准

- [x] Action 编排 + 草稿调用有单测，安全边界（只到草稿）被测试锁定。
