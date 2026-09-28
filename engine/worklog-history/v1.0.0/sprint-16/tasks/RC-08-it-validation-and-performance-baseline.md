# RC-08 IT 验收与性能基线

**状态**: DONE
**目标**: 为固定报表快路径和 Copilot 探索路径分别建立真实业务验收基线。

## 验收链

页面 -> 模板 -> SQL -> 数据结果 -> 展示组件

## 性能目标

- 固定报表：优先 `1s` 内返回
- 准实时汇总：优先 `3s` 内返回
- Copilot 探索：`5-15s` 可接受

## 核心校验

- 模板命中不再走 NL2SQL
- 数据口径与现网页面一致
- 参数筛选结果一致

## 已完成

- 新增可执行脚本：
  - `it/test_fixed_report_fastpath.sh`
  - `it/test_copilot_template_first.sh`
  - `it/test_multi_surface_fixed_report_reuse.sh`
- 新增当前本地 `3003 -> 8092 -> 8091` 链路性能基线：
  - `it/performance-baseline.md`
- 已实测通过：
  - 固定报表目录 `GET /api/report-catalog`
  - 固定报表运行态 `POST /api/fixed-reports/{templateCode}/run`
  - Copilot 模板优先候选直答
  - Copilot 固定报表直接命中
  - 固定报表目录到 `Dashboard / Report Factory / Screens` 创建流程 handoff
