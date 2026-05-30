# T01: flowerbiz.json 补 actions 定义（映射 adminapi 端点）

**优先级**: P1
**状态**: DONE
**依赖**: F1/T01

## 目标

在 `actions` 节定义报花域写回动作，映射到 adminapi 已有的"草稿+正式"双端点。

## 技术设计

利好：adminapi 已提供双端点（核验自 `FlowerBizInfoBadDebtController` 等）：

| action | object | draft 端点 | commit 端点 |
|--------|--------|-----------|-------------|
| 创建坏账处理单 | 报花明细 | `POST /flower/bizBadDebt/saveDraftFlowerBadDebt` | `POST /flower/bizBadDebt/saveFlowerBadDebt` |
| 发起换花 | 报花明细 | `POST /biz/change/saveDraftChangeFlower` | （对应正式端点） |
| 发起撤花 | 报花明细 | `/biz/back/...` | - |

每个 action 声明：`params`（含 from=对象属性、required）、`approval: human`、`audit: true`、`guard`（权限要求）。

本 Task 优先只定义"创建坏账处理单"一个动作走通端到端，其余作为模板列出。当前已完成坏账草稿 action 定义；执行层仍需 T02/T03 接管 guard、草稿调用和审计。

## 影响范围

- `flowerbiz.json` 的 `actions` 节。
- `租赁报花明细` 补充 `报花单id`、`项目id`、`明细id`，用于 action params 溯源。

## 验证

- [x] action.params 引用的对象属性在 objects/links 中可解析。
- [x] endpoint 路径与 adminapi 实际 Controller 一致（已核验坏账端点）。
- [x] IT 脚本：`worklog/v1.0.0/sprint-26-202605/it/test_flowerbiz_actions.sh`。
- [x] 证据：`worklog/v1.0.0/sprint-26-202605/it/evidence/20260530-local/flowerbiz-actions.md`。

## 完成标准

- [x] "创建坏账处理单"action 定义完整，含 draft/commit/params/guard/approval/audit。
