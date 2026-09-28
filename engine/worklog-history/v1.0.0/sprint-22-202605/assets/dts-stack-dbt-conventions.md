# 馨懿诚业务在 dts-stack dbt 项目的命名 / tag / 分层规范

> 本文件是 sprint-22 F4 的输入。任何花卉业务（馨懿诚绿植租摆，xycyl）在 dts-stack `services/dts-dbt/` 下落地的 dbt 资产都必须遵守本规范。

## 一、命名空间与隔离

dts-stack 现有业务命名空间：

| 业务 | 命名空间前缀 | 状态 |
|---|---|---|
| 基金管理 | `fin_*`（`stg_fin_*` / `dim_fund_*` / `biz_dwd_*_fund` / `biz_ads_*_fund_kpi`） | 已生产 |
| 项目管理（科研项目） | `pm_*` / `pm_*_v2` | 已生产 |
| 跨业务通用维度 | `dim_boolean_alias` / `dim_node_type_*` 等 | 已生产 |
| **馨懿诚绿植租摆** | `xycyl_*`（本规范） | sprint-22 引入 |

### 强制规则

1. **所有花卉业务 dbt 资产名以 `xycyl_` 开头**
2. **所有花卉业务 source name 以 `xycyl_` 开头**（如 `xycyl_ods` / `xycyl_dim_seeds`）
3. **所有花卉业务物化目标 schema 以 `xycyl_` 开头**（`xycyl_ods` / `xycyl_stg` / `xycyl_dwd` / `xycyl_dws` / `xycyl_ads`）
4. **不复用** dts-stack 已有的 `dim_*_alias` 维度表 —— 如需类似维度，命名为 `xycyl_dim_*_alias`
5. **不直接 `ref('某基金或 PM 业务模型')`** —— 跨命名空间引用需走 source 显式声明

## 二、模型命名约定

### 完整命名结构

```
{namespace}_{layer}_{domain}_{topic}[_{variant}]
```

- `namespace` —— `xycyl`
- `layer` —— `stg` / `dwd` / `dws` / `ads` / `dim`
- `domain` —— `finance` / `procurement` / `task` / `inventory` / `green` / `project`
- `topic` —— 具体主题，如 `settlement` / `customer_ar_rank`
- `variant` —— 可选，如 `daily` / `monthly` / `snapshot`

### 示例

| 模型名 | 含义 |
|---|---|
| `xycyl_stg_settlement_info` | stg 层，结算单原始表 |
| `xycyl_dwd_finance_settlement` | dwd 层，财务结算明细（含状态翻译） |
| `xycyl_dwd_finance_settlement_status_alias` | dwd 层，结算状态码翻译维度 |
| `xycyl_dws_finance_project_monthly` | dws 层，项目 × 月汇总 |
| `xycyl_ads_finance_settlement_summary` | ads 层，结算总览 mart |
| `xycyl_ads_finance_customer_ar_rank_daily` | ads 层，客户欠款排行（日快照） |

### 反例

| 错误命名 | 原因 |
|---|---|
| `settlement_summary` | 缺命名空间、缺 layer、缺 domain |
| `xycyl_settlement_summary` | 缺 layer 与 domain |
| `mart_xycyl_finance_settlement_summary` | layer 应在 namespace 后 |
| `XYCYL_ADS_FIN_SETTLE` | 应小写 + 全词 + 下划线分隔 |

## 三、Tag 规范

### 必须包含的 tags

每个花卉业务 dbt 模型必须包含以下 tags：

```sql
{{ config(tags=['xycyl', 'xycyl-{domain}', '{topic}']) }}
```

| Tag | 用途 |
|---|---|
| `xycyl` | 业务命名空间，区分基金 / PM 业务 |
| `xycyl-finance` / `xycyl-procurement` / ... | 域级，Airflow DAG selector 命中粒度 |
| `settlement` / `ar` / `payment` / ... | 主题级，按需运行某子集 |

### Layer 自动 tag

不需要手写 layer tag —— `dbt_project.yml` 中按目录自动分配（参考现有 fin/pm 业务）：

```yaml
models:
  dts:
    +materialized: table
    xycyl:
      stg:
        +tags: ['stg', 'xycyl-stg']
      dwd:
        +tags: ['dwd', 'xycyl-dwd']
      dws:
        +tags: ['dws', 'xycyl-dws']
      ads:
        +tags: ['ads', 'xycyl-ads']
        +materialized: table   # ads 层强制物化
```

### Tag 使用样例

