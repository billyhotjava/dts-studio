# dts-platform 指标联邦接入 dts-copilot 设计

- 日期:2026-05-31
- 状态:设计已确认,待评审 → 进入 Sprint-28 实施
- 范围:dts-copilot(dts-copilot-ai 后端 + dts-copilot-webapp 前端);dts-platform **力争零代码改动**

---

## 1. 背景与目标

dts-stack(治理平台)的 dts-platform 已建有一套完整的**治理指标(Governance Indicator)子系统**:指标有权威口径定义,且已编译成 dbt mart 产数。诉求:把这些**治理过的权威指标**接进 dts-copilot 的 agent-first 单入口,让用户问数时优先拿到「口径权威」的答案,而不是每次都让 AI 现生成 SQL。

**核心定位**:从 dts-platform **取指标**,用 **copilot 自己的可视化组件渲染**(不是 iframe 嵌别人的成品报表)。延续「治理在 dts-stack、智能在 dts-copilot」的分工——口径单一源在平台,copilot 负责取数、智能路由、渲染、沉淀。

## 2. 核心决策记录

| # | 决策点 | 结论 |
|---|--------|------|
| D1 | 取数方式 | **服务联邦(A)**:copilot 实时调 dts-platform 指标 API 拿算好的聚合值,自己用 echarts 渲染;口径计算留平台,单一口径源 |
| D2 | 触发/呈现 | **C(copilot 目标态)**:指标优先路由 + 可召唤产物 + 资产库目录。agent 问数时优先匹配已发布指标 |
| D3 | 路由机制 | **A**:指标目录定期同步进 copilot 语义层,复用现有意图路由;优先级 `已发布指标 > 视图/mart > 现生成 SQL`;命中后取值实时调 API |
| D4 | 平台改动 | **力争零改码**:4 个端点 + DTO 字段均已就位;copilot 单方面适配平台现有 API/鉴权 |
| D5 | 跨服务认证 | **试用期:单受限只读机器账号**(简单快部署)。⚠️ 生产化/多租户前应切回**令牌透传**以恢复 Ranger 行列权限与指标密级控制——本期是显式权宜,非永久方案 |
| D6 | 健壮性 | 平台不可达/超时 → **显式降级 + 一键退回现生成 SQL**,绝不静默假装有数 |
| D7 | 落地节奏 | 独立 **Sprint-28**,分 P1(浏览渲染)→ P2(优先路由)→ P3(增强) |

## 3. dts-platform 既有能力(已核实,方案 A 的基础)

`GovernanceIndicatorResource`(`@RequestMapping("/api/governance")`):
- `GET /indicators`、`/indicators/by-domain`、`/indicators/by-template`、`/indicators/{id}` → 指标目录与定义(`IndicatorDto`)
- `GET /indicators/dashboard?domain=&days=` → `IndicatorDashboardService.getDashboard`(总览聚合)
- `GET /indicators/{id}/detail?days=` → 单指标趋势明细
- `GET /indicators/{id}/drilldown?dimension=&period=` → 下钻聚合
- 指标生命周期:`publish` / `versions` / `rollback`(可按 `status=已发布` 过滤)

`IndicatorDto` 字段(匹配/口径/渲染所需全有):`code/name/category/definition/expressionSql/status/version/tags/aggregationType/measureField/numeratorExpression/denominatorExpression/staticFilter/dynamicFilterConfig/dimensionFields/dateColumn/timeGrain/owner/dataLevel`。

底层 `DbtIndicatorGenerator` 把指标编译成 dbt mart 产数 → 数据与口径同源。

**结论**:取值端点与字段均已 REST 暴露,dts-platform 侧基本无需新建端点;唯一外部依赖是鉴权配置(机器账号只读角色)。

## 4. 架构总览与数据流(三个面)

