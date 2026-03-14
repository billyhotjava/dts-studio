# NV-06: CardEditorPage autorun 支持

**状态**: READY
**依赖**: FE-03

## 目标

CardEditorPage 支持 `autorun=1` URL 参数，跳转后自动执行 SQL 并渲染结果图表。

## 技术设计

### URL 参数读取

CardEditorPage 已支持从 URL 读取 `sql`、`db`、`name` 参数。增加 `autorun` 参数：

```typescript
const searchParams = new URLSearchParams(location.search);
const autorun = searchParams.get("autorun") === "1";
```

### 自动执行逻辑

在组件 mount 后（`useEffect`），当满足以下条件时自动执行：
- `autorun === true`
- `sql` 参数非空
- `db` 参数非空（数据库 ID）
- 编辑器模式为 "sql"

执行流程（复用现有手动"运行"按钮的逻辑）：
1. 设置 loading 状态
2. 调用 `analyticsApi.runDatasetQuery(datasetQuery)`
3. 渲染 `ChartRenderer`
4. 清除 loading 状态

### 防重复执行

使用 `useRef` 标记已自动执行过，避免 React Strict Mode 或 re-render 导致重复查询。

## 影响文件

- `CardEditorPage.tsx`（修改：增加 autorun 逻辑）

## 完成标准

- [ ] 访问 `/questions/new?sql=SELECT 1 AS val&db=1&autorun=1`，页面自动执行并显示结果
- [ ] 不带 `autorun` 时行为不变（需手动点运行）
- [ ] 自动执行期间显示 loading 状态
- [ ] SQL 执行出错时正常显示错误信息
