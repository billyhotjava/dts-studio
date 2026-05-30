# T02: OntologyService 对象图解析与多表 JOIN 生成

**优先级**: P0
**状态**: DONE
**依赖**: F0/T02, T01

## 目标

让 OntologyService 能沿 links 把多个对象的视图拼成正确的多表 JOIN SQL，支撑"贯穿/追溯"类查询。

## 技术设计

- 输入：起点对象 + 目标对象（或路径）+ 用户筛选。
- 路径解析：在 linkGraph 上做 BFS 找最短路径；多路径时返回候选让 planner 选择或追问。
- SQL 生成：
  - 全部 LEFT JOIN，保留 is_orphan 行（孤儿不丢）。
  - `biz_ids_json` 这类 JSON 多值键用 PG `jsonb_array_elements_text` 展开后 JOIN。
  - 沿用 flowerbiz guardrails：PG 语法、月份 `to_char`、ILIKE 模糊匹配。
- 输出：SQL + 涉及视图清单（用于 sourceRefs）+ 孤儿提示标记。

## 影响范围

- `OntologyService` 新增对象图遍历与 JOIN 构造方法。
- 复用现有 SQL 执行通道，不新建数据访问层。

## 验证

- [x] 单测：客户→报花两跳生成的 JOIN 正确，孤儿行保留。
- [x] 单测：报花→结算经 biz_ids_json 展开 JOIN 正确。
- [x] 多路径场景返回候选而非随意选路。

## 完成标准

- [x] 对象图导航 SQL 生成有单测覆盖，含软外键与 JSON 展开两个难点。

## 证据

- `it/test_object_graph_join.sh`
- `it/evidence/20260530-local/object-graph-join.md`
