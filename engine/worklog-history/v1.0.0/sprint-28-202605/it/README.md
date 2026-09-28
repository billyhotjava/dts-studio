# Sprint-28 集成测试(IT)计划

## 规则

- IT 项默认 `READY`;只有证据文件存在且内容可复现后才能改 `DONE`。
- 浏览器行为必须用 Playwright 或等价人工记录证明,不能只靠 typecheck/test/build。
- 对历史状态簿记的修正必须写明“改前、证据、改后”。

## IT 清单

| ID | 集成检查 | 关联 Feature | 优先级 | 状态 | 证据 |
|----|----------|--------------|--------|------|------|
| IT00 | Sprint-28 基线: typecheck/test/build 当前绿灯记录 | 全部 | P0 | DONE | `evidence/20260531-local/it00-baseline.md` |
| IT01 | `/agent-bi?view=sessions` 渲染历史会话视图 | F1 | P0 | DONE | `evidence/20260531-local/f1-workspace-routing.md` |
| IT02 | `/agent-bi?view=signals` 渲染信号视图或受控空态 | F1,F3 | P0 | DONE | `evidence/20260531-local/f1-workspace-routing.md`; `evidence/20260531-local/f3-signals.md` |
| IT03 | `/agent-bi?fixedReport=PRS-FLOWERBIZ-OVERVIEW` 保留上下文并执行或报明确错误 | F1 | P0 | DONE | `evidence/20260531-local/f1-workspace-routing.md` |
| IT04 | `/assets?tab=` 三类资产无双 PageHeader | F2 | P0 | DONE | `evidence/20260531-local/f2-asset-library.md` |
| IT05 | `/dashboards`、`/questions`、`/collections` 列表入口收敛,详情/编辑深链不破坏 | F2 | P0 | DONE | `evidence/20260531-local/f2-asset-library.md` |
| IT06 | 冷启动信号卡不再展示假业务预警 | F3 | P1 | DONE | `evidence/20260531-local/f3-signals.md` |
| IT07 | S25/S26/S27 queue 与证据口径校准 | F4 | P1 | DONE | `evidence/20260531-local/f4-queue-consistency.md` |
| IT08 | `Nl2SqlEvalPage` 和 appShellConfig 死常量完成去留处理 | F5 | P2 | DONE | `evidence/20260531-local/f5-cleanup.md` |
| IT09 | 全量收口验证: typecheck/test/build + Playwright smoke | 全部 | P0 | DONE | `evidence/20260531-local/it09-final-verification.md` |
| IT10 | Agent BI 单窗口结果面: 不再渲染右侧 CanvasPanel,inline SQL 自动预览 | F6 | P0 | DONE | `evidence/20260531-local/f6-single-window.md` |

## 预期验证命令

```bash
cd dts-copilot-webapp
pnpm typecheck
pnpm test
pnpm build
```

浏览器 smoke 使用本地 webapp,至少覆盖:

- `/agent-bi?view=sessions`
- `/agent-bi?view=signals`
- `/agent-bi?fixedReport=PRS-FLOWERBIZ-OVERVIEW`
- `/assets?tab=dashboards`
- `/assets?tab=cards`
- `/assets?tab=collections`
- `/dashboards`
- `/questions`
- `/collections`
