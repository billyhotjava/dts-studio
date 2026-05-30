# T02: 口径/表/字段/SQL 展示

**优先级**: P1
**状态**: READY
**依赖**: T01（溯源面板形态就绪）、F8-T01/F8-T03（trace 响应与纠正契约）

## 目标

在 T01 的溯源面板内，分区展示三类溯源信息，让 NL2SQL 结果可信（spec §6、D8）：

1. **命中口径**：本体口径名 + 口径版本，例「利润＝收入−成本 · 报花域 · 口径 v3」；
2. **数据来源**：用到的表与字段；
3. **生成 SQL**：只读展示，可复制，默认收起、按需展开。

消费 `aiAgentChatSend`（及 `getAiAgentSession` / SSE `done` 事件）返回的 `trace` 结构。**当前后台契约尚不足**，契约落地归 F8;本 Task 实现前端消费与降级兜底。

## 技术设计

### 后台契约现状（已核对源码）

`src/api/types.ts`：
- `AiAgentChatMessage`（L245-271）已有：`generatedSql`、`routedDomain`、`targetView`、`templateCode`、`dataSurface`、`qualityLevel`、`qualityNotes`、`reportCode`、`sourceRefs?: string[] | string`，以及工具调用 `toolName/toolParams/toolResult`。
- **没有**结构化的口径（metric caliber + 版本）与表/字段（sources）字段；`sourceRefs` 仅为字符串数组，语义不固定。
- `CopilotStreamEvent`（`done`，L1394-1407）透传上述同名字段 + `sourceRefs`，同样无结构化口径/来源。
- `ExplainabilityResponse.explainCard`（L341-355）已有 `metricDefinition` / `dataLineage` / `trace`，是口径/血缘的既有形态，可作为 `trace` 结构设计参照。

**结论**：需后台在聊天链路补结构化 `trace`，由 F8-T01/T03 负责。在契约落地前，前端用现有字段降级。

### 契约扩展需求（F8-T01/T03）

在 `aiAgentChatSend` 返回的 assistant 消息、`getAiAgentSession` 消息、SSE `done` 事件上新增可选 `trace`：

```ts
type CopilotTrace = {
  metricCaliber?: {
    name: string;        // 口径名，如「利润」
    formula?: string;    // 口径公式，如「收入−成本」
    domain?: string;     // 本体域，如「报花域」
    version?: string;    // 口径版本，如「v3」
    ontologyRef?: string;// 本体节点引用（供 T03 纠正/跳转本体地图）
  };
  sources?: Array<{
    table: string;
    fields?: string[];
    role?: string;       // 如 fact / dim
  }>;
  sql?: string;          // 生成 SQL（与消息 generatedSql 等价或更精确）
};
```

落点：`AiAgentChatMessage.trace?: CopilotTrace`、`CopilotStreamEvent`（`done`）`trace?: CopilotTrace`，并在 `copilot.ts` 的 `aiAgentChatSendStream` `done` 分支与归一化函数 `normalizeLegacyAiChat*` 中透传。

### 前端实现

1. **类型**：在 `src/api/types.ts` 增 `CopilotTrace`，挂到 `AiAgentChatMessage.trace?` 与 `CopilotStreamEvent` `done`。
2. **透传**：`copilot.ts` `aiAgentChatSendStream` `done` 分支补 `...(parsed.trace ? { trace: parsed.trace } : {})`；`CopilotChat.tsx` 把 `event.trace` / `msg.trace` 落到消息状态（参照现有 `sourceRefs` 透传 L470/L646/L1020）。
3. **展示组件**：在 `TracePanel.tsx` 面板内新增三个分区（建议拆出纯展示子组件 `TraceCaliberSection` / `TraceSourcesSection` / `TraceSqlSection`，保持单文件 < 800 行、组件 < 50 行）：
   - **口径分区**：渲染 `「{name}＝{formula} · {domain} · 口径 {version}」` 芯片样式；缺失字段优雅省略。
   - **来源分区**：表 → 字段列表（表名为主、字段为次，体现层级）。
   - **SQL 分区**：只读 SQL + 复制按钮，默认收起（D8「完整 SQL 按需展开」）。SQL 只读视图与 `InlineSqlPreview` 的代码块复用同一呈现（评估抽取共享只读 `SqlCodeBlock`，避免重复样式）。
