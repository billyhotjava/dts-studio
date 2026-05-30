# F5: 乐观 NL2SQL 回答(口径芯片常驻)

**优先级**: P0
**状态**: READY

## 目标

赋予 agent「乐观执行」的性格(决策 D4):面对一句自然语言问数,基于本体置信度**直接出结果**,把推断出的关键假设(本月=2026-05、利润=收入−成本、范围=在租项目)做成**常驻、可点改的口径芯片**钉在结果卡顶部(决策 D8,方便新手核对);仅当置信度低到大概率猜错时,才**降级为反问**(澄清芯片单选)先问再执行。点击口径芯片改假设 → 以新参数重新发起查询 → F4 活产物画布对应产物**原地刷新**(复用产物 id,不新增产物)。

> 设计依据:`docs/superpowers/specs/2026-05-30-agent-first-ui-design.md`
> - **D4 歧义处理性格**:乐观执行 + 可改假设芯片(风格 A)——按本体置信度直接出结果,把假设做成可点改芯片;仅当置信度低到大概率猜错时降级为反问。
> - **D8 溯源/口径呈现**:关键口径(假设芯片)**常驻结果卡**(方便新手);完整 SQL / 溯源面板按需展开。
> - **§6 对话完整回路**:🎙 提问 → 🧠 本体匹配口径 → ⚡ 乐观出结果 + 假设芯片 → 🔍 溯源可展开 → ↘ 推荐追问 → 📌 存为资产。
> - **§8 模块边界原则**:画布与对话解耦,消息持有产物 id,画布按 id 渲染当前产物。

## 现状与衔接点(已核对源码)

- **发送链路**:`src/components/copilot/CopilotChat.tsx` 的 `handleSendText` 构造 `body: CopilotSendBody`(`type CopilotSendBody = Parameters<typeof analyticsApi.aiAgentChatSend>[0]`),优先走 `aiAgentChatSendStream`(SSE),失败回退 `analyticsApi.aiAgentChatSend`(同步)。SSE `done` 事件在 `src/api/modules/copilot.ts`(约第 275–290 行)解析,落到消息字段。
- **消息字段现状**(`src/api/types.ts` `AiAgentChatMessage`,第 245–271 行):已有 `responseKind` / `generatedSql` / `routedDomain` / `targetView` / `templateCode` / `suggestedDisplay` / `dataSurface` / `qualityLevel` / `qualityNotes` / `reportCode` / `sourceRefs`;**没有** `assumptions[]`、`confidence`、`clarifications[]` 字段。
- **流事件现状**(`src/api/types.ts` `CopilotStreamEvent`,第 1388–1408 行):`done` 事件透传上述字段;**没有** assumptions / confidence / clarifications。
- **入参现状**(`src/api/modules/copilot.ts` `copilotApi.aiAgentChatSend`,第 171–190 行):`{ userMessage, sessionId?, datasourceId?, schemaName?, objectContext?, pageContext? }`;**没有**承载「用户改后假设」的字段。
- **结论(关键风险)**:乐观执行需要的 `assumptions[]`(可改口径)、`confidence`(置信度)、低置信时的 `clarifications[]`(澄清选项)三类数据,**后台 SSE 与同步接口当前均未返回**;改后重算所需的 `assumptionOverrides` 入参也**尚未支持**。这些契约已单列 **F8 后台契约与降级联调**。F8 未完成前,F5 只能以 mock contract / degraded runtime 验收;真实链路完整通过必须依赖 F8 live contract。
- **结果卡承载**:口径芯片挂在每条 assistant 消息的结果卡顶部,即 `CopilotChat.tsx` 渲染 `InlineSqlPreview` 之前/之上的位置(约第 1108–1145 行,`generatedReportNotice` 与 `InlineSqlPreview` 区域)。
- **画布刷新契约**:F4 `src/types/artifact.ts` 约定 `Artifact { id, type, spec, sourceMessageId }` + `useArtifactStore`;F5 改后重算须**复用同一 `artifactId`** 更新 `spec`,使画布原地刷新而非新增产物(对齐 D6「默认当前产物,不平铺」)。

## 复用与依赖

- 复用:`aiAgentChatSendStream` / `analyticsApi.aiAgentChatSend`(发送)、`InlineSqlPreview`(结果预览 + 重新执行)、`copilotReasoningState`(流式状态)、F4 `useArtifactStore` / `Artifact`(画布产物刷新)、`CopilotChat.helpers`(消息排序/取用户问题)。
- 依赖:**F3**(对话脊柱:消息流 + 发送/流式接入已扶正)、**F4**(活产物画布:产物引用接口 + `useArtifactStore` 已就绪)、**F8**(真实后台契约;未就绪时只做降级和 mock 验收)。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 假设/口径芯片组件(常驻) | P0 | READY | F3, F4 |
| T02 | 乐观执行流(高置信直接出结果 + 假设芯片) | P0 | READY | T01, F3, F8-T01 |
| T03 | 低置信降级反问(澄清芯片单选) | P0 | READY | T02, F8-T01, F8-T02 |
| T04 | 芯片可改 → 重算 → 画布原地刷新 | P0 | READY | T01, T02, F4, F8-T02 |

## 完成标准

- [ ] 高置信问数:一句话直接流式出结果;F8 live contract 存在时,结果卡顶部常驻可读的口径芯片(如「本月=2026-05 ✎」「利润=收入−成本 ✎」「范围=在租项目 ✎」),后台未返回时按容错降级且不报错(T01、T02)。
- [ ] 低置信问数:F8 live contract 返回 `clarifications[]` 时,渲染澄清芯片并带选择继续执行;后台未返回时回退乐观执行或无芯片降级,不可假标真实通过(T03)。
- [ ] 点击口径芯片进入编辑态 → 提交新假设 → 以 `assumptionOverrides` 重新发起查询 → F4 画布对应产物通过同 `artifactId` `upsert` 原地刷新(不新增产物、旧产物不被复制进托盘)(T04)。
- [ ] 接口契约扩展点已在 Task 内写明:`AiAgentChatMessage` / `CopilotStreamEvent.done` 增 `assumptions[]`、`confidence`、`clarifications[]`;`CopilotSendBody` 增 `assumptionOverrides` / `clarificationAnswers`;前端类型先行,后台补字段前有容错。
- [ ] 新增组件/纯函数有 vitest 覆盖(芯片渲染与编辑、置信度阈值判定、override 入参构造、画布 id 复用);`pnpm typecheck`、`pnpm test`、`pnpm build` 全绿。
