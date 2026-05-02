# Sprint-22: 财务结算域语义包收口 + dts-stack 治理层试点 (FS)

**时间**: 2026-05
**前缀**: FS (Finance Semantics)
**状态**: READY
**目标（双轨）**:

- **轨 1（dts-copilot 内 / 智能层）**：把 sprint-19 ~ sprint-21 已落地的 8+ 张财务 authority / mart 视图整合成 LLM 可识别的 `finance` 语义资产，让财务类自然语言问句进入"语义路由 + 模板 + Few-shot"快路径，停止在裸 `f_*` / `a_*` 表上现场拼 SQL。
- **轨 2（dts-stack 内 / 治理层）**：在已成型的 dts-stack dbt 工作流中新建花卉业务命名空间，用 dbt 重写 sprint-21 落地的 mart 视图，纳入 Airflow 调度与 OpenMetadata 治理；dts-copilot 通过 datasource 直读 dbt 产出，**自身保留独立部署能力**。

## 背景

经 sprint-16 业务页面盘点、sprint-19/20/21 固定报表数据面收口，财务域已经具备：

- 8 张已落地或即将落地的 authority / mart 视图：
  - `authority.finance.settlement_summary`
  - `authority.finance.receivable_overview`
  - `authority.finance.advance_request_status`
  - `authority.finance.pending_receipts_detail`
  - `authority.finance.pending_payment_approval`
  - `authority.finance.project_collection_progress`
  - `authority.finance.reimbursement_status` / `reimbursement_list`
  - `authority.finance.invoice_reconciliation`
  - `mart.finance.customer_ar_rank_daily`
- 8 张固定报表 `templateCode`（FIN-AR-OVERVIEW / FIN-CUSTOMER-AR-RANK / FIN-PROJECT-COLLECTION-PROGRESS / FIN-PENDING-RECEIPTS-DETAIL / FIN-PENDING-PAYMENT-APPROVAL / FIN-ADVANCE-REQUEST-STATUS / FIN-REIMBURSEMENT-STATUS / FIN-INVOICE-RECONCILIATION）

但 `dts-copilot-ai/src/main/resources/semantic-packs/` 下只有：

```
field-operations.json
procurement.json
project-fulfillment.json
```

**缺 `finance.json`**。这导致：

1. 财务类问句（`"上月应收金额"` / `"客户 X 欠款"` / `"待付款审批清单"`）经常落到 `Nl2SqlService` 的 AGENT_WORKFLOW 兜底分支，让 LLM 现场拼 `f_*` / `a_month_accounting` 表
2. 同义词没沉淀（`应收 / 待收 / 欠款 / 未收` 是同一字段；`已付 / 实付 / 已支付` 是同一字段），LLM 容易写出口径错位的 SQL
3. 没有 guardrails，LLM 可能绕过已建好的 `authority.finance.settlement_summary` 直接去拼底层 `f_settlement` + `f_settlement_item`，破坏数据面投资
4. 没有 few-shots，LLM 对"按账期 / 按项目 / 按客户"分组的口径无锚

本 sprint 不再建新视图（sprint-21 已经把视图建齐），主要工作是**把已落地的视图语义化、配字典、配路由、配回归**。

### 双轨架构与"独立部署"约束

dts-stack 的 dbt 工作流已经成型：`models/{ods,stg,dwd,dws,ads}/` 五层规范、`dwh_finance_dbt_manual.py` / `dwh_project_management_dbt_manual.py` 等 DAG 用 `tag:finance` 选择模型、`services/dts-openmetadata/ingestion/dbt.yml` 把 dbt manifest/catalog/run_results 自动推到 OpenMetadata。但 **dts-stack 现有的 `fin_*` / `pm_*` 模型是基金管理 / 项目管理业务，馨懿诚花卉业务的 mart 还没在 dbt 里**。

本 sprint 决定：

- **mart 视图未来全部由 dbt 写**（不再用 PG materialized view + Liquibase 手维护）
- **OpenMetadata 是元数据治理 SOT**（从 dbt manifest 派生，不再手维护 `analytics_table` / `analytics_field`）
- **dts-copilot 保留独立部署能力**：开发期 dts-stack 跑 dbt → 编译产物（mart DDL + dbt manifest 关键元数据 + OpenMetadata Glossary 导出）作为静态资产打包进 dts-copilot 镜像；外部独立部署场景不要求 dts-stack 在线
- **sprint-22 双轨并行**：F1/F2/F3 完成 dts-copilot 内的语义化（短期可上线）；F4 完成 dts-stack 内的 dbt 试点（长期产出物）；F4 完成后 F1-T03 / F1-T04 自动派生，无需手维护

