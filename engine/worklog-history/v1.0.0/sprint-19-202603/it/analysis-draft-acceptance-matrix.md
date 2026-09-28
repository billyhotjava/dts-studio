# Sprint-19 分析草稿工作流验收矩阵

## 覆盖范围

| 场景 | 入口 | 期望 |
|------|------|------|
| 查询资产中心展示草稿 | `/questions` | 可见 `Copilot 草稿` 视图、来源标识、状态标识 |
| Copilot 结果保存草稿 | `InlineSqlPreview` | 生成 `analysis_draft`，不直接污染正式查询目录 |
| 草稿打开查询编辑器 | `/questions/new?draft={id}` | 编辑器显示 `来源：AI Copilot` 与原始问题 |
| 草稿转正式查询 | 编辑器保存 | 未修改草稿时通过 `save-card` 晋升为正式查询 |
| 草稿执行与图表化 | 编辑器运行 | 可在查询编辑器内执行并继续进入图表化链路 |
| 草稿多端入口 | 仪表盘 / 报告工厂 / 大屏 | 识别 `analysisDraft` 查询参数并展示来源上下文 |
| 草稿到仪表盘 | `/dashboards/new?analysisDraft={id}` | 自动晋升查询卡片并在空白仪表盘中挂入首张图卡 |
| 草稿到报告工厂 | `/report-factory?analysisDraft={id}` | 若草稿关联会话，可默认带入 `session` 作为报告来源 |
| 草稿到大屏 | `/screens?analysisDraft={id}` | 可基于草稿语义直接生成大屏 prompt |

## 当前证据

- 轻量契约测试：
  - `tests/analysisDraftReuseModel.test.ts`
  - `tests/analysisDraftSurfaceEntry.test.ts`
  - `tests/queryAssetCenterModel.test.ts`
  - `tests/queryDraftHandoff.test.ts`
  - `tests/copilotAnalysisDraft.test.ts`
- 类型与构建：
  - `pnpm run typecheck`
  - `pnpm run build`
- 一键脚本：
  - `it/test_analysis_draft_workflow.sh`

## 待补真人联调

- 登录后在 Copilot 里生成一条 SQL 类回答
- 验证 `保存草稿`
- 验证 `在查询中打开`
- 验证未修改草稿时保存为正式查询
- 验证修改草稿后保存会按当前编辑内容生成正式查询
