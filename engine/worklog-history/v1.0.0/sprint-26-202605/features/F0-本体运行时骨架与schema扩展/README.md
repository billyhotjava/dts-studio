# F0: 本体运行时骨架与 schema 扩展

**优先级**: P0
**状态**: DONE

## 目标

在不破坏现有 NL2SQL 行为的前提下，扩展 semantic-pack schema（追加 links/metrics/signals/actions 四节），并新增薄运行时 `OntologyService` 把 pack 升级为可导航对象图。本 Feature 是后续三层的地基，必须先做且必须向后兼容。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | semantic-pack schema 扩展与向后兼容校验 | P0 | DONE | 无 |
| T02 | OntologyService 骨架与 pack 加载 | P0 | DONE | T01 |
| T03 | 现有报花 NL2SQL 回归基线 | P0 | DONE | 无 |

## 完成标准

- [x] 四节（links/metrics/signals/actions）有明确 schema 定义与校验，缺省时不报错（旧 pack 零改动可加载）。
- [x] `OntologyService` 能从 `SemanticPackService` 加载结果构建内存对象图模型，空跑不改变现有 planner 输出。
- [x] 报花域现有 fewShots / 高频问句回归通过，无退化。
