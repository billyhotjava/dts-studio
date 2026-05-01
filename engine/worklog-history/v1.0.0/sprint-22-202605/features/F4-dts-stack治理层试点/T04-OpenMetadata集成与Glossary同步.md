# T04: OpenMetadata 集成 + Glossary 同步

**优先级**: P1
**状态**: READY
**依赖**: T02, T03

**仓**: `dts-stack`

## 目标

让 dts-stack 已加强的 OpenMetadata 拉取花卉 dbt manifest，自动建立花卉财务模型的目录、血缘、字段描述与 Glossary 词条。完成后这成为 sprint-23+ 的元数据 SOT，dts-copilot 启动时通过 OpenMetadata API 派生 finance.json 与字段元数据。

## 技术设计

### 1. 现有 OpenMetadata 集成形态

dts-stack 已有：

- `services/dts-openmetadata/ingestion/dbt.yml` —— 通用 dbt ingestion 配置
- `services/dts-openmetadata/ingestion/postgres.yml` —— PG 表 ingestion
- `services/dts-openmetadata/ingestion/run-dbt-ingestion.sh` —— 触发脚本
- `services/dts-openmetadata/ingestion/run-postgres-ingestion.sh`
- `dts-platform/.../service/openmetadata/` —— 2 个集成类（推断为 OpenMetadata 客户端封装）

### 2. xycyl_dbt.yml 新增

参照 `services/dts-openmetadata/ingestion/dbt.yml`，新建：

```yaml
# services/dts-openmetadata/ingestion/xycyl_dbt.yml
source:
  type: dbt
  serviceName: ${OPENMETADATA_DBT_SERVICE_XYCYL:-xycyl_finance_dbt}
  sourceConfig:
    config:
      type: DBT
      dbtConfigSource:
        dbtConfigType: local
        dbtManifestFilePath: /opt/dbt/target/manifest.json
        dbtRunResultsFilePath: /opt/dbt/target/run_results.json
        dbtCatalogFilePath: /opt/dbt/target/catalog.json
      dbtUpdateDescriptions: true     # 用 dbt schema.yml 中的 description 更新 OpenMetadata
      includeTags: true                # 同步 dbt tags 为 OpenMetadata tags
      dbtClassificationName: xycyl_dbt # 区分既有基金 / pm 业务
sink:
  type: metadata-rest
  config: {}
workflowConfig:
  loggerLevel: INFO
  openMetadataServerConfig:
    hostPort: ${OPENMETADATA_HOST_PORT}
    authProvider: openmetadata
    securityConfig:
      jwtToken: ${OPENMETADATA_AUTH_TOKEN}
```

### 3. 触发脚本

新建 `services/dts-openmetadata/ingestion/run-xycyl-dbt-ingestion.sh`，沿用 `run-dbt-ingestion.sh`，仅替换：

- 配置文件路径：`xycyl_dbt.yml`
- 服务名 env：`OPENMETADATA_DBT_SERVICE_XYCYL`

### 4. Glossary 同步

OpenMetadata 的 Glossary 是结构化词汇表，可作为 `BizEnumDictionary` 的长期替代。

#### 4.1 Glossary 设计

```
Glossary: 花卉财务术语 (xycyl_finance_glossary)
├─ Domain: 应收 (Accounts Receivable)
│  ├─ Term: 应收金额 / receivable_amount
│  │  ├─ Synonyms: 应收, 应收账款
│  │  └─ Linked Asset: xycyl_ads_finance_settlement_summary.receivable_amount
│  ├─ Term: 已收金额 / received_amount
│  │  └─ Synonyms: 已收, 已回款, 实收
│  ├─ Term: 待收金额 / outstanding_amount
│  │  └─ Synonyms: 待收, 欠款, 未收
│  └─ Term: 回款率 / collection_rate
├─ Domain: 应付 (Accounts Payable)
│  └─ Term: 付款金额 / payment_amount
├─ Domain: 状态码
│  ├─ Term: settlement.status 已结算
│  │  └─ Linked Value: 2
│  ├─ Term: settlement.status 已收款
│  │  └─ Linked Value: 3
│  └─ ... （6 类财务枚举的所有值）
└─ Domain: 时间口径
   ├─ Term: 账期 / account_period
   └─ Term: 截止日 / as_of_date
```

