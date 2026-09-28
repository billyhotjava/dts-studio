# Sprint-22: 报花域语义化收口 + dts-stack 治理层试点 (FB)

**时间**: 2026-05
**前缀**: FB (Flower Biz)
**状态**: READY
**目标（双轨）**:

- **轨 1（dts-copilot 内 / 智能层）**：建立报花（flowerbiz）域的 NL2SQL 完整链路 —— `flowerbiz.json` 语义包 + 路由规则 + Few-shot 模板，让"加摆/撤摆/换花/调花/坏账/销售/赠花/起租/审核"等业务问句进入快路径，停止在裸 `t_flower_biz_*` 表上现场拼 SQL。
- **轨 2（dts-stack 内 / 治理层）**：在 dts-stack dbt 工作流中新建花卉业务命名空间 `xycyl_*`，**从 0 把报花域 5 层（ods → stg → dwd → dws → ads）建齐**，纳入 Airflow 调度与 OpenMetadata 治理。dts-copilot 通过 datasource 直读 dbt 产出，自身保留独立部署能力。

## 背景

### 报花域是什么

馨懿诚绿植租摆业务的事实链：

```
报花 → 采购 → 配送/入库 → 摆放/养护 → 结算 → 收款
 ↑
 业务事实源头（所有下游 mart 的真正数据源）
```

报花单 `t_flower_biz_info` 是业务事实核心。财务 settlement / month_accounting / customer_ar 都是报花的**聚合衍生**。

### 为什么先做报花，不做财务

最初 sprint-22 设计为财务域语义化（基于 sprint-19~21 已落地的 8 张财务报表）。2026-05 深入 review `rs-flowers-base/flowerbiz` 模块后发现：

- 不先吃透报花 13 种 bizType 与 7 个状态，财务 mart 的金额 `SUM` 全错（销售/赠送走另一条结算链；坏账金额负值；调拨内部对冲）
- 不先做报花 dbt 模型，财务 mart 的上游永远是裸 `t_flower_biz_*` 表，dbt 复杂度爆炸

财务版本归档到 `_archived-finance-version-for-sprint-25/`，sprint-25 启动时复用双轨架构与工艺。

### 报花域的真实复杂度

#### 7 个状态

| status | 中文 | 业务含义 |
|---|---|---|
| 20 | 草稿 | 未提交 |
| 1 | 审核中 | 等项目经理 / 业务经理审 |
| 21 | 驳回 | 退回草稿重编 |
| 2 | 备货中 | 已审，等采购 + 配送 + 入库 |
| 3 | 核算中 | 入库完成，等财务核算 |
| 4 | 待结算 | 核算完成，等月底结算 |
| 5 | 已结束 | 全流程完成 |
| -1 | 作废 | 任意状态可作废 |

#### 13 种 bizType（报花的真正复杂度核心）

| bizType | 中文 | 金额方向 | 走的链路 | adminweb 子目录 |
|---|---|---|---|---|
| 1 | 换花 | 中性 | 标准（rent 不变） | `flower/flowerbiz/biz` |
| 2 | 加花 / 加摆 | **+正** | 标准（rent 增加 + 触发采购） | `flower/flowerbiz/add` |
| 3 | 减花 / 撤摆 | **-负** | 标准（rent 减少 + 触发回收） | `flower/flowerbiz/cut` |
| 4 | 调花 / 调拨 | 中性 | 跨摆位 / 跨库房 | `flower/flowerbiz/transfer` |
| 6 | 坏账 | **-负** | **走 ISaleAccountService** | `flower/flowerbiz/baddebt` |
| 7 | 售花 / 销售 | +正 | **走 ISaleAccountService** | `flower/sale` |
| 8 | 赠花 | 0 | **走 ISaleAccountService** | (在 sale 内) |
| 10 | 配料 | +正 | 辅料 | (在 add 内) |
| 11 | 加盆架 | +正 | 容器 | `flower/flowerbiz/addBasket` |
| 12 | 减盆架 | -负 | 容器 | (在 cut 内) |

> 13 种 bizType 在 adminweb 是分开的表单，业务人员问"加花"和"撤摆"是两件事，不能合成一个问句模板。

### 报花联动的 6 个真实陷阱（dbt 模型设计必须处理）

