# T01: TracePanel 扶正为画布溯源面板

**优先级**: P1
**状态**: READY
**依赖**: F4 活产物画布（`CanvasActions` 动作行 + 「`</>` SQL·溯源」按钮）

## 目标

把当前长在消息体内、靠 `trace-toggle` 折叠按钮触发的 `TracePanel`（推理过程折叠块）扶正为**画布级溯源面板**：由 F4 画布动作行的「`</>` SQL·溯源」按钮触发，以抽屉/侧栏形态打开，绑定到「当前在看的产物」对应的消息。本 Task 只做**形态迁移与事件对接**，面板内容（口径/表/字段/SQL）在 T02、纠正入口在 T03。

## 技术设计

### 现状

- `src/components/copilot/CopilotChat.tsx` L1146-1168：`hasTrace` 时渲染 `.trace-toggle` 按钮 + `expandedTraces` Set 控制 `traceExpanded`，展开后内嵌 `<TracePanel toolMessages={toolMsgs} />`。
- `src/components/copilot/TracePanel.tsx`：`Props { toolMessages: AiAgentChatMessage[] }`，逐步展开工具调用的 `toolParams` / `toolResult` JSON。当前为「消息内嵌折叠块」形态。
- `TracePanel.css`：`.trace-panel`（`margin-top:4px` 内嵌样式）+ `.trace-toggle`（折叠触发器样式，当前由 CopilotChat 使用）。

### 改造

1. **新增面板宿主形态**：在 `TracePanel.tsx` 增加抽屉/侧栏容器（受控开关），新增 Props：
   ```ts
   interface Props {
     open: boolean;                 // 由画布动作行控制
     onClose: () => void;
     toolMessages: AiAgentChatMessage[];
     // T02 补充：trace / metricCaliber / sources / sql / sourceRefs / generatedSql
   }
   ```
   - 形态：右侧抽屉（overlay 抽屉，遮罩点击与 `Esc` 关闭，焦点陷阱 + `role="dialog"` `aria-modal`）。优先复用项目内既有抽屉/Drawer 基元（先在 `src/ui/` 下搜索；无则在本组件内自带轻量抽屉，动画仅用 `transform` / `opacity`，遵循 web 性能规则）。
   - 保留现有「逐步工具调用展开」内容区作为面板的「溯源/推理」分区，T02 在其上新增口径/表/SQL 分区。
2. **触发源迁移到画布动作行**：
   - F4 的 `CanvasActions`（活产物画布顶部动作行，spec §3 右侧画布：`[存为卡片][钉到看板][</> SQL·溯源][导出]`）统一通过 `onAction({ action: "trace-sql", artifact })` 派发，不再额外定义 `onOpenTrace(messageId)`。
   - 由画布容器（F4/F6 接线层）持有 `traceOpen` 状态与「当前在看产物」对应的 `artifact.sourceMessageId`,再回找对应 `message` / `toolMessages` / `trace` 传给 `TracePanel`。
   - **契约约定**：F4 只负责派发统一 `CanvasActionEvent`;F6 消费 `action === "trace-sql"` 并用 `artifact.sourceMessageId` 绑定消息。若 F4 尚未落地动作行，T01 先在 `CopilotChat` 内用临时按钮触发面板，并标注 `// TODO(F4): 由 CanvasActions trace-sql 事件接管触发`。
3. **下线/降级旧入口**：消息内 `.trace-toggle` 折叠块不再是唯一入口。
   - 保留 `toolMsgs` / `hasTrace` 计算逻辑（数据来源不变）。
   - 旧 `.trace-toggle` 内嵌按钮：在画布动作行可用后移除；过渡期可保留为兜底（行为改为打开同一面板，而非内嵌展开），避免双形态并存导致重复渲染。
4. **CSS**：`TracePanel.css` 新增抽屉/侧栏布局类（`.trace-panel--drawer`、遮罩、滑入动画）；保留 `.trace-panel__*` 内容样式；`.trace-toggle` 若旧入口移除则一并清理。

## 影响范围

- `src/components/copilot/TracePanel.tsx`：新增受控抽屉/侧栏形态与 `open`/`onClose` Props。
- `src/components/copilot/TracePanel.css`：新增抽屉布局/动画类，调整或清理 `.trace-panel`（内嵌态）与 `.trace-toggle`。
- `src/components/copilot/CopilotChat.tsx`：移除/降级 `expandedTraces` + `.trace-toggle` 内嵌渲染（L1146-1168），改为持有/转交画布触发的面板状态（过渡期临时按钮）。
- `src/components/copilot/InlineSqlPreview.tsx`：与溯源面板的职责边界对齐——`InlineSqlPreview` 仍负责结果卡内的可执行 SQL 预览/编辑；溯源面板复用其只读 SQL 展示能力（T02 决定是否抽取共享只读 SQL 视图）。本 Task 仅确认不破坏 `InlineSqlPreview` 现有行为。
- F4 `CanvasActions`（动作行）：统一派发 `CanvasActionEvent { action: "trace-sql", artifact }`（跨 Feature 契约，F6 接线）。
- 新增/更新测试：`TracePanel.test.tsx`。

## 验证

- [ ] 单测（`TracePanel.test.tsx`）：`open=false` 不渲染面板内容；`open=true` 渲染、点击遮罩/关闭按钮触发 `onClose`；传入 `toolMessages` 时渲染各步骤标题与展开。
- [ ] 交互验证：点击画布动作行「`</>` SQL·溯源」打开抽屉，再次点击/Esc/遮罩关闭。
- [ ] `trace-sql` 事件携带的 `artifact.sourceMessageId` 能定位到当前产物对应消息。
- [ ] 键盘可达：抽屉打开后焦点进入面板，`Esc` 关闭，焦点回到触发按钮（a11y）。
- [ ] 旧 `.trace-toggle` 内嵌入口移除后，无残留死代码 / 未用 CSS 类。
- [ ] `pnpm typecheck` 通过；`pnpm test` 通过；`pnpm build` 通过。

## 完成标准

- [ ] 溯源面板由 F4 画布动作行「`</>` SQL·溯源」按钮触发，以抽屉/侧栏形态打开，绑定「当前在看产物」对应消息。
- [ ] 形态从「消息内浮动/内嵌折叠块」迁移完成，旧 `.trace-toggle` 唯一入口下线（或降级为打开同一面板）。
- [ ] `TracePanel` 受控接口（`open`/`onClose`）就绪，与 F4 `CanvasActions` 的统一事件契约（`action: "trace-sql"` + `artifact.sourceMessageId`）已写明；F4 未就绪时有临时触发并标注 TODO。
- [ ] 不破坏 `InlineSqlPreview` 现有结果卡内 SQL 预览/编辑行为。
- [ ] 三脚本（typecheck/test/build）全绿，含新增/更新单测。
