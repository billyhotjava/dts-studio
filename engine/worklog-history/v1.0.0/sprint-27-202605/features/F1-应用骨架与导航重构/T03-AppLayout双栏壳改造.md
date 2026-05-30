# T03: AppLayout 改造为双栏工作台壳

**优先级**: P0
**状态**: READY
**依赖**: T02

## 目标

把 `AppLayout` 从「单列内容区 + 浮动 CopilotSidebar」改造为支持 spec §4「状态二」的双栏工作台容器：**对话脊柱（约 40% 宽）+ 活产物画布（剩余宽）**，并区分两类布局——全屏布局（auth/public/screens）与工作台双栏布局。双栏内部为 F3（对话脊柱）/ F4（活产物画布）预留插槽，本 Task 只做容器与布局，不实现对话/画布业务。

## 技术设计

1. **布局分流**（`dts-copilot-webapp/src/layouts/AppLayout.tsx`）：
   - 现状：`/auth/*`、`/screens/*`、`/public/screen/:uuid` 已在 `routes.tsx` 中处于 `AppLayout` 之外（全屏，无壳），保持不变。
   - `/public/card`、`/public/dashboard` 当前在 `AppLayout` 内，但通过 `isPublicRoute`（`location.pathname.startsWith("/public/")`）跳过鉴权。T03 让公开路由走「极简/无导航」分支——只渲染 `Outlet`，不渲染左导航、双栏、CopilotSidebar、MobileTabBar（与 T05 协同，保证分享页不依赖工作台壳）。
   - 引入布局模式判定：`isWorkspaceRoute = location.pathname === APP_HOME_PATH`（工作台双栏）；其余已登录业务页（dashboards/questions/data 等治理与资产页）走「标准单列内容布局」（保留现有 `.main` + header + `Outlet`）。

2. **双栏容器结构**：
   - 仅在 `isWorkspaceRoute` 时，把 `.main-content` 内的 `Outlet` 包进双栏容器：
     ```
     <div class="workspace-shell">
       <section class="workspace-shell__spine">  {/* F3 对话脊柱插槽 */}
       <section class="workspace-shell__canvas"> {/* F4 活产物画布插槽 */}
     </div>
     ```
   - 两栏内容由 `AgentWorkspacePage`（T02）通过 `Outlet` 渲染；本 Task 在 CSS 与容器层面定义结构与占位，不在 layout 里硬塞业务组件。脊柱/画布的具体子组件由 F3/F4 注入。

3. **CopilotSidebar 处置**：
   - 现状第 536-538 行：`!copilotEmbeddedInPage` 时渲染浮动 `CopilotSidebar`，`copilotEmbeddedInPage = location.pathname === "/agent-bi"`。
   - 改造后工作台采用「内嵌对话脊柱」，浮动 `CopilotSidebar` 在工作台路由下不再渲染（脊柱取代之）。其它非工作台业务页是否保留浮动 Copilot：本 Task 默认在工作台外保留现状浮窗（避免回归），待 F3 落地后再统一收敛。`copilotEmbeddedInPage` 判定改用 `isWorkspaceRoute`。

4. **样式**（`dts-copilot-webapp/src/layouts/layout.css`）：
   - 新增 `.workspace-shell`（`display:flex; height:100%`）、`.workspace-shell__spine`（`flex: 0 0 40%; min-width` + 纵向滚动）、`.workspace-shell__canvas`（`flex: 1; min-width: 0`）。
   - 复用现有设计 token（`--spacing-*`、`--color-border`、`--color-bg-*`）；不硬编码色值/间距。
   - 双栏分隔用 `border-left` 或 surface 分层体现层次；遵循「动画只用 transform/opacity」原则（本 Task 一般无动画）。
   - 响应式：窄屏（移动端，配合 `MobileTabBar`）双栏降级为单栏堆叠或仅脊柱，画布转为可切换视图（具体交互留 F4，本 Task 给基础断点占位）。

5. **保持鉴权逻辑不变**：`sessionStatus` 检查、平台 token/会话校验、`resolvePrivilegedAccess`、登出流程均保持原样，仅调整布局渲染分支。

## 影响范围

（真实文件/模块/路由/接口）

- `src/layouts/AppLayout.tsx`（布局分流、双栏容器、`copilotEmbeddedInPage` → `isWorkspaceRoute`、公开路由极简分支）
- `src/layouts/layout.css`（新增 `.workspace-shell*` 规则与响应式断点）
- 协作：`src/components/copilot/CopilotSidebar`（工作台下不渲染）、F3 对话脊柱、F4 活产物画布（插槽消费方）
- 关联：`src/components/nav/MobileTabBar`（窄屏布局）

## 验证

- [ ] `/agent-bi` 工作台渲染双栏：左侧脊柱占位约 40% 宽、右侧画布占位填满剩余宽。
- [ ] `/auth/*`、`/screens/*`、`/public/*` 全屏/极简渲染，不出现工作台双栏与左导航。
- [ ] 治理/资产页（`/data`、`/dashboards`、`/questions`）仍为标准单列布局，正常可用。
- [ ] 工作台路由下不再出现浮动 `CopilotSidebar`。
- [ ] 窄屏断点下双栏正确降级，无横向溢出（对照 web 测试规则 320/768/1024/1440）。
- [ ] `pnpm typecheck` / `pnpm build` 通过。

## 完成标准

- [ ] `AppLayout` 区分全屏布局 / 工作台双栏布局 / 标准单列布局三种分支。
- [ ] 双栏容器结构与 CSS 落地，F3/F4 插槽就位。
- [ ] 鉴权逻辑零回归；公开路由不再挂工作台壳。