#### 4.2 Glossary 落地方式

OpenMetadata 提供两种方式：

- **API 写入**（推荐）：写一次性 Python 脚本 `tools/openmetadata-seed-xycyl-glossary.py`，调用 OpenMetadata SDK 批量创建 Glossary + Terms
- **YAML/JSON 导入**：OpenMetadata UI 的 Bulk Import

本 sprint 选 API 写入，把脚本提交到 `services/dts-openmetadata/seeds/xycyl_finance_glossary.py`，由 IT 验证脚本调用。

#### 4.3 与 dts-copilot 的对接

dts-copilot 的 `BizEnumService` / `SemanticPackService` 增加一个新的"长期模式"：

- 启动时调用 OpenMetadata `/api/v1/glossaryTerms?glossary=xycyl_finance_glossary` 拉取
- 缓存到内存（每小时刷新）
- 如果 OpenMetadata 不可达，降级到 BizEnumDictionary（短期方案）

> 此对接代码**不**在本 sprint 写。本 sprint 完成后开 sprint-23 的 Issue 跟踪：dts-copilot 接入 OpenMetadata Glossary。

### 5. 与 dts-platform 后端 governance 服务的关系

dts-platform 已有：

- `IndicatorService` —— 指标管理
- `DimensionService` —— 维度管理
- `SemanticTermService` —— 语义词条
- `ReferenceCodeService` —— 参考代码

它们与 OpenMetadata Glossary 是什么关系？两种可能：

1. **dts-platform 内部维护一份**，与 OpenMetadata 双向同步（大概率）
2. **dts-platform 直接调 OpenMetadata API**

实施时需先与 dts-platform 团队确认。无论哪种，OpenMetadata 都是 SOT，sprint-22 的工作量集中在 OpenMetadata 一侧即可。

### 6. 与 sprint-22 F1-T03 / T04 的衔接

| 资产 | 短期（sprint-22 上线时） | 长期（F4 完成后） |
|---|---|---|
| 字段 display label | `analytics_field` UPDATE（F1-T03） | OpenMetadata Catalog API 派生 |
| 字段 synonyms | `analytics_field` UPDATE（F1-T03） | OpenMetadata Glossary 派生 |
| 状态枚举字典 | `BizEnumDictionary` INSERT（F1-T04） | OpenMetadata Glossary（Domain: 状态码）派生 |
| 视图 description | `analytics_table` UPDATE（F1-T03） | OpenMetadata Table 元数据派生（来自 dbt schema.yml description） |

## 影响范围

- `services/dts-openmetadata/ingestion/xycyl_dbt.yml` —— 新增
- `services/dts-openmetadata/ingestion/run-xycyl-dbt-ingestion.sh` —— 新增
- `services/dts-openmetadata/seeds/xycyl_finance_glossary.py` —— 新增（一次性 seed）
- `services/dts-airflow/dags/dwh/openmetadata_xycyl_ingestion.py` —— 新增（如选 Airflow 调度 ingestion）

## 验证

- [ ] OpenMetadata UI 中能看到 `xycyl_finance_dbt` service 与 10 张 ads 模型
- [ ] 模型血缘清晰（ods → stg → dwd → dws → ads）
- [ ] dbt schema.yml 中的 description 同步到 OpenMetadata 字段描述
- [ ] Glossary `xycyl_finance_glossary` 包含 ≥ 4 个 Domain 与 ≥ 30 个 Term
- [ ] 通过 OpenMetadata API `GET /api/v1/glossaryTerms?glossary=xycyl_finance_glossary` 能拉到完整词条
- [ ] 拉取到的词条与 sprint-22 finance.json 内 synonyms 集合一致（可手动比对）

## 完成标准

- [ ] OpenMetadata 是 sprint-23+ 的元数据 SOT
- [ ] xycyl_finance_glossary 落地，至少覆盖 sprint-22 finance.json 的 synonyms / 6 类财务枚举
- [ ] sprint-23 Issue 已开：dts-copilot 接入 OpenMetadata Glossary 派生
