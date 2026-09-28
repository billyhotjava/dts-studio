# T05: dts-copilot datasource 切换 + 独立部署导出

**优先级**: P0
**状态**: READY
**依赖**: T02, T03

**仓**: `dts-copilot` + `dts-stack`（双仓变更）

## 目标

完成本 sprint 双轨衔接最后一公里：

1. **dts-copilot 内**：注册"花卉 mart" datasource 指向 dbt 产出库，完成 sprint-22 内的 query template / routing rule 切换；冻结（不删除）`EltSyncService` 框架
2. **dts-stack 内**：提供 `bin/export-dbt-artifacts.sh`，把 dbt 编译产物（mart DDL + manifest 元数据 + glossary 导出）打包成 dts-copilot 独立部署可消费的 tarball

## 技术设计

### 1. dts-copilot datasource 注册

在 `copilot_ai.ai_data_source` 表中新增一行：

```sql
INSERT INTO copilot_ai.ai_data_source (
    name, db_type, jdbc_url, username, password_secret_ref, description, status, tag
) VALUES (
    '花卉报花 Mart (dbt)',
    'postgres',
    'jdbc:postgresql://dbt-output-host:5432/xycyl_warehouse',
    'copilot_reader',
    'secret:dbt-output-readonly',
    '由 dts-stack dbt 项目 xycyl 命名空间产出，每日刷新，schema = xycyl_ads',
    'ACTIVE',
    'flowerbiz,xycyl,dbt-output'
);
```

#### 1.1 多种部署形态

| 部署形态 | 数据源 jdbc_url | 备注 |
|---|---|---|
| 内部部署（馨懿诚） | dts-stack 同库的 dbt 产出 schema | dts-copilot 直读 PG |
| 通过 Trino 联邦查询 | `jdbc:trino://dts-trino:8080/...` | 跨库联邦（如 mart 在 PG，源系统在 MySQL） |
| 独立部署（外部客户） | dts-copilot 自己的 PG，跑 sprint-22 导出脚本 | T05 第二部分提供导出 |

#### 1.2 query template 内的 view 名称映射

F2-T01 的 query template 默认走 dts-stack dbt 产出（直接读 `xycyl_ads.*`）。如需兜底走 adminapi 实时查询，模板按 datasource tag 切换：

| 模板默认 view（dbt 产出） | 兜底 view（adminapi 实时） |
|---|---|
| `xycyl_ads.xycyl_ads_flowerbiz_lease_summary` | （动态拼 `t_flower_biz_info` + `t_flower_biz_item` JOIN）|
| `xycyl_ads.xycyl_ads_flowerbiz_lease_detail` | 同上 |
| `xycyl_ads.xycyl_ads_flowerbiz_pending` | 同上，按 status 过滤 |
| `xycyl_ads.xycyl_ads_flowerbiz_sale_summary` | 走 adminapi `flower/sale` 接口数据 |
| `xycyl_ads.xycyl_ads_flowerbiz_baddebt_summary` | `t_flower_biz_info WHERE biz_type=6` |
| ... | ... |

> sprint-21 财务视图（`authority.finance.*`）**不**作为本 sprint 的兜底 —— 它们是财务衍生层，与本 sprint 报花域不直接对应。sprint-25 财务域上线时再处理 view 切换映射。

**做法**：

- F2-T01 的 12 条模板**保留旧视图名**作为兜底
- 新增一个 changelog `v1_0_0_0NN+1__finance_query_templates_dbt_routing.xml`：
  - 在 `nl2sql_query_template` 加一列 `target_view`（如已有，用 UPDATE）
  - 当 datasource = "花卉财务 Mart (dbt)" 时，模板自动映射到 `xycyl_ads_*` 名
  - 当 datasource = sprint-21 PG 视图时，仍用 `authority.finance.*` 名
- `Nl2SqlService` 在路由时根据当前 datasource 选择目标视图（**这要求小改 Java**，标记为 sprint-22 的 1 处代码改动）

> 决策点：是否接受这 1 处 Java 改动？如不接受，则 F4-T05 退化为"datasource 切换由人工触发"，新旧视图名不能共存。

### 2. EltSyncService 冻结（不删除）

- 修改 `dts-copilot-analytics/.../service/elt/EltSyncService.java`：在 `runAll()` 与 `runSync()` 入口加 `@Deprecated` 与 `LOG.warn("ELT framework deprecated. Use dts-stack dbt instead.")`
- 修改 `application.yml`：默认 `dts.elt.enabled=false`（之前是 `true`）
- 保留 `EltWatermarkService` 与 `ProjectFulfillmentSyncJob` / `FieldOperationSyncJob` 代码不动
- 在 `worklog/v1.0.0/sprint-25` 计划"正式删除"

