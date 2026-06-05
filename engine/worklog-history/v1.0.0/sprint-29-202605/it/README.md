# Sprint-29 集成测试(IT)证据

实施完成后此处必须留**真实证据**(命令输出、对平台的 live contract、截图、用例结果),不得空占位。

## 证据分层

- **Mock Contract**:用 mock 指标目录/取值 fixture 验证前端消费与渲染。
- **Degraded Runtime**:平台不可达时验证显式降级 + 一键退回现生成 SQL。
- **Live Contract**:真实调 dts-platform `/api/governance/indicators*` 跑通端到端。

## IT 清单

| ID | 集成检查 | 关联 Feature | 阶段 | 状态 | 证据 |
|----|----------|--------------|------|------|------|
| IT01 | 机器账号能 live 拉到已发布指标目录 + 定期/手动刷新 | F1 | P1 | DONE | `evidence/20260531-local/f1-f5-indicator-routing-drilldown.md`(服务认证 200,local fixture `fetched=1`) |
| IT02 | analytics BFF 端点返回目录/取值,webapp 可消费 | F1,F2 | P1 | DONE | `evidence/20260531-local/f1-f2-indicator-bff-asset-preview.md` |
| IT03 | 资产库「平台指标」浏览 → 点开 → echarts 渲染 Indicator 产物 | F2 | P1 | DONE | `evidence/20260531-local/f1-f2-indicator-bff-asset-preview.md` |
| IT04 | 指标取值(dashboard/detail)live 调通,口径/数据与平台一致 | F1,F2 | P1 | DONE | `evidence/20260531-local/f1-f5-indicator-routing-drilldown.md`(local fixture: dashboard 1 行、detail 2 行、drilldown 3 行) |
| IT05 | 问数命中已发布指标 → 优先调指标 API;未命中 → 退回现生成 SQL | F3 | P2 | DONE | `evidence/20260531-local/f1-f5-indicator-routing-drilldown.md` |
| IT06 | 命中指标做成可改芯片(切候选/退回);溯源显示权威来源 vs 现生成 | F4 | P2 | DONE | `evidence/20260531-local/f1-f5-indicator-routing-drilldown.md` |
| IT07 | 平台不可达/超时 → 显式降级 + 一键退回,不静默不阻断 | F1,F3 | 全程 | DONE | `evidence/20260531-local/f1-f5-indicator-routing-drilldown.md` |
| IT08 | drilldown 下钻 / 口径 version 变更提醒 | F5 | P3 | DONE | `evidence/20260531-local/f1-f5-indicator-routing-drilldown.md` |
| IT09 | `pnpm typecheck` + `pnpm test` + `pnpm build`(前端)+ 后端单测全绿 | 全部 | 全程 | DONE | `evidence/20260531-local/f1-f5-indicator-routing-drilldown.md` |
| IT10 | 确认 dts-platform 指标业务零改动,仅服务认证只读白名单/配置变更 | F1 | P1 | DONE | `evidence/20260531-local/f1-f5-indicator-routing-drilldown.md` |
| IT11 | 资产库看板 tab 展示 PRS 固定报表/大屏资产组,入口跳转 Copilot 大屏预览链路 | F6 | P1b | DONE | `evidence/20260601-local/f6-prs-fixed-report-assets.md`; `evidence/20260601-local/f6-v1-screen-assets-runtime-sync.md` |
| IT12 | 旧 WH/FIN/PROC 固定报表归档模板不再生成死链,旧 URL 降级为 Agent 分析 | F6 | P1b | DONE | `evidence/20260601-local/f6-legacy-fixed-report-deadlinks.md` |
| IT13 | PRS v1 screen 原型落成 `analytics_screen`,预览 API 可返回组件和变量 | F6 | P1c | DONE | `evidence/20260601-local/f6-v1-screen-assets-runtime-sync.md` |

## 验证环境

- 后端:`dts-copilot-ai`;前端:`dts-copilot-webapp`(容器端口 50080;本地开发端口 3003)
- 平台:dts-platform `/api/governance/indicators*`(机器账号只读)
- 截图/产物存 `../assets/`
