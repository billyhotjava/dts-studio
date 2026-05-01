# T03: Airflow DAG 调度（xycyl-finance）

**优先级**: P0
**状态**: READY
**依赖**: T02

**仓**: `dts-stack`

## 目标

复用 dts-stack 现有的 dbt DAG 模板（`dwh_finance_dbt_manual.py`），新建 `dwh_xycyl_finance_dbt_manual.py`，让花卉财务 dbt 模型可以通过 Airflow 触发执行 `dbt run` / `dbt test` / `dbt docs generate`。

## 技术设计

### 1. DAG 模板复用策略

dts-stack 已有的 `dwh_finance_dbt_manual.py` / `dwh_project_management_dbt_manual.py` / `dwh_dbt_dbt_manual.py` 是完全相同的模板，只是 selector 默认值不同：

- `dwh_finance_dbt_manual.py` —— `selector="tag:finance"`
- `dwh_project_management_dbt_manual.py` —— `selector="tag:project_management"`
- `dwh_dbt_dbt_manual.py` —— `selector="tag:all"`（兜底）

新建 `dwh_xycyl_finance_dbt_manual.py`：

```python
selector = "tag:xycyl-finance"
dag_id = "dwh_xycyl_finance_dbt_manual"
tags = ["dbt", "transform", "dwh", "xycyl", "xycyl-finance"]
```

其余逻辑完全沿用现有模板（docker run dbt-image / 通过 dag_run.conf 传递 operation / target / threads / vars / macro_args）。

### 2. DAG 触发方式

#### 方式 A：手动触发（sprint-22 默认）

通过 dts-platform 的 `/api/etl/dbt/dags/trigger` 触发：

```bash
curl -X POST "${DTS_PLATFORM}/api/etl/dbt/dags/trigger" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "dagId": "dwh_xycyl_finance_dbt_manual",
    "conf": {
      "operation": "run",
      "models": "tag:xycyl-finance",
      "target": "prod",
      "threads": "4"
    }
  }'
```

#### 方式 B：定时调度（sprint-23 启用）

把 DAG 改为 `schedule_interval='0 2 * * *'`（每日凌晨 2 点跑），本 sprint 不启用。

### 3. dts-platform 同步钩子

参考现有 DAG，跑完后会回调 `${DTS_PLATFORM_BASE_URL}/api/etl/dbt/models/sync`，把本次 run_results.json 上报。这意味着 OpenMetadata ingestion（T04）可以在 dts-platform 看到最新跑批结果。

本 task 不需要改 dts-platform 任何代码，复用现有钩子。

### 4. 失败告警与重试

DAG 模板默认配置：

- `retries=1`
- `retry_delay=timedelta(minutes=5)`
- 失败时回调 dts-platform，dts-platform 写 `IndicatorObservabilityService` / `QualitySignalService` 的失败信号

本 sprint 不修改默认配置。运维侧需要确认：

- Airflow webserver 的告警渠道是否已配置（钉钉 / 飞书 / 邮件）
- dts-platform 的 `IssueTicketService` 是否启用了"自动转工单"

### 5. 与 OpenMetadata Ingestion 的关系

OpenMetadata ingestion（`services/dts-openmetadata/ingestion/dbt.yml`）是**独立的**调度任务，不在 dbt DAG 内。两者关系：

```
dbt DAG（每日）
  └─ dbt run → dbt test → dbt docs generate
                                  └─ 写 target/manifest.json + catalog.json + run_results.json

OpenMetadata Ingestion DAG（每日，dbt 之后）
  └─ run-dbt-ingestion.sh
        └─ 读 target/*.json → 推送到 OpenMetadata
```

T04 会建一个新的 OpenMetadata ingestion 配置 `xycyl_dbt.yml`，区分 service name `xycyl_finance_dbt` 与现有 `dts_finance_dbt`。

### 6. dbt-platform-sync 的资源映射

`dwh_finance_dbt_manual.py` 中的 `DTS_PLATFORM_SYNC_PATH=/api/etl/dbt/models/sync` 用来：

- 把 dbt run 结果上报给 dts-platform
- dts-platform 内部触发 `DbtRunResultService` / `DbtArtifactSyncScheduler` 同步

本 task 实施时确认：dts-platform 是否能识别 xycyl 命名空间（不需要白名单），还是需要先在 dts-platform 注册 xycyl service？

> 决策点：T03 实施开始前要先和 dts-platform 后端确认这件事，可能需要在 dts-platform 配置中加一条 service 声明（不在 sprint-22 范围，但要打开 issue 跟踪）。

## 影响范围

- `services/dts-airflow/dags/dwh/dwh_xycyl_finance_dbt_manual.py` —— 新增（基于 `dwh_finance_dbt_manual.py` 复制 + 修改 3 行）
- 不改 dts-platform 后端代码（除非"决策点"需要）

## 验证

- [ ] DAG 在 Airflow UI 可见，状态为 unpaused
- [ ] 手动触发 `operation=run` 在 dev target 跑通，全部 xycyl-finance 模型 success
- [ ] 手动触发 `operation=test` 跑通，警告数 ≤ T02 的预期
- [ ] 手动触发 `operation=docs-generate` 生成 `target/index.html`
- [ ] dts-platform 在跑批后能在 `/etl/dbt/models` 看到 xycyl 模型清单（如果 service 注册问题已解决）
- [ ] Airflow run history 中失败的 task 有重试记录

## 完成标准

- [ ] DAG 文件提交并通过 Airflow lint
- [ ] 至少 3 次成功运行（dev / staging / prod target 各 1 次）
- [ ] 接 T04 OpenMetadata ingestion 后 manifest 可被消费
