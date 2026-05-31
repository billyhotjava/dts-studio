# Sprint-29 集成测试(IT)证据

实施完成后此处必须留**真实证据**(命令输出、对平台的 live contract、截图、用例结果),不得空占位。

## 证据分层

- **Mock Contract**:用 mock 指标目录/取值 fixture 验证前端消费与渲染。
- **Degraded Runtime**:平台不可达时验证显式降级 + 一键退回现生成 SQL。
- **Live Contract**:真实调 dts-platform `/api/governance/indicators*` 跑通端到端。

## IT 清单

| ID | 集成检查 | 关联 Feature | 阶段 | 状态 | 证据 |
|----|----------|--------------|------|------|------|
| IT01 | 机器账号能 live 拉到已发布指标目录 + 定期/手动刷新 | F1 | P1 | TODO | Live Contract |
| IT02 | copilot-ai 暴露的 BFF 端点返回目录/取值,webapp 可消费 | F1,F2 | P1 | TODO | - |
| IT03 | 资产库「平台指标」浏览 → 点开 → echarts 渲染 Indicator 产物 | F2 | P1 | TODO | - |
| IT04 | 指标取值(dashboard/detail)live 调通,口径/数据与平台一致 | F1,F2 | P1 | TODO | Live Contract |
| IT05 | 问数命中已发布指标 → 优先调指标 API;未命中 → 退回现生成 SQL | F3 | P2 | TODO | Mock + Live |
| IT06 | 命中指标做成可改芯片(切候选/退回);溯源显示权威来源 vs 现生成 | F4 | P2 | TODO | - |
| IT07 | 平台不可达/超时 → 显式降级 + 一键退回,不静默不阻断 | F1,F3 | 全程 | TODO | Degraded Runtime |
| IT08 | drilldown 下钻 / 口径 version 变更提醒 | F5 | P3 | TODO | - |
| IT09 | `pnpm typecheck` + `pnpm test` + `pnpm build`(前端)+ 后端单测全绿 | 全部 | 全程 | TODO | - |
| IT10 | 确认 dts-platform 零代码改动(仅机器账号配置) | F1 | P1 | TODO | - |

## 验证环境

- 后端:`dts-copilot-ai`;前端:`dts-copilot-webapp`(`pnpm dev`,端口 3003)
- 平台:dts-platform `/api/governance/indicators*`(机器账号只读)
- 截图/产物存 `../assets/`