## 现状核实

### 财务域已落地的取数面

| Source Type | Target Object | 状态 | 主要场景 |
|---|---|---|---|
| AUTHORITY_SQL | `authority.finance.settlement_summary` | sprint-19 落地 | 月度结算总览（应收/已收/未收） |
| VIEW | `authority.finance.receivable_overview` | sprint-21 待收口 | 应收概览 KPI |
| VIEW | `authority.finance.advance_request_status` | sprint-19/20 落地 | 预支申请状态板 |
| VIEW | `authority.finance.pending_receipts_detail` | sprint-21 落地 | 待收款明细 |
| VIEW | `authority.finance.pending_payment_approval` | sprint-21 落地 | 待付款审批 |
| VIEW | `authority.finance.project_collection_progress` | sprint-21 落地 | 项目回款进度 |
| VIEW | `authority.finance.reimbursement_status` | sprint-19/20 落地 | 报销状态 |
| AUTHORITY_SQL | `authority.finance.reimbursement_list` | sprint-20 落地（batch2） | 报销列表 |
| VIEW | `authority.finance.invoice_reconciliation` | sprint-19/20 落地 | 发票对账 |
| MART | `mart.finance.customer_ar_rank_daily` | sprint-21 落地 | 客户欠款排行 |

### 已存在但归在 project 域的视图（关联）

- `v_monthly_settlement`（在 `project-fulfillment.json` 内声明）—— 月度每项目的应收/已收/未收，是财务问句的常见入口

### 真实业务问句样本（来自现网盘点）

- `本月各项目应收多少？`
- `2025 年 3 月哪些项目还没收款完？`
- `客户欠款前 10 是谁？`
- `张三那笔报销审批到哪一步？`
- `上月预支单还有多少没核销？`
- `万象城那个项目 2025 年回款进度是多少？`
- `2025 年 Q1 总开票金额是多少？`
- `待付款审批里有没有超过 30 天的？`

> 这些问句目前在 Copilot 上**部分落到正确视图、部分被路由到 `v_monthly_settlement`、部分进入 AGENT_WORKFLOW 探索模式**，行为不稳定。

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 状态 | 轨 | 说明 |
|----|---------|---------|--------|------|---|------|
| F1 | 财务域语义资产基线 | 4 | P0 | READY | 1 (copilot) | 盘点 + finance.json + 字段语义 + 字典 |
| F2 | 财务 NL2SQL 快路径与 planner 直达 | 3 | P0 | READY | 1 (copilot) | 模板 + routing rule + few-shots/guardrails |
| F3 | 财务域回归与验收 | 3 | P1 | READY | 1 (copilot) | IT 脚本 + 验收矩阵 + 真人联调 |
| F4 | dts-stack 治理层试点（花卉财务域） | 5 | P0 | READY | 2 (dts-stack) | dbt sources + 花卉 mart 模型 + Airflow DAG + OpenMetadata 集成 + datasource 切换 |

## 影响范围

### 轨 1：dts-copilot 内（智能层）

#### 仅新增

