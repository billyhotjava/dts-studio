# Sprint-27: Agent-First 单入口前端重构 · P1 核心骨架(MVP)

**时间**: 2026-05
**状态**: READY
**目标**: 把 dts-copilot 前端从「BI 工具 + 浮动聊天插件」重构为「agent 工作台」——对外一个入口(对话脊柱 + 活产物画布),上线即可「一句话问数 → 出结果 → 一键沉淀为卡片/看板」。

## 背景

后台建模(本体、NL2SQL、报花域语义层)已就绪,但前端把「问数据」切成 6+ 个互不相通的入口(`/agent-bi`、`/analyze`、`/explore-sessions`、`/report-factory`、`/fixed-reports`、`/metric-lens`)加一个浮动 `CopilotSidebar`,导致 NL2SQL 名义可用、实际不好用。代码层面 `appShellConfig.ts` / `appNavigation.ts` 已开始收口(`HIDDEN_LEGACY_NAV_IDS` 已隐藏大部分旧导航,首页指向 `/agent-bi`),本 Sprint 完成「彻底重构」的第一阶段。

设计依据:`docs/superpowers/specs/2026-05-30-agent-first-ui-design.md`(brainstorm 产出,含 11 条核心决策 D1–D11)。

## 范围(P1 = 能力 ① 即席问数 + ② 报表/看板沉淀)

纳入:对话脊柱、活产物画布、冷启动首屏、乐观 NL2SQL + 口径芯片常驻、溯源面板、存卡片/钉看板、删旧路由彻底重构(保留 `/public/*`)。
不纳入(后续 Sprint):推荐追问引擎/本体地图(P2)、完整版主动洞察/action 桩(P3)。

## 落地分段

- **P1a 基础可上线**:先完成 F3-T00 测试基线、F1/F2/F3/F4,并保证「一句话问数 → 流式出结果 → 右侧画布展示」在后台未补高级字段时可降级运行。
- **P1b 契约增强**:完成 F8 后,F5/F6 的口径芯片、低置信反问、结构化溯源进入真实后台契约验收;F8 未完成前,这些能力只能以 mock contract / degraded runtime 作为证据。
- **P1c 资产沉淀**:在 F4/F5/F8 的产物与契约稳定后,接 F7 存卡片、钉看板、导出和资产库。

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 状态 |
|----|---------|---------|--------|------|
| F1 | 应用骨架与导航重构 | 5 | P0 | READY |
| F2 | 冷启动首屏 | 4 | P0 | READY |
| F3 | 对话脊柱(CopilotChat 拆分扶正) | 6 | P0 | READY |
| F4 | 活产物画布与产物托盘 | 4 | P0 | READY |
| F5 | 乐观 NL2SQL 回答(口径芯片常驻) | 4 | P0 | READY |
| F6 | 溯源信任面板 | 3 | P1 | READY |
| F7 | 资产沉淀(存卡片/钉看板/资产库) | 4 | P1 | READY |
| F8 | 后台契约与降级联调 | 4 | P0 | READY |

## 依赖顺序

```
F3-T00 (现状测试基线)
   ├─> F1 (骨架) ──┬─> F3 (对话脊柱) ──┬─> F5 (乐观回答) ──> F6 (溯源)
   │               ├─> F4 (画布) ───────^
   │               └─> F2 (首屏)
   └─> F8 (后台契约) ───────────────────┴──────────────┘

F7 (资产沉淀) 依赖 F4 + F5 的产物契约稳定后实施。
```
F3-T00 是删除旧入口和拆分 `CopilotChat` 前的回归闸门;F5/F6 的完整验收依赖 F8,但 UI 可在 F8 未完成时按降级原则上线 P1a。

## 后台契约依赖(F8 负责,跨 F5/F6)

子代理核对 `dts-copilot-webapp/src/api/types.ts` 后确认:当前 `aiAgentChatSend` / `AiAgentChatMessage` / `CopilotStreamEvent.done` **不返回** 以下字段,前端需后台配合扩展(`dts-copilot-ai` / `adminapi`):

- **F5 乐观执行**:`assumptions[]`(口径假设)、`confidence`(置信度,前端阈值常量 `OPTIMISTIC_CONFIDENCE_THRESHOLD=0.6`)、`clarifications[]`(低置信澄清项);入参缺 `assumptionOverrides` / `clarificationAnswers`。
- **F6 溯源**:结构化 `trace{ metricCaliber(口径名+版本), sources[](表/字段), sql }`(现仅有 `generatedSql`/`routedDomain`/`dataSurface`/语义不固定的 `sourceRefs`)。可参照已有 `ExplainabilityResponse.explainCard`(metricDefinition/dataLineage/trace)的结构设计。
- **F6 纠正回流**:`submitChatFeedback` 已含 `correctedSql`(POST `/api/ai/nl2sql/feedback`),本期复用;`submitCaliberCorrection` 接口桩签名对齐 sprint-26 adminapi 写回,P2 实现真正回流评测集/本体草稿。

**容错原则**:前端一律「有则用、无则降级不报错」,后台补字段前不阻塞 P1a 基础 UI 上线。IT05/IT06/IT09 只有在 F8 live contract 通过后才能标为完整通过;F8 未完成时只能记录 mock contract 或 degraded runtime 证据。

## 完成标准

- [ ] 进入应用第一眼是 agent 工作台(冷启动首屏),不再是旧的 6 菜单
- [ ] 旧业务路由已删除,`/public/*` 三类分享链接仍可访问
- [ ] 一句话(文字或语音)问数能流式出结果;F8 字段存在时口径芯片常驻可改,字段缺失时降级不报错
- [ ] 结果可一键「存为卡片」「钉到看板」,资产库二级入口可浏览
- [ ] 点 SQL·溯源 能看到命中口径/表/字段/SQL;F8 trace 缺失时回退到 `generatedSql` / `sourceRefs` / tool messages
- [ ] `CopilotChat` 拆分后单文件 < 800 行,核心交互有测试护栏
- [ ] `it/README.md` 有真实集成验证证据(非空占位)
