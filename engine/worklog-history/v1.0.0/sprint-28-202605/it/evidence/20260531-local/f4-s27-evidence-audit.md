# F4-T02 S27 证据复核

## 范围

复核 Sprint-27 IT04/IT07/IT08/IT09 的 DONE 是否有证据支撑,并清理证据文件中的过期矛盾备注。

## 结论矩阵

| IT | 原状态 | 复核结论 | 证据 |
|----|--------|----------|------|
| IT04 语音提问 -> 出结果 | DONE | 保留 DONE | `f3-t04-composer.md` 记录 Playwright 注入 `SpeechRecognition` final transcript 后走真实 webapp nginx -> analytics -> AI SSE,返回 200 并渲染结果 |
| IT07 存为卡片 -> 资产库可见 | DONE | 保留 DONE | `f7-asset-actions.md` 记录 live `POST /api/card` 后 `GET /api/card` 可按名称找到 |
| IT08 钉到看板 -> 看板可见 | DONE | 保留 DONE | `f7-asset-actions.md` 记录 live `POST /api/dashboard`、`POST /api/dashboard/save` 后列表可见且详情 `ordered_cards[]` 包含卡片 |
| IT09 溯源面板展示口径/表/SQL | DONE | 保留 DONE | `f8-live-contract.md` 记录 live SSE `done.trace.metricCaliber`、`trace.sources[]`、`trace.sql` 同时存在 |

## 边界说明

- IT04 是 headless 浏览器中的语音链路验证:注入的是标准 `SpeechRecognition` final transcript,没有验证物理麦克风权限和真实音频采集;该边界已写入证据,不冒充物理麦克风验证。
- IT09 的纠正写回仍沿用 `/api/ai/nl2sql/feedback` 降级路径,不作为“溯源面板展示口径/表/SQL”的完成前置条件。

## 修正

- `f6-trace-panel.md` 尾部旧的“IT09 保持 TODO / 不标 Live Contract”已改为被 `f8-live-contract.md` 补齐。
- `f8-live-contract.md` 尾部旧的“IT04 仍只有 mock、IT07/IT08 未做 live”已改为指向后续补齐证据。

## 改后

Sprint-27 README、IT README、queue 均保留 DONE。不存在“证据文件仍写未完成但 IT 标 DONE”的矛盾。