> 决策点：sprint-22 上线时 `dts.elt.enabled` 默认 false，是不是会让 sprint-19 ~ sprint-21 的旧物化视图停止刷新？需要确认这两件事是不是同一件事——`EltSyncService` 跑的是 mart_project_fulfillment_daily / mart_field_operations，不一定是 sprint-21 的 authority.finance.*。如果 authority.finance.* 是 Liquibase 静态视图（不是 EltSyncJob 产出），冻结 EltSync 对 sprint-22 上线没影响。

### 3. dts-stack 独立部署导出脚本

新增 `dts-stack/bin/export-dbt-artifacts.sh`：

```bash
#!/usr/bin/env bash
# Usage: bin/export-dbt-artifacts.sh <namespace> <out_dir>
# Example: bin/export-dbt-artifacts.sh xycyl-flowerbiz ./dist/xycyl-flowerbiz
#
# 输出物：
#   ./dist/xycyl-flowerbiz/
#     ├── ddl/
#     │   ├── 01_schemas.sql               # CREATE SCHEMA xycyl_ods, xycyl_stg, ... 
#     │   ├── 10_dim.sql                   # 维度表 + seeds
#     │   ├── 20_stg.sql                   # stg 视图（编译后）
#     │   ├── 30_dwd.sql                   # dwd 视图
#     │   ├── 40_dws.sql                   # dws 表
#     │   └── 50_ads.sql                   # ads 表（mart）
#     ├── seeds/                           # 静态 seed（如状态字典）
#     ├── manifest-extract.json            # dbt manifest 关键元数据抽取（不含敏感信息）
#     ├── glossary.json                    # OpenMetadata Glossary 导出
#     └── README.md                        # 独立部署说明

set -euo pipefail

NAMESPACE="${1:-xycyl-flowerbiz}"
OUT_DIR="${2:-./dist/${NAMESPACE}}"

# 1. dbt compile 把模型展开为最终 SQL
dbt compile --select tag:${NAMESPACE} --profiles-dir ./profiles

# 2. 从 target/manifest.json 抽取 nodes，按 layer 排序输出 DDL
python3 tools/dbt-extract-ddl.py \
  --manifest target/manifest.json \
  --tag ${NAMESPACE} \
  --out ${OUT_DIR}/ddl/

# 3. 抽取 manifest 关键元数据（description / tests / refs / tags）
python3 tools/dbt-extract-manifest.py \
  --manifest target/manifest.json \
  --tag ${NAMESPACE} \
  --out ${OUT_DIR}/manifest-extract.json

# 4. 从 OpenMetadata 导出 Glossary
python3 tools/openmetadata-export-glossary.py \
  --glossary xycyl_flowerbiz_glossary \
  --out ${OUT_DIR}/glossary.json

echo "Export complete: ${OUT_DIR}"
```

#### 3.1 配套工具脚本

- `tools/dbt-extract-ddl.py` —— 从 manifest 抽取 compiled SQL，按层级输出
- `tools/dbt-extract-manifest.py` —— 抽取元数据（不含 jdbc_url 等敏感信息）
- `tools/openmetadata-export-glossary.py` —— 调用 OpenMetadata API 导出 Glossary

这些工具 sprint-22 必须给出可工作的版本（哪怕粗糙）。

### 4. dts-copilot 独立部署消费这些产物

dts-copilot 的发布流程在 sprint-23 增加：

- 拉取 `dts-stack/dist/xycyl-flowerbiz/` 作为 release artifacts
- 打入 dts-copilot Docker 镜像 `/opt/copilot/static-assets/`
- 容器启动时检查目标 datasource 中是否已有 mart 表
  - 若无：执行 `ddl/*.sql` 建表 + 提示用户跑 ETL（用户自行）
  - 若有：跳过
- 加载 `manifest-extract.json` / `glossary.json` 作为元数据兜底（OpenMetadata 不可达时使用）

> sprint-22 不实现这部分，只产出可消费的 tarball。sprint-23 dts-copilot 侧消费。

### 5. 一致性回归

sprint-22 末必须验证：同一组报花问句通过 dbt 产出 vs adminapi 实时查询的结果一致。

新建 `worklog/v1.0.0/sprint-22-202605/it/test_xycyl_dbt_consistency.sh`：

