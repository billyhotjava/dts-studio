# Sprint-26: 报花域本体化垂直切片（Ontology Vertical Slice）

**时间**: 2026-05
**前缀**: ON (Ontology)
**状态**: IN_PROGRESS
**目标**: 在报花域已建好的 dbt mart 之上，落地 Palantir 式本体三层（Links / Metrics+Signals / Actions），把 dts-copilot 从"只读报表"升级为"对象图导航 + 风险预警 + 一键写回 adminapi"的业务指导闭环。

## 背景

报花域是当前唯一已跑通 `ods_ptr_mysql_* -> stg -> dwd -> dws -> ads` 五层、且有完整 semantic-pack（`flowerbiz.json`）的主线。客户的诉求已经从"要报表"上移到"要业务指导"——不只是"本月坏账多少"，而是"哪些项目即将坏账、建议怎么处理、能不能一键发起处理"。

对照 Palantir Foundry：
- **Data Integration**（语义化数据基础）已完成 = dbt 五层 mart。
- **Ontology · Objects+Properties** 半成品 = `semantic-pack` 的 `objects`/`synonyms`（但对象 = 一张扁平视图）。
- **Ontology · Links / Actions** 缺失 = 对象彼此孤立、纯只读、无写回。
- **AIP** 只读版 = Agent 在视图上做 NL2SQL，不在对象图上导航，更不能触发动作。

Sprint-26 用**垂直切片**策略：只在报花域把本体三层一次走通，做出可演示的"问答→预警→一键处理坏账草稿"闭环，验证本体论范式跑得通后，再复制到项目/采购/财务（Sprint-25 已先行铺设的共享维度是其底座）。

## 关键设计决策（来自 brainstorming）

1. **三层全要，垂直切片先行**：Tier1→2→3 层间顺序被依赖锁死；报花域先单域走完三层，再横向复制。
2. **本体在 semantic-pack 内生长 + 薄运行时**：扩展现有 pack schema（追加 `links`/`metrics`/`signals`/`actions` 四节），新增一个薄 `OntologyService` 加载并解析对象图/求值预警/编排动作。**不新建子系统**，权威源仍是 Git 可审的 JSON。
3. **写回 = 调 adminapi 现有 REST + 审批队列**：Action 绝不直写业务库。利好发现——adminapi `FlowerBizInfoBadDebtController` 已同时提供 `saveDraftFlowerBadDebt`（草稿）与 `saveFlowerBadDebt`（提交），换花/调花/撤花同样是"草稿+正式"双端点。闭环即：Action 调 `saveDraft*` 落草稿 → 现有 adminweb 人工复核 → 调 `save*` 提交。审批门禁复用 adminweb 现成 UI，零新增审批系统。

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 状态 | 说明 |
|----|---------|---------|--------|------|------|
| F0 | 本体运行时骨架与 schema 扩展 | 3 | P0 | DONE | T01/T02/T03 均有可重跑测试证据 |
| F1 | Tier1 对象图与导航 | 4 | P0 | DONE | Golden Questions 4/4 命中对象图导航 |
| F2 | Tier2 指标与预警 | 4 | P1 | DONE | T01/T02/T03/T04 均有可重跑测试证据 |
| F3 | Tier3 写回 Action 闭环 | 4 | P1 | BLOCKED | T01/T02/T03 完成；T04 阻塞在正确 PRS adminapi gateway + 业务 Authorization |
| F4 | 本体范式固化与验收 | 2 | P2 | IN_PROGRESS | T01 checklist 完成；T02 受 F3/T04 运行态入口阻塞影响 |

## 本 sprint 不做

- 不新建独立本体元模型表/图数据库；权威源保持 semantic-pack JSON。
- 不直写 `rs_cloud_flower` 业务库；所有写回必经 adminapi REST + adminweb 人工确认。
- 不把对象图导航/预警/Action 扩展到项目、采购、财务域；本 sprint 只做报花单域垂直切片。
- 不修改 adminapi/adminweb 既有业务逻辑（只调用其现有端点）；如发现端点缺口，记录为接口需求，不在本 sprint 内改业务运行时。
- 不在没有真实回归证据前把任一 Feature 标 DONE。

## 完成标准

- [x] semantic-pack schema 扩展四节（links/metrics/signals/actions）有 schema 校验，且对未声明这些节的旧 pack 完全向后兼容。
- [x] `OntologyService` 运行时加载有单测，不只检查文件存在。
- [x] flowerbiz 对象图打通"客户→项目→报花→采购→结算"软外键链路，贯穿类 Golden Questions 命中 ≥90%。
- [x] metrics 口径集中定义，与 dbt 4 列金额标准一致；signals 命中结果与 adminweb 固定报表对账误差 <0.5%。
- [ ] "一键创建坏账处理单草稿"端到端跑通：guard 鉴权 → 调 `saveDraftFlowerBadDebt` → 回审批卡片 → 写审计日志；正式提交仍由 adminweb 人工完成。
- [x] 产出一份《本体化域接入 checklist》，可被项目/采购/财务域直接复用。
- [ ] IT 证据包含：pack schema 兼容性测试、运行时加载测试、对象图导航回归、预警对账、Action 端到端审计链路。

## 与相邻 sprint 的关系

- **依赖** Sprint-22（报花域 dbt mart + flowerbiz.json 语义资产基线，已 DONE）。
- **复用** Sprint-25 共享维度（dim_customer/project/contract...）——对象图 Links 直接引用这些维度，但本 sprint 不等 Sprint-25，因为报花域自身的 mart 已足够走通垂直切片。
- **铺路** 后续把本体三层范式复制到项目（Sprint-25 数据面就绪后）、采购、财务域；F4 的接入 checklist 即交付物。