```
┌──────────── dts-platform (治理/口径单一源,零改码) ────────────┐
│  GET /api/governance/indicators            → 指标目录/定义       │
│  GET /indicators/dashboard | {id}/detail | {id}/drilldown → 取值 │
└──────▲──────────────────────────────────────────▲───────────────┘
       │① 同步面(~1h + 手动,机器账号)             │③ 取值面(命中时·实时)
       │  拉 status=已发布 指标目录(定义/口径/同义/维度)
┌──────┼──────────────────────────────────────────┼───────────────┐
│ copilot                                          │                │
│  IndicatorCatalogSyncService → 缓存目录 → 语义层  │                │
│        │                                         │                │
│  用户问句 →[意图路由]② 优先级:已发布指标>视图/mart>生成SQL        │
│        命中 ┐                            └ 未命中 → 现生成 SQL(existing)│
│            ▼                                                      │
│  PlatformIndicatorClient 调 dashboard/detail/drilldown ──────────┘
│            ▼                                                       │
│  Indicator 产物(复用 sprint-27 F4 画布) → echarts/DataTable 渲染  │
│  口径芯片=指标 definition(F5) 溯源=指标X·口径vN(F6) 存为资产(F7) │
│  资产库「平台指标」分组(浏览/召唤)                                │
└───────────────────────────────────────────────────────────────────┘
```

- **① 同步面(定期)**:`IndicatorCatalogSyncService` 定时(~1h)+ 手动拉已发布指标目录,缓存进语义层供本地匹配(不为每 token 跨服务)。复用现有 glossary 刷新机制。记录每指标 `version`,口径变更可标记。
- **② 路由面(每次问数)**:复用现有意图路由(sprint-13 data-layer 路由 / sprint-22 semantic-pack / sprint-26 本体路由的接入点),新增「已发布指标」为最高优先目标。
- **③ 取值面(命中时·实时)**:`PlatformIndicatorClient` 调取值端点拿权威聚合值 → 包装 `Indicator` 产物 → copilot echarts 渲染。口径与数据均源于平台,天然一致。

## 5. 指标优先路由判定逻辑(C 的智能核心)

1. **匹配**:对同步来的目录(仅 `status=已发布`)用 `name/code/tags/definition/category/dimensionFields` + copilot 现有同义词字典(sprint-8 NV-07)算候选 + 置信度。匹配在**本地目录**做。
2. **优先级硬规则**:`已发布指标(高置信) > 视图/mart > 现生成 SQL`。
3. **与 sprint-27 乐观执行合流**:
   - 高置信单命中 → 调 API 取值 → `Indicator` 产物;命中指标名做成**可改假设芯片**「指标=XX ✎」(改错可切候选/退回生成 SQL)。
   - 多候选/中置信 → 乐观取最佳 + 芯片标「按指标X」,可一键切换。
   - 低置信/无命中 → 退回现生成 SQL,口径芯片标「现生成」。
4. **信任分层(溯源)**:走指标 → 溯源「dts-platform 指标X · 口径vN · expressionSql」;退回生成 → 「现生成 SQL」。用户一眼区分「权威指标」vs「AI 现算」。
5. **时间/维度映射**:问句时间词/维度 → `drilldown` 的 `dimension/period`;映射不上则降级(只出 dashboard 总览或退回生成),不报错。

## 6. 组件与边界

**copilot 后端(dts-copilot-ai,新增):**
- `IndicatorCatalogSyncService`:定时+手动同步已发布指标目录,缓存(内存 + 可选持久化扛重启)。
- `PlatformIndicatorClient`:调 `dashboard/detail/drilldown` 的 REST 客户端,机器账号鉴权 + 超时/降级。
- 路由集成:把指标目录注册为意图路由新目标,实现优先级判定;匹配复用同义词字典。
- 响应契约:命中时把 `IndicatorDto` 的 `definition/expressionSql/version` 填进 **sprint-27 F8** 的 `trace.metricCaliber` 与口径芯片字段,数据填进产物——零新增契约形状。