```bash
#!/usr/bin/env bash
# 在 dbt 产出（xycyl_ads.*）与 adminapi 实时查询（rs_cloud_flower）之间做一致性回归
# 期望：两侧行数 100% 一致，数量差 ≤ 1，金额差 < 0.01 元

set -euo pipefail

DBT_DATASOURCE_ID="${DBT_DS_ID:-99}"   # dbt 产出（PG）
SOURCE_DATASOURCE_ID="${SRC_DS_ID:-1}" # adminapi 业务库（MySQL，rs_cloud_flower）
COPILOT_API="${COPILOT_API:-http://localhost:8091/api/ai}"
API_KEY="${COPILOT_API_KEY:-}"

run_query() {
  local ds_id="$1"
  local sql="$2"
  curl -s -X POST "${COPILOT_API}/copilot/execute" \
    -H "Authorization: Bearer ${API_KEY}" \
    -H "Content-Type: application/json" \
    -d "{\"datasourceId\": ${ds_id}, \"sql\": \"${sql}\"}"
}

# === R1: 本月加摆汇总 vs 业务库现场聚合 ===
echo "== R1: lease_in (this month) =="
dbt_r1=$(run_query "$DBT_DATASOURCE_ID" "SELECT ROUND(SUM(\"加摆金额\"),2) FROM xycyl_ads.xycyl_ads_flowerbiz_lease_summary WHERE \"业务月份\" = DATE_FORMAT(CURDATE(),'%Y-%m')")
src_r1=$(run_query "$SOURCE_DATASOURCE_ID" "SELECT ROUND(SUM(i.rent * i.plant_number),2) FROM t_flower_biz_info f JOIN t_flower_biz_item i ON i.flower_biz_id=f.id WHERE f.biz_type=2 AND f.status=5 AND f.del_flag='0' AND i.del_flag='0' AND DATE_FORMAT(i.start_time,'%Y-%m') = DATE_FORMAT(CURDATE(),'%Y-%m')")
echo "dbt=${dbt_r1} | src=${src_r1}"

# === R2: 销售汇总 ===
# === R3: 坏账金额 ===
# === R4: 待结算单数 ===
# === R5: 养护人工作量 Top 5 ===
# 略，按相同 pattern
```

**断言策略**：

- 数值断言用容差（金额差 < 0.01 元，行数差 = 0，数量差 ≤ 1）
- adminapi 实时查询用最简单的 JOIN，避免业务复杂逻辑
- 失败时输出 `dbt=X | src=Y` 让排查更容易

## 影响范围

### dts-copilot 仓

- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__xycyl_dbt_datasource.xml` —— 新增
- `dts-copilot-ai/src/main/java/.../service/copilot/Nl2SqlService.java` —— **小改**（按 datasource 选目标视图，约 30 行）
- `dts-copilot-analytics/src/main/java/.../service/elt/EltSyncService.java` —— 加 `@Deprecated`
- `dts-copilot-analytics/src/main/resources/application.yml` —— `dts.elt.enabled` 默认 false
- `worklog/v1.0.0/sprint-22-202605/it/test_xycyl_dbt_consistency.sh` —— 新增

### dts-stack 仓

- `bin/export-dbt-artifacts.sh` —— 新增
- `tools/dbt-extract-ddl.py` —— 新增
- `tools/dbt-extract-manifest.py` —— 新增
- `tools/openmetadata-export-glossary.py` —— 新增

## 验证

- [ ] dts-copilot ChatPanel 切换 datasource 到"花卉报花 Mart (dbt)"，输入"本月加摆撤摆净增减"，返回结果与 adminweb 现网页面一致（行数 / 数量 ±1 / 金额差 < 0.01）
- [ ] `test_xycyl_dbt_consistency.sh` 通过
- [ ] `bin/export-dbt-artifacts.sh xycyl-flowerbiz ./dist/xycyl-flowerbiz` 产出完整 tarball
- [ ] tarball 中 `ddl/50_ads.sql` 可在新 PG 实例跑通
- [ ] `EltSyncService` 默认禁用，启动日志有 deprecated 提示
- [ ] dts-copilot 主流程不依赖 dts-stack 在线（只在拉取最新 manifest 时联线）

## 完成标准

- [ ] datasource 切换链路打通
- [ ] 一致性回归通过
- [ ] 独立部署导出可工作
- [ ] sprint-22 整体 DONE 后，dts-copilot 用户在切换 datasource 时无感