4. **降级兜底**（`trace` 缺失时）：
   - 口径：用 `routedDomain` / `qualityLevel` / `reportCode` 拼出近似口径信息，并提示「口径结构化待后台补充」。
   - 来源：解析 `sourceRefs`（`string[] | string`，先 `normalizeSourceRefs` 归一）渲染为来源列表。
   - SQL：回退到消息 `generatedSql`（即结果卡 `extractedSql`）。
   - 保留 T01 既有「工具调用逐步展开」分区作为最底层兜底。
5. **常驻芯片边界**：关键口径芯片**常驻结果卡由 F5 负责**（D8）；本面板展示的是完整口径 + 版本 + 来源 + SQL，不重复实现常驻芯片，但二者口径名/版本取值需一致（同一 `trace.metricCaliber` 源）。

## 影响范围

- `src/api/types.ts`：新增 `CopilotTrace`；`AiAgentChatMessage.trace?`、`CopilotStreamEvent`（done）`trace?`。
- `src/api/modules/copilot.ts`：`aiAgentChatSendStream` `done` 透传 `trace`；归一化函数透传（如 `aiChatCompatibility.ts` 的 `normalizeLegacyAiChat*`，需同步核查）。
- `src/components/copilot/TracePanel.tsx`：新增口径/来源/SQL 三分区（含纯展示子组件）+ `sourceRefs` 归一/降级逻辑。
- `src/components/copilot/TracePanel.css`：口径芯片、来源层级、SQL 收起/展开样式。
- `src/components/copilot/InlineSqlPreview.tsx` / `inlineSqlPreviewPresentation.ts`：若抽取共享只读 `SqlCodeBlock`，相应引用调整（不改变结果卡现有行为）。
- `src/components/copilot/CopilotChat.tsx`：透传 `trace` 到消息状态与 `TracePanel`。
- **跨团队**：NL2SQL / copilot 后台需补 `trace{ metricCaliber, sources[], sql }`（由 F8 跟踪，前端有降级，不阻塞 P1a 基础上线）。

## 验证

- [ ] 单测：给 `TracePanel` 传含 `trace.metricCaliber` 的消息，断言渲染「利润＝收入−成本 · 报花域 · 口径 v3」。
- [ ] 单测：传 `trace.sources` 渲染表/字段层级；传 `trace.sql` 渲染只读 SQL + 复制。
- [ ] 降级单测：`trace` 缺失、仅有 `sourceRefs` + `generatedSql` 时，来源与 SQL 仍可展示，且出现「口径结构化待后台补充」提示。
- [ ] 真实后台未返回 `trace` 时，IT09 记录为 degraded runtime；只有 F8 live contract 返回 `trace` 后才标完整通过。
- [ ] SQL 分区默认收起，点击展开（符合 D8 按需展开）。
- [ ] `pnpm typecheck` / `pnpm test` / `pnpm build` 全绿。

## 完成标准

- [ ] 溯源面板分区展示命中口径（本体口径名 + 版本）、来源表/字段、生成 SQL。
- [ ] 消费 `aiAgentChatSend` / SSE `done` 的 `trace` 结构；契约扩展需求 `trace{ metricCaliber, sources[], sql }` 已写入本 Task 并落到 `types.ts` 类型。
- [ ] 后台未返回 `trace` 时有完整降级（`sourceRefs` + `generatedSql` + 工具调用兜底）且不报错。
- [ ] 完整 SQL/溯源默认收起、按需展开（D8）；口径取值与 F5 常驻芯片同源一致。
- [ ] 三脚本全绿，含新增单测。