**copilot 前端(dts-copilot-webapp,新增):**
- `Indicator` 产物类型:扩展 sprint-27 F4 `Artifact`(`type:'indicator'`),渲染复用 echarts/DataTable;「下钻」动作调 `drilldown`。
- 资产库「平台指标」分组:浏览目录、点开召唤(挂进 sprint-27 F7 资产库)。
- 口径芯片/溯源:命中指标时 source 改为指标 `definition`/`expressionSql`,溯源标权威来源(复用 F5/F6)。

**dts-platform 侧:** 零改码;仅需配置一个**受限只读机器账号**(治理指标读角色)。

## 7. 跨服务集成与健壮性

- **认证(D5)**:试用期用单受限只读机器账号(Keycloak client-credentials 或配置 service user)。⚠️ 生产/多租户前切令牌透传恢复行列权限/密级。
- **目录同步**:~1h + 手动;分页拉 `status=已发布`;记录 `version`,口径变更标记「口径已更新」。
- **取值**:默认实时;可选 dashboard/detail 秒~分钟级微缓存保护平台。
- **降级(延续 sprint-27 F8「有则用无则降级」)**:
  - 同步失败 → 用上次缓存目录(stale-while-revalidate);从无缓存 → 指标路由临时禁用,全退回现生成 SQL,不阻断问数。
  - 取值失败(命中但超时/报错)→ 显式提示「平台指标服务暂不可达」+「改用 AI 现生成」一键退回;不静默。
- **可观测**:指标路由命中率、指标 API 延迟/失败率、降级次数埋点。

## 8. 与现有 Sprint 的关系

- **底座依赖 sprint-27**:复用 F4 画布、F5 口径芯片/乐观执行、F6 溯源、F8 契约。⚠️ sprint-27 这些底座的 live 缺口(IT04/07/08/09 乐观标 DONE)需先压实,否则指标产物会踩同样运行时坑。
- **与 sprint-26 本体路由是兄弟**:指标路由与本体对象路由都是权威口径源,未来可统一为单一语义路由层(本体对象 + 治理指标 + 视图/mart 同台判优先级)。本期路由接入点设计为此留位,不强求合并。

## 9. 分期(独立 Sprint-28,贴合「尽快简单上手」)

- **P1 取值+渲染(浏览,无路由)**:机器账号认证 + `IndicatorCatalogSync` + 资产库「平台指标」浏览 + 点开调 `dashboard/detail` → `Indicator` 产物 echarts 渲染。最快可演示闭环,不碰复杂路由。
- **P2 指标优先路由(C 核心)**:接入意图路由,命中→指标可改芯片 + 乐观执行 + 权威溯源,未命中退回现生成 SQL。
- **P3 增强**:`drilldown` 交互下钻、口径 version 变更提醒、多候选切换、取值微缓存、可观测埋点。

## 10. 范围边界与风险

- **YAGNI**:本期不做指标的创建/编辑(写回平台);copilot 只读消费已发布指标。
- **不做** iframe 嵌平台成品报表/大屏;不在 copilot 重算口径(口径恒取平台)。
- **风险**:
  - sprint-27 底座 live 缺口未压实会传导到本方案(高)。
  - 机器账号绕过行列权限/密级,试用期可接受,生产前必须切令牌透传(中,已标注)。
  - `IndicatorDto` 无显式同义词字段,匹配质量依赖 `name/tags/definition` + copilot 同义词字典(中)。
  - 同 Keycloak realm 与机器账号读角色需运维配置确认(中)。
  - 取值实时调用对 dts-platform 的负载(低,可加微缓存)。

## 11. 开放问题

- 机器账号的具体获取方式(Keycloak client vs service user)与读角色范围,需与 dts-stack 运维确认。
- `drilldown` 的 `dimension/period` 取值枚举,需对照 `IndicatorDto.dimensionFields/timeGrain` 在实施时映射。
