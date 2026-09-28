# 报花域本体化设计参考（Palantir Ontology 映射）

> 本文是 Sprint-26 的设计锚点，来自 brainstorming 阶段的四个关键决策，供各 Task 实施时对照。

## Palantir 概念 → 本项目现状

| Palantir 概念 | 含义 | 现状 | Sprint-26 动作 |
|---|---|---|---|
| Data Integration | 语义化数据基础 | ✅ dbt 五层 mart | 复用 |
| Ontology · Objects+Properties | 业务对象+属性 | 🟡 semantic-pack objects/synonyms（扁平视图） | 复用 + 加 metrics |
| Ontology · Links | 对象关系图 | ❌ 缺失 | **F1** 补 links |
| Ontology · Kinetics | 派生指标/规则/预警 | ❌ 缺失 | **F2** 补 metrics/signals |
| Ontology · Actions | 带校验/审计的写回 | ❌ 缺失 | **F3** 补 actions |
| AIP | Agent 在本体上推理+行动 | 🟡 只读 NL2SQL | F1-F3 升级 planner |

## 四个关键决策

1. **三层全要，垂直切片先行**——报花单域先走完 Tier1→2→3，再横向复制。
2. **本体在 semantic-pack 内生长 + 薄运行时 OntologyService**——不新建子系统，权威源是 Git 可审 JSON。
3. **写回 = 调 adminapi REST + adminweb 人工审批**——绝不直写业务库。
4. **利好**：adminapi 已有 `saveDraft*`/`save*` 双端点，审批门禁复用 adminweb 现成 UI。

## semantic-pack 四节扩展骨架

```jsonc
"links":   [{ "name","from","to","fromKey","toKey","cardinality","joinHint?","note?" }],
"metrics": [{ "name","object","expr","unit?","format?","caliber" }],
"signals": [{ "name","object","severity","when","advice","linkedActions?" }],
"actions": [{ "name","object","intent","endpoint":{"service","draft","commit"},
             "params":[{"name","from","required"}],"approval","audit","guard" }]
```

## Tier3 写回闭环（安全姿态）

```
copilot 建议动作卡片 → 用户确认 → guard 鉴权 → 调 saveDraftFlowerBadDebt（草稿）
→ 回卡片"已生成草稿#xxx" + 写审计 → 业务员在 adminweb 复核 → 点 saveFlowerBadDebt（提交）
```

dts-copilot 永远只到"草稿"，正式提交必经 adminapi 业务规则 + adminweb 人工确认——零绕过、全审计、人在环路。

## 核验过的落地锚点

- `SemanticPackService.PACK_FILES`（4 个 pack，数组追加式）。
- `AssetBackedPlannerPolicy.plan(userQuestion, martHealthSnapshot)`（决策树入口，已注入多个 catalog 服务）。
- adminapi `FlowerBizInfoBadDebtController` → `/flower/bizBadDebt` → `saveDraftFlowerBadDebt` / `saveFlowerBadDebt`。
- 换花 `FlowerChangeController /biz/change saveDraftChangeFlower`、撤花 `/biz/back bizBack`、调花 `FlowerTransferController`。