| # | 陷阱 | dbt 设计影响 |
|---|---|---|
| 1 | 异步触发（`@Async`）：报花 status=2 后 PlanPurchaseItem 不立刻有 | dbt source freshness 不能严格设阈值；mart 物化前提是上下游已对齐 |
| 2 | 软外键满天飞（plan_purchase_item.flower_item_id / project_green / settlement_item 都无 DB 约束） | dbt schema.yml `relationships` 测试用 `severity: warn`；dwd 层 LEFT JOIN |
| 3 | `t_flower_biz_item.status` 在 4 个 service 同时写 | incremental 模型用 `update_time` 不用 `status` 作 unique_key |
| 4 | 金额符号：bizType=2/10/11 正，bizType=3/6/12 负 | ads 层"应收金额"必须按 bizType 分组聚合，不能直接 SUM |
| 5 | bizType=7/8（售花/赠花）走 `ISaleAccountService`，**不进 SettlementItem** | 必须分两个 ads：`xycyl_ads_flowerbiz_lease_*` + `xycyl_ads_flowerbiz_sale_*` |
| 6 | 时间字段不可靠（`start_lease_time` 可被事后改，由 `t_flower_rent_time_log` 记录） | 按月汇总用哪个时间字段，必须先做口径访谈决策 |

### 5 个口径决策（实施前业务方必须拍板）

具体见 `assets/flowerbiz-caliber-decisions.md`：

1. 报花的"业务时间"是什么？（`apply_time` / `start_lease_time` / `settlement_time` / `finish_time`）
2. 13 种 bizType 在 mart 里怎么分组？（每种独立 / 按"租赁/销售/坏账/调拨"4 类合并 / 不聚合让 LLM 现场过滤）
3. 异步联动的"数据滞后"如何处理？（强制 JOIN / 只取已对齐 / 分两个 mart）
4. 金额符号在 mart 层规范化吗？（保留原始 / 统一变正 + amount_direction 字段）
5. 销售/赠花是否进财务 mart？（合并 / 独立 + UNION）

### 双轨架构与"独立部署"约束

dts-stack 的 dbt 工作流已经成型：`models/{ods,stg,dwd,dws,ads}/` 五层规范、`dwh_finance_dbt_manual.py` 等 DAG 用 `tag:finance` 选择模型、`services/dts-openmetadata/ingestion/dbt.yml` 把 dbt manifest/catalog/run_results 自动推到 OpenMetadata。但 **dts-stack 现有的 `fin_*` / `pm_*` 模型是基金管理 / 项目管理业务，馨懿诚花卉业务的资产 0 张**。

本 sprint 决定（与归档版相同）：

- **mart 视图未来全部由 dbt 写**（不再用 PG materialized view + Liquibase 手维护）
- **OpenMetadata 是元数据治理 SOT**（从 dbt manifest 派生）
- **dts-copilot 保留独立部署能力**：开发期 dts-stack 跑 dbt → 编译产物（mart DDL + manifest 元数据 + Glossary 导出）作为静态资产打包进 dts-copilot 镜像
- **sprint-22 双轨并行**：F1/F2/F3 完成 dts-copilot 内的语义化（短期可上线，先给业务 ROI）；F4 完成 dts-stack 内的报花 dbt 试点（长期治理产出）；F4 完成后 F1-T03 / F1-T04 自动派生

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 状态 | 轨 | 说明 |
|----|---------|---------|--------|------|---|------|
| F0 | 业务发现与口径决策 | 5 | P0 | READY | discovery | 真实问句采集 + 状态机文档 + 口径访谈 + 数据画像 + 消费者画像 |
| F1 | 报花域语义资产基线 | 4 | P0 | READY | 1 (copilot) | 盘点 + flowerbiz.json + 字段语义 + 字典 |
| F2 | 报花 NL2SQL 快路径与 planner 直达 | 3 | P0 | READY | 1 (copilot) | 模板 + routing rule + few-shots/guardrails |
| F3 | 报花域回归与验收 | 3 | P1 | READY | 1 (copilot) | IT 脚本 + 验收矩阵 + 真人联调 |
| F4 | dts-stack 治理层试点（报花域） | 5 | P0 | READY | 2 (dts-stack) | dbt sources + 5 层模型 + Airflow DAG + OpenMetadata 集成 + datasource 切换 |

## 影响范围

### 轨 1：dts-copilot 内（智能层）

#### 仅新增

