# 模板路由优先级根因验证

**日期**: 2026-06-03
**范围**: `AssetBackedPlannerPolicy` 高可信模板命中后的 domain/target 归属

## 根因

“看下2026年各个绿植的采购情况”这类问题同时包含：

- 采购意图：应命中 `TPL-34`，走 `mysql.rs_cloud_flower.t_purchase_price_item`。
- 绿植词面：泛化意图路由容易归到 `green/project`，目标变成 `public.xycyl_dwd_project_green_snapshot`。

旧逻辑在模板已命中后，仍优先采用泛化路由的 `domain/primaryView`。结果是 SQL 可能已经是正确的模板 SQL，但回答元数据、目标视图和后续自动预览入口会被错误业务域带偏。更早版本/弱路径下，Agent 还会继续进入业务对象/LLM 生成路径，从而出现未授权的 `PRODUCTION.FLOWER_BIZ.PRS_PROCUREMENT_DELIVERY_RECORD`。

## 修复

高可信预制模板命中时：

- `routedDomain` 使用模板 `domain`。
- `primaryTarget` 使用模板 `target_view`。
- 泛化意图路由只作为未命中模板时的补充。

## 验证

```bash
mvn -q -Dmaven.repo.local=/opt/prod/prs/source/.m2 \
  -pl dts-copilot-ai \
  -Dtest=AssetBackedPlannerPolicyTest,TemplateMatcherServiceTest,BusinessObjectCatalogServiceTest \
  test
```

结果：PASS。

运行容器热更新后调用：

```bash
POST /internal/agent/chat/send
message = 看下2026年各个绿植的采购情况
```

关键返回：

```json
{
  "templateCode": "TPL-34",
  "responseKind": "TEMPLATE_SQL",
  "routedDomain": "procurement",
  "targetView": "mysql.rs_cloud_flower.t_purchase_price_item"
}
```

进一步通过 `/api/dataset` 联邦入口执行模板 SQL，返回 5 行 Top 数据，前 3 行：

| 绿植 | 规格 | 采购明细行数 | 采购单数 | 采购数量 | 采购金额 |
| --- | --- | ---: | ---: | ---: | ---: |
| 绿萝 | 规格:1.5m | 368 | 102 | 478 | 33890.5100 |
| 蝴蝶兰 | 规格:0.5m | 178 | 89 | 1029 | 30166.5100 |
| 小绿萝 | 规格:0.3m | 807 | 126 | 5723 | 28258.9700 |

## 部署说明

`docker compose up -d --build copilot-ai` 当次受 Docker Hub `eclipse-temurin:21-jre-alpine` metadata TLS 超时影响未完成。为验证运行行为，已本地构建 jar 并热更新当前 `dts-copilot-ai` 容器 `/app/app.jar`，容器健康检查为 `UP`。