| Airflow conf | 命中模型 |
|---|---|
| `models: tag:xycyl-finance` | 花卉财务全部模型 |
| `models: tag:xycyl-finance,tag:ads` | 花卉财务 ads 层 |
| `models: tag:xycyl-finance,tag:settlement` | 花卉财务结算主题 |
| `models: +tag:xycyl-ads-finance-settlement-summary` | 仅刷某 mart 模型及其上游 |

## 四、分层职责

| 层 | 职责 | 不允许做的事 |
|---|---|---|
| `xycyl_ods` | source 声明，identifier 指向真实 ODS 镜像表（`rs_cloud_flower` 入湖结果） | 不做 transformation |
| `xycyl_stg` | 重命名 + 类型转换 + 占位符收敛 + 软删过滤（`del_flag = '0'`） | 不做业务派生、不 JOIN 多表 |
| `xycyl_dwd` | 业务键拼接、状态码翻译为中文、数据质量过滤、字段中文化 | 不做汇总聚合 |
| `xycyl_dim` | 维度表，含状态码 → 中文 alias、地区维度、组织维度等 | 不依赖 stg 层 |
| `xycyl_dws` | 跨业务对象汇总（项目 × 月、客户 × 月、人 × 月） | 不直接面向 mart 报表 |
| `xycyl_ads` | mart 层，sprint-21 各 authority/mart 视图的对应物，字段中文化、KPI 派生 | 不允许重新做 stg/dwd 已经做的脏活 |

## 五、tests 规范

每个 ads 模型必须在 schema.yml 中定义至少：

- 主键字段 `not_null` + `unique`（粒度键）
- 状态字段 `accepted_values`（值域）
- 金额字段 `expression_is_true: ">= 0"`（severity: warn）
- 时间字段 `not_null`

参考 dts-stack 已有 `fin_schema.yml` / `pm_schema_v2.yml` 的 tests 写法，不重新发明。

## 六、文档规范

每个 ads 模型必须在 schema.yml 提供：

- 模型级 description（中文，含粒度说明，如"客户 × 日的欠款快照"）
- 字段级 description（中文，含口径说明，如"应收金额，单位元，DECIMAL(14,2)，已 ROUND(2)"）

这些 description 经 OpenMetadata ingestion 后会自动同步到 OpenMetadata UI，并被 dts-copilot 通过 OpenMetadata API 派生消费。

## 七、与 OpenMetadata 的对接

| 资源 | 对应 OpenMetadata 实体 |
|---|---|
| `xycyl_ads_*` 模型 | OpenMetadata Table（service: `xycyl_finance_dbt`） |
| schema.yml description | OpenMetadata Table / Column description |
| dbt tags | OpenMetadata Tags（classification: `xycyl_dbt`） |
| `dim_*_alias` 中的状态码 → 中文 | OpenMetadata Glossary Domain "状态码" |
| 同义词（finance.json synonyms 内容） | OpenMetadata Glossary Term synonyms |

## 八、Airflow DAG 约定

| 业务域 | DAG 文件 | selector |
|---|---|---|
| 花卉财务 | `dwh_xycyl_finance_dbt_manual.py` | `tag:xycyl-finance` |
| 花卉采购（sprint-24） | `dwh_xycyl_procurement_dbt_manual.py` | `tag:xycyl-procurement` |
| 花卉任务（sprint-23） | `dwh_xycyl_task_dbt_manual.py` | `tag:xycyl-task` |
| 花卉全量 | `dwh_xycyl_dbt_manual.py` | `tag:xycyl` |

## 九、独立部署导出约定

dts-stack `bin/export-dbt-artifacts.sh xycyl-finance` 输出物：

```
dist/xycyl-finance/
├── ddl/
│   ├── 01_schemas.sql       # CREATE SCHEMA
│   ├── 10_dim.sql           # 维度表 + seeds
│   ├── 20_stg.sql           # stg 视图
│   ├── 30_dwd.sql           # dwd 视图
│   ├── 40_dws.sql           # dws 表
│   └── 50_ads.sql           # ads mart
├── seeds/                   # 状态字典 CSV
├── manifest-extract.json    # 关键元数据（不含敏感）
├── glossary.json            # OpenMetadata Glossary 导出
└── README.md                # 独立部署说明
```

dts-copilot 独立部署场景消费此 tarball 实现"无 dts-stack 也能用"。

## 十、版本与回退

- xycyl 命名空间不再走 `_v1` / `_v2` 后缀（dts-stack 现有 `pm_*_v2` 是历史遗留）
- 重大改动通过 dbt snapshot + Liquibase 兼容层处理，不重命名模型
- 任何破坏性变更需先在 dts-copilot semantic-pack 标记 `deprecated: true`，下个 sprint 才能真正修改 dbt 模型