- `dts-copilot-ai/src/main/resources/semantic-packs/finance.json` —— 新增
- `dts-copilot-ai/src/main/resources/prompts/finance-few-shots.txt` —— 新增（可选）
- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__finance_query_templates.xml` —— 新增
- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__finance_routing_rules.xml` —— 新增
- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__finance_biz_enum.xml` —— 新增（**短期**；F4 完成后从 OpenMetadata Glossary 派生）

#### 字段语义补齐（短期）

- `copilot_analytics.analytics_table` / `analytics_field` —— UPDATE finance 行（**短期**；F4 完成后从 OpenMetadata Catalog API 派生，本 sprint 完成后 sprint-23 归档）

#### datasource 切换（F4 收尾）

- `copilot_ai.ai_data_source` —— 新增"花卉财务 mart" datasource（指向 dts-stack dbt 产出库）
- `dts-copilot-analytics/.../service/elt/` —— **冻结**（标记 `@Deprecated`，下个 sprint 删除）

#### 不动

- 所有 `authority.finance.*` 视图本体（sprint-21 完成）保留 30 天作为回退路径
- `0040_seed_finance_procurement_templates.xml`（已落地，不回退）
- `procurement.json` / `project-fulfillment.json`

### 轨 2：dts-stack 内（治理层）

#### 仅新增

- `services/dts-dbt/models/xycyl/sources/xycyl_ods_sources.yml` —— 花卉业务 ODS 源声明（`rs_cloud_flower` 库的 16 张关键源表）
- `services/dts-dbt/models/xycyl/stg/*.sql` —— stg 层（占位符收敛 + 类型转换）
- `services/dts-dbt/models/xycyl/dwd/*.sql` —— dwd 层（含状态码翻译为中文 dim_*_alias）
- `services/dts-dbt/models/xycyl/dws/*.sql` —— dws 层（项目 × 月汇总）
- `services/dts-dbt/models/xycyl/ads/*.sql` —— ads 层（10 张花卉财务 mart 视图）
- `services/dts-dbt/models/xycyl/xycyl_finance_schema.yml` —— 花卉财务域 dbt schema 测试 + 文档
- `services/dts-airflow/dags/dwh/dwh_xycyl_finance_dbt_manual.py` —— Airflow DAG（沿用现有模板，selector=`tag:xycyl-finance`）
- `services/dts-openmetadata/ingestion/xycyl_dbt.yml` —— OpenMetadata 拉取花卉 dbt manifest 配置（可选，复用 `dbt.yml` 加 service 区分也可）
- `worklog/v1.0.0/sprint-22-202605/assets/dts-stack-dbt-conventions.md` —— 花卉业务在 dts-stack 的命名 / tag / 分层规范

#### 不改

- dts-stack 现有 `fin_*` / `pm_*` / `dwh_dbt_dbt_manual.py` 等基金 / 项目管理业务的 dbt 资产（命名空间隔离，互不影响）
- dts-stack 的 docker-compose / OpenMetadata / Ranger / Trino 部署形态

## 完成标准

### 轨 1（dts-copilot 智能层）

- [ ] `finance.json` 语义包覆盖 8 张固定报表 + 月度结算 + 应收/欠款 / 待付/待收 / 报销/发票/预支 9 类问句
- [ ] 同义词字典覆盖：`应收 / 待收 / 欠款 / 未收`、`已收 / 已回款 / 实收`、`已付 / 实付 / 已支付`、`账期 / 月份 / 期间`、`核销 / 销账`、`审批 / 流转`
- [ ] `BizEnumDictionary` 增补 6 类财务枚举（短期；F4 完成后由 OpenMetadata Glossary 派生）
- [ ] 财务域 8+ 类典型问句进入 `TEMPLATE_FAST_PATH` 或 `DIRECT_RESPONSE`，不再走 AGENT_WORKFLOW 兜底
- [ ] `Nl2SqlRoutingRule` 中 `finance` 域权重 ≥ `procurement` / `project`，避免被错路由
- [ ] 远程 `ai.yuzhicloud.com` 上 8 张财务固定报表执行返回 `200`，且口径与旧系统页面一致
- [ ] `it/test_finance_query_regression.sh` 在远程库通过
- [ ] 至少 5 条典型问句在 ChatPanel 上有真人联调记录
- [ ] guardrails 沉淀：禁止跳过 `authority.finance.*` 直接拼 `f_settlement` / `a_month_accounting` / `a_collection_record`

### 轨 2（dts-stack 治理层）

- [ ] dbt 工作空间下 `models/xycyl/` 命名空间建立，与现有 `fin_*` / `pm_*` 无命名冲突
- [ ] sprint-21 落地的 10 张 mart 视图全部有对应 dbt 模型（ads 层 + 必要的 dwd/dws 中间层）
- [ ] dbt 模型全部带 `tags=['xycyl', 'xycyl-finance']`，可被 Airflow DAG `selector=tag:xycyl-finance` 命中
- [ ] dbt schema.yml 的字段 description / tests 覆盖 ads 层关键字段，文档可生成
- [ ] `dwh_xycyl_finance_dbt_manual.py` 可在 Airflow 触发并跑通 `dbt run --select tag:xycyl-finance`
- [ ] OpenMetadata ingestion 拉取花卉 dbt manifest 后，可在 OpenMetadata UI 看到模型血缘 + 词汇表
- [ ] dts-copilot 注册"花卉 mart" datasource，指向 dbt 产出库，并能直读 mart 表
- [ ] 同一 finance 问句在 dts-copilot 通过新 datasource 与旧 `authority.finance.*` 视图两侧返回结果一致（口径回归通过）

### 双轨衔接

- [ ] sprint-22 后期发布前确认：dts-copilot 镜像 + 静态资产（mart DDL 导出 + manifest 元数据导出）可在不连接 dts-stack 的环境独立部署
- [ ] sprint-23 计划制定：把 task / 库存域、采购域按相同双轨模板复制

## 与相邻 sprint 的关系

| Sprint | 关系 |
|---|---|
| sprint-19 | 财务结算汇总 / 预支 / 报销 / 发票四张固定报表数据面已落地，本 sprint 在其基础上做语义化 |
| sprint-20 | F6 采购域语义收口确立的工艺范式（authority SQL → semantic pack → query template → planner rule → IT 回归），本 sprint 完整复制到财务域 |
| sprint-21 | 剩余 4 张财务报表数据面收口完成是本 sprint 的硬前置 |
| sprint-23（计划） | 任务作业域语义包 + 任务作业域 dbt 试点，沿用本 sprint 双轨工艺 |
| sprint-24（计划） | 库存采购域语义包 + 采购域 dbt 试点（注意：dts-stack 已有 `pm_*` 命名空间，需明确隔离） |
| sprint-25（计划） | EltSyncService 正式删除（确认所有域 datasource 已切到 dts-stack 产出后） |

## 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| 财务字段在 `f_*` 与 `a_*` 两套表族中存在重名（如 `amount` / `status`） | LLM 容易选错表 | 全部走 `authority.finance.*`，semantic pack 不暴露底层表名 |
| 月度结算同时存在 `v_monthly_settlement` 与 `authority.finance.settlement_summary` 两个入口 | 路由冲突，结果不一致 | 在 finance.json 中把 `v_monthly_settlement` 标记为 project 域协同视图，避免重定义；在 routing rule 中按问句关键词区分 |
| 报销 / 预支 / 发票走 Flowable 流程，状态字段含审批节点 | 状态枚举翻译不全 | T04 字典任务覆盖；guardrails 中标注"审批中状态需读 `act_*` 不能从业务表猜" |
| 财务问句涉及金额，LLM 在 `SUM` 时容易丢精度 | 数据失真 | few-shots 强制 `ROUND(...,2)`；guardrails 显式列出 |
| 双轨产出物在 sprint 末出现"哪个是最新"的歧义 | 数据漂移 | F4 完成验收后，sprint-21 落地的 PG 物化视图改为"只读快照 / 30 天保留"，新数据全部从 dts-stack dbt 产出 |
| dts-stack 现有 `fin_*` 命名空间是基金管理业务，命名容易撞车 | dbt 编译错误 / OpenMetadata 标签污染 | 花卉业务一律加 `xycyl-` 前缀，schema 走单独的 `xycyl_finance` schema 而非 `public`，OpenMetadata service 名加 `xycyl_` 区分 |
| dts-copilot 独立部署场景需要 dts-stack 的 dbt 产物 | 部署门槛升高 | 提供 `bin/export-dbt-artifacts.sh`：从 dts-stack 编译产物导出 mart DDL + manifest 关键元数据，作为 dts-copilot release tarball 的一部分；外部部署只需启动 PG + 跑这份导出 SQL |
| dts-stack dbt 跑失败导致 dts-copilot 数据陈旧 | 业务数据滞后 | F4-T05 在 dts-copilot 健康检查中加入"datasource 最新数据时间"探针；超过阈值降级到 sprint-21 PG 物化视图 |

## 输出物清单

### 文档（本目录）

- `README.md`（本文件）
- `features/F1-财务域语义资产基线/`：4 个 Task 文档
- `features/F2-财务NL2SQL快路径与planner直达/`：3 个 Task 文档
- `features/F3-财务域回归与验收/`：3 个 Task 文档
- `features/F4-dts-stack治理层试点/`：5 个 Task 文档
- `it/README.md` —— sprint IT 入口
- `it/test_finance_query_regression.sh` —— 远程财务回归脚本
- `it/finance-query-regression.md` —— 财务回归基线
- `it/test_xycyl_dbt_consistency.sh` —— dts-stack 产出 vs sprint-21 PG 视图一致性脚本（F4 产出）
- `assets/finance-authority-catalog.md` —— 财务 authority 视图清单 + dbt 模型映射
- `assets/dts-stack-dbt-conventions.md` —— 花卉业务在 dts-stack 的命名 / tag / 分层规范

### 代码（dts-copilot 仓）

见上方"影响范围 → 轨 1"

### 代码（dts-stack 仓）

见上方"影响范围 → 轨 2"
