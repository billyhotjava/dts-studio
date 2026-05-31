# F4: 活产物画布与产物托盘

**优先级**: P0
**状态**: DONE

## 目标

在工作台右侧建立「活产物画布」——钉住「当前在看的产物」(图 / 表 / 报表),追问时原地演化(同一画布换内容,而非新开窗);会话历史产物收进底部「产物托盘」,可点击切回。核心是**画布(产物渲染)与对话(消息流)解耦**:消息只持有产物 `id`,画布按 `id` 渲染当前产物,二者通过「产物引用」接口连接。本 Feature 同时提供画布顶部动作行的 UI 与事件契约,具体动作实现交 F6/F7。

> 设计依据:`docs/superpowers/specs/2026-05-30-agent-first-ui-design.md`(§4 状态二·右侧活产物画布、§6 结果卡与动作行、§8 模块边界原则「画布与对话解耦」、决策 D6「默认当前产物,不平铺」)。
> 范围内**只做画布与托盘的承载与渲染**:复用现有 `charts/` 与 `DataTable` 渲染产物;不实现存卡片/钉看板/导出/溯源的业务逻辑(T04 只出按钮 UI + 派发事件,实现归 F6 溯源 / F7 资产沉淀)。

## 依赖

- **F1**(应用骨架):AppLayout 双栏壳已留出右侧画布插槽。
- 与 **F3**(对话脊柱)并行:F3 负责消息流并在消息上携带产物引用;本 Feature 负责按引用渲染。二者通过 T01 定义的「产物引用」接口对齐,集成在 F5 落地。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 产物引用数据模型与接口 | P0 | DONE | F1 |
| T02 | 画布渲染容器(ArtifactCanvas) | P0 | DONE | T01 |
| T03 | 当前产物 + 产物托盘(ArtifactTray) | P0 | DONE | T01, T02 |
| T04 | 画布动作行(CanvasActions) | P0 | DONE | T02 |

## 完成标准

- [x] `src/types/artifact.ts` 定义 `Artifact`(含 `id` / `type: 'chart' | 'table' | 'report'` / `spec` / `sourceMessageId` 等),并导出「消息持有产物 id,画布按 id 渲染当前产物」的契约类型与产物存储 hook(`useArtifactStore`)(T01)。
- [x] `ArtifactCanvas` 能按 `Artifact.type` 正确渲染:`chart` 走 `charts/ChartRenderer`(echarts 封装),`table` 走 `DataTable`,缺产物 / 加载中 / 错误均有占位态(T02)。
- [x] 画布默认只显示「当前产物」;同一会话先后产生的产物收进底部「产物托盘」,点击托盘项可把任一历史产物切回画布(决策 D6,非平铺)(T03)。
- [x] 追问产生新产物时,画布原地切换为新产物,旧产物入托盘且不丢失(可切回)(T03 store + panel 验证)。
- [x] 画布顶部动作行渲染 [存为卡片] [钉到看板] [SQL·溯源] [导出] 四个按钮,点击各自派发统一事件(`onArtifactAction`),不在本 Feature 内实现具体动作(T04)。
- [x] 画布渲染与对话消息流解耦:`ArtifactCanvas` 仅依赖 `Artifact` 数据,不直接读取 `AiAgentChatMessage`;消息侧只持有 `artifactId`。
- [x] 新增组件有渲染/切换的单元测试;`pnpm typecheck`、`pnpm test`、`pnpm build` 全绿。
