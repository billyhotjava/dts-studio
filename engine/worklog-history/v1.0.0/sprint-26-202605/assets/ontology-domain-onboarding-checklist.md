# 本体化域接入 checklist

> 适用范围：把一个已具备 dbt/固定报表基础的数据域接入 DTS Copilot 本体三层（Objects+Links / Metrics+Signals / Actions）。本清单来自 Sprint-26 报花域垂直切片，后续项目、采购、财务域按此执行。

## 0. 入口条件

- 已确认该域的权威数据面：优先 `public.ods_ptr_mysql_* -> stg -> dwd -> dws -> ads`，不使用临时业务库直连作为默认 NL2SQL 数据面。
- 已有至少一个 adminweb 对账面：固定报表、列表页或可复核 SQL/Mapper。
- 写回类场景必须先确认 adminapi 草稿端点与正式提交端点。Copilot 只能调用 `saveDraft*` 草稿端点，正式 `save*` 提交由 adminweb 人工完成。
- 写回联调前必须确认 `copilot.action.adminapi.base-url` 指向真实业务 adminapi gateway，不能默认指向 copilot proxy 或其它平台 admin；如果 adminapi 需要业务 Authorization，先准备 `copilot.action.adminapi.authorization`，否则端到端写回只能记录为阻塞。

## 1. semantic-pack 扩展

每个新域在语义包内补齐四类声明，权威源保持 Git 可审 JSON。

| 节 | 必填内容 | 验收方式 |
|---|---|---|
| `links` | 对象间软外键、`fromKey`/`toKey`、`cardinality`、`joinHint` | `OntologyService.buildJoinPlan` 单测能生成多跳 JOIN |
| `metrics` | 指标名、对象、表达式、单位、format、caliber | 与 dbt schema 注释和 adminweb 口径一致 |
| `signals` | severity、when 阈值、advice、linkedActions | `OntologyService.evaluateSignals` 单测覆盖命中与未命中 |
| `actions` | action name/object/intent、adminapi endpoint、params、approval、audit、guard | 静态校验只允许草稿调用，`postCommit` never |

## 2. Java 扩展点

优先复用现有运行时，只有域注册或分支词表确实缺失时才改代码。

- `SemanticPackService.PACK_FILES`：新增语义包文件时注册路径。
- `SemanticPackService.normalizeSemanticDomain`：补域名别名，例如项目/采购/财务中文词。
- `OntologyService`：通常不为新域改代码，保持 links/metrics/signals/actions 数据驱动。
- `AssetBackedPlannerPolicy`：新域需要补意图词、固定报表候选或 signal 触发词时，先写 planner RED 测试。
- `AgentBiReportCatalogService` / `BusinessObjectCatalogService` / `TemplateMatcherService`：只有固定报表、业务对象或模板目录需要新增入口时才扩展。
- 新写回动作必须走 `OntologyActionApprovalService`：先返回审批卡片，用户确认后执行 guard，再调用 `OntologyActionExecutor.createDraft` 并写审计。

## 3. 对账与证据

- adminweb 对账：每个域至少选择一个业务可识别报表或列表页，记录页面路径、adminapi Controller/Mapper 或 SQL、指标口径、误差阈值。
- DB 对账：金额类默认误差阈值 `<0.5%`；数量类默认要求一致，若历史数据缺口需写明 gap。
- IT 证据必须包含命令、输出摘要、数据源、日期环境和结论。不能只放空占位。
- 运行脚本应可独立重跑：`it/test_*.sh` 做静态和单元门禁，`RUN_DB=1` 或 `RUN_LIVE=1` 才访问运行态。

## 4. Golden Questions

每个域至少准备三类问句：

- 贯穿类：跨对象路径，例如客户 -> 项目 -> 合同 -> 结算。
- 预警类：命中 `signals`，输出风险对象、指标值、等级和建议动作。
- 动作类：只生成 action proposal / approval card，不直接写业务系统。

验收目标：

- 贯穿类命中率 >= 90%。
- 预警类优先走 `L2_ONTOLOGY_SIGNAL`，不退回固定报表目录。
- 动作类必须证明未确认不写回、guard 失败拒绝、确认后只到草稿。

## 5. Action 安全门

- action 必须声明 `approval=human`、`audit=true`、`guard=<domain>:<object>:draft`。
- adminapi endpoint 必须同时记录草稿和正式端点，但执行器只能调用 draft。
- `HttpAdminApiActionClient` 必须配置 `copilot.action.adminapi.base-url`；如需业务登录态，再使用 `copilot.action.adminapi.authorization` 配置 Authorization；不要把 token 写入 Git。
- 审计记录至少包含 who/session/action/object/guard/params/result/success/error。
- 真实端到端证据必须证明 adminweb 可见草稿，且 Copilot 没有调用正式提交端点。

## 6. 项目域纸面演练

Sprint-25 项目域按本清单接入时应这样落位：

| checklist 项 | 项目域候选 | 当前状态 |
|---|---|---|
| objects | 项目、客户、合同、摆位、绿植、摆位调整 | 源表清单已确认 |
| links | 客户 -> 项目 -> 合同 -> p_project_green -> p_position_adjustment | 等事实表入数后校验孤儿率 |
| metrics | 实摆组数、租金、成本、项目数、合同到期天数 | `p_project_green` 仍 0 行，金额口径待拍板 |
| signals | 合同到期预警、停用项目仍有实摆、摆位调整积压 | 依赖 `xycyl_dws_project_green_monthly` 和合同维度 |
| actions | 创建巡检/调整跟进草稿、合同续签提醒草稿 | 需先确认对应 adminapi 草稿端点 |
| adminweb 对账 | 项目实摆总览、项目/合同到期类固定报表 | 待锁定至少 2 个对账面 |
| Golden Questions | 项目实摆总览、客户下项目绿植、合同到期预警 | F1/F2 完成后落测试 |

项目域最小落地顺序：

1. 等 `p_project_green` / `p_position_adjustment*` 入数，完成状态分布、parent_id、金额字段画像。
2. 业务方把五个 P0 决策改为 `RESOLVED`。
3. 落共享维度：`xycyl_dim_project`、`xycyl_dim_customer`、`xycyl_dim_position`、`xycyl_dim_goods`、`xycyl_dim_contract`。
4. 落事实与汇总：`xycyl_dwd_project_green_snapshot`、`xycyl_dwd_position_adjustment`、`xycyl_dws_project_green_monthly`。
5. 补项目域 semantic-pack 的 links/metrics/signals/actions，先做只读和预警，再接 adminapi 草稿动作。

## 7. Done 门槛

- checklist 对应域的 `it/README.md` 有证据矩阵。
- schema/pack 加载、JOIN、signals、action guard/audit、Golden Questions 均有可重跑脚本。
- 所有 BLOCKED 项必须写明运行态证据、阻塞条件和解除后重跑命令。