- `dts-copilot-ai/src/main/resources/semantic-packs/flowerbiz.json` —— 新增（**主交付物**）
- `dts-copilot-ai/src/main/resources/prompts/flowerbiz-few-shots.txt` —— 新增（可选）
- `dts-copilot-ai/src/main/resources/prompts/flowerbiz-constraints.txt` —— 新增（13 bizType / 6 陷阱 / 5 口径硬约束）
- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__flowerbiz_query_templates.xml` —— 新增
- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__flowerbiz_routing_rules.xml` —— 新增
- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__flowerbiz_biz_enum.xml` —— 新增（短期）

#### 字段语义补齐（短期）

- `copilot_analytics.analytics_table` / `analytics_field` —— UPDATE flowerbiz 行（**短期**；F4 完成后从 OpenMetadata Catalog API 派生）

#### datasource 切换（F4 收尾）

- `copilot_ai.ai_data_source` —— 新增"花卉报花 mart" datasource

### 轨 2：dts-stack 内（治理层）

#### 仅新增

- `services/dts-dbt/models/xycyl/sources/xycyl_ods_flowerbiz_sources.yml` —— **报花 7 张关键源表**：`t_flower_biz_info` / `t_flower_biz_item` / `t_flower_biz_item_detailed` / `t_change_info` / `t_recovery_info` / `t_recovery_info_item` / `t_flower_rent_time_log`
- `services/dts-dbt/models/xycyl/stg/xycyl_stg_flower_biz_*.sql` —— 7 张 stg 模型
- `services/dts-dbt/models/xycyl/dwd/xycyl_dwd_flowerbiz_*.sql` —— 含 `bizType` 翻译为中文的维度表
- `services/dts-dbt/models/xycyl/dws/xycyl_dws_flowerbiz_*.sql` —— 项目 × 月 / 客户 × 月 / 养护人 × 月 三个汇总
- `services/dts-dbt/models/xycyl/ads/xycyl_ads_flowerbiz_*.sql` —— **8 张 ads mart 模型**（详见 `assets/flowerbiz-mart-catalog.md`）
- `services/dts-dbt/models/xycyl/xycyl_flowerbiz_schema.yml` —— dbt 测试 + 文档
- `services/dts-airflow/dags/dwh/dwh_xycyl_flowerbiz_dbt_manual.py` —— Airflow DAG（selector=`tag:xycyl-flowerbiz`）
- `services/dts-openmetadata/ingestion/xycyl_dbt.yml` —— OpenMetadata 拉花卉 dbt manifest

#### 不改

- dts-stack 现有 `fin_*` / `pm_*` / `dwh_dbt_dbt_manual.py` 等基金 / 项目管理业务的 dbt 资产
- dts-stack 的 docker-compose / OpenMetadata / Ranger / Trino 部署形态

## 完成标准

### F0：业务发现（前置门）

- [ ] 报花真实问句采集 ≥ 100 条（按 13 bizType 分类）
- [ ] 报花单全生命周期状态机图 + 7 状态业务义释 + 13 bizType 业务义释 落地
- [ ] 5 个口径决策有业务方拍板结论
- [ ] 7 张源表的数据画像报告（空值率 / 状态实际分布 / 时间断点）
- [ ] 至少 5 类消费者（养护人 / 项目经理 / 业务经理 / 财务 / 老板）的报花问句决策路径文档化

### 轨 1（dts-copilot 智能层）

- [ ] `flowerbiz.json` 语义包覆盖 8+ 业务对象（报花单 / 加摆 / 撤摆 / 换花 / 调花 / 坏账 / 销售 / 回收）+ 30+ 同义词 + 10+ few-shots + 8 guardrails
- [ ] `BizEnumDictionary` 增补 3 类报花枚举：`flower_biz.status`（7 状态）、`flower_biz.biz_type`（13 类型）、`recovery.recovery_type`（3 类去处）
- [ ] 报花 8+ 类典型问句进入 `TEMPLATE_FAST_PATH`，不再走 AGENT_WORKFLOW
- [ ] `Nl2SqlRoutingRule` 中 `flowerbiz` 域权重 ≥ procurement / project，避免被错路由
- [ ] 远程 `ai.yuzhicloud.com` 上 8 类典型问句返回结果与旧系统列表页口径一致
- [ ] `it/test_flowerbiz_query_regression.sh` 在远程库通过
- [ ] 至少 5 类问句在 ChatPanel 真人联调通过
- [ ] guardrails 沉淀：禁止把 13 bizType 直接 `SUM(bizTotalRent)`、禁止跨业务链路（销售 vs 租赁）混合聚合、禁止用 `create_time` 作业务时间

### 轨 2（dts-stack 治理层）

- [ ] dbt 工作空间下 `models/xycyl/` 命名空间建立，与现有 `fin_*` / `pm_*` 无命名冲突
- [ ] 报花 7 张源表全部声明为 dbt sources
- [ ] 8 张 ads mart 模型 + 必要的 stg/dwd/dws 落地，schema.yml 测试覆盖关键字段
- [ ] dbt 模型全部带 `tags=['xycyl', 'xycyl-flowerbiz', '<bizType>']`，可被 Airflow `selector=tag:xycyl-flowerbiz` 命中
- [ ] `dwh_xycyl_flowerbiz_dbt_manual.py` 可在 Airflow 触发并跑通 `dbt run + dbt test + dbt docs generate`
- [ ] OpenMetadata 中可看到模型血缘 + Glossary 词汇表
- [ ] dts-copilot 注册"花卉报花 mart" datasource，能直读 ads 表
- [ ] 同一组报花问句在新旧两个 datasource 跑出的结果一致（行数 100% / 数量±1 / 金额差 < 0.01 元）

### 双轨衔接

- [ ] dts-copilot 镜像 + 静态资产 tarball 可在不连接 dts-stack 的环境独立部署
- [ ] sprint-23（采购域）启动前确认报花 ads 可作为采购域上游 ref

## 与相邻 sprint 的关系

| Sprint | 关系 |
|---|---|
| sprint-19/20/21 | 财务报表数据面已落地，sprint-22 不动它们；sprint-25 复用 + 重做 |
| sprint-20 | F6 采购域语义收口的工艺范式（authority SQL → semantic pack → query template → planner rule → IT 回归），本 sprint 双轨化扩展 |
| **sprint-23** | 采购域（purchase）双轨化，**ref 报花域 dws** 作为上游，工作量明显 < sprint-22 |
| **sprint-24** | 摆放域（project_green / pendulum）双轨化 |
| **sprint-25** | 财务域（settlement / month_accounting）双轨化，**复用本 sprint 归档的 finance 版本** + ref 报花/采购/摆放 dws |
| sprint-26 | EltSyncService 正式删除（确认所有域 datasource 已切到 dts-stack 产出后） |
| sprint-27 | dts-copilot 接入 OpenMetadata Glossary，BizEnumDictionary / analytics_field 进入只读兜底模式 |

## 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| 13 bizType 在 mart 直接 SUM 出错 | 数据失真，业务方失去信任 | 强制按 bizType 分组聚合；few-shots 给全 13 例；guardrails 显式列出 |
| bizType=7/8 走另一条结算链漏掉 | 销售收入丢失 | 拆分 `lease` vs `sale` 两类 ads；guardrails 标注 |
| 异步链路导致数据滞后 | mart 与现网 UI 不一致 | F4-T05 加"datasource 数据时效"探针；超阈值降级到旧 datasource |
| 时间字段口径未对齐 | 月度汇总数据不可比 | F0-T03 口径访谈作为前置门，未拍板不进 F1 |
| 软外键孤儿数据 | dbt schema test 大量 warn | `severity: warn` 而非 error；专门一个 dws 跟踪孤儿率 |
| 状态字段竞态 | incremental 模型遗漏更新 | 用 `update_time` 作 unique_key；数据画像跟踪 `update_time` 单调性 |
| dts-stack 现有 `fin_*` 命名容易撞车 | dbt 编译错误 / OpenMetadata 标签污染 | 花卉业务一律 `xycyl_` 前缀，schema 走单独的 `xycyl_*` 而非 `public` |
| dts-copilot 独立部署需要 dts-stack 的 dbt 产物 | 部署门槛升高 | `bin/export-dbt-artifacts.sh` 导出 mart DDL + manifest 元数据 + glossary tarball |
| dts-stack dbt 跑失败 | 业务数据陈旧 | F4-T05 健康检查探针；超阈值告警 |

## 输出物清单

### 文档（本目录）

- `README.md`（本文件）
- `features/F0-业务发现与口径决策/`：5 个 Task 文档（discovery 阶段）
- `features/F1-报花域语义资产基线/`：4 个 Task
- `features/F2-报花NL2SQL快路径与planner直达/`：3 个 Task
- `features/F3-报花域回归与验收/`：3 个 Task
- `features/F4-dts-stack治理层试点/`：5 个 Task
- `it/README.md` —— sprint IT 入口
- `it/test_flowerbiz_query_regression.sh` —— 远程报花回归脚本
- `it/flowerbiz-query-regression.md` —— 报花回归基线
- `it/test_xycyl_dbt_consistency.sh` —— dts-stack 产出 vs 旧 datasource 一致性脚本（F4 产出）
- `assets/flowerbiz-source-catalog.md` —— **报花 7 张源表完整字段口径 + 状态机 + bizType 解释**
- `assets/flowerbiz-mart-catalog.md` —— 8 张 ads mart 设计清单 + dbt 模型映射
- `assets/flowerbiz-caliber-decisions.md` —— 5 个口径决策模板（业务方填）
- `assets/dts-stack-dbt-conventions.md` —— 花卉业务在 dts-stack 的命名 / tag / 分层规范

### 代码（dts-copilot 仓）

见上方"影响范围 → 轨 1"

### 代码（dts-stack 仓）

见上方"影响范围 → 轨 2"

## 归档说明

最初 sprint-22 的 finance 域版本归档在 `_archived-finance-version-for-sprint-25/`，sprint-25 启动时复用其工艺范式与文档结构。
