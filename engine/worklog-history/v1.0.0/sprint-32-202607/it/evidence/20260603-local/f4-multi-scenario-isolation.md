# F4/T02 多场景共存隔离验证

**时间**: 2026-06-03
**环境**: 本地 `dts-copilot` / `dts-stack`
**入口**:
- AI: `http://127.0.0.1:50091/internal/agent/chat/send`
- Analytics: `http://127.0.0.1:50092/api/dataset`
- 联邦入口: database_id = 9 (`trino`)

## 变更

- 新增 `semantic-packs/warehouse.json`，让 Agent runtime 装载仓库库存口径护栏。
- 新增 `TPL-53`：库存现量按库房和 SKU 汇总，目标表 `mysql.rs_cloud_flower.s_stock_info`。
- 新增 `TPL-54`：低库存 SKU 清单，目标表 `mysql.rs_cloud_flower.s_stock_info`。
- 保留旧 `WH-STOCK-OVERVIEW` / `WH-LOW-STOCK-ALERT` 非激活状态，不把未发布资产库占位当成可打开资产。

## 运行证据

重跑命令：

```bash
bash worklog/v1.0.0/sprint-32-202607/it/test_f4_multi_scenario_isolation.sh
```

输出：

```json
{"question":"看下2026年各个绿植的采购情况","templateCode":"TPL-34","domain":"procurement","target":"mysql.rs_cloud_flower.t_purchase_price_item","rowCount":100,"firstRow":["绿萝","规格:1.5m",368,102,478,33890.5100,"2026-01-03T00:00:00Z","2026-05-29T00:00:00Z"]}
{"question":"查询2026年销售情况","templateCode":"TPL-52","domain":"flowerbiz","target":"public.xycyl_ads_flowerbiz_sale_summary","rowCount":139,"firstRow":["2026-01","朔黄大厦",6900.00,1.00,0.00,0.00]}
{"question":"展示2026年库存现状","templateCode":"TPL-53","domain":"warehouse","target":"mysql.rs_cloud_flower.s_stock_info","rowCount":100,"firstRow":["大兴基地","1564500285738790913","垃圾袋","120X140cm","描述:厚（50个/包）#规格:120X140cm","包",9944,"6463.6",1,"2026-05-28T20:51:01Z"]}
{"question":"低库存预警","templateCode":"TPL-54","domain":"warehouse","target":"mysql.rs_cloud_flower.s_stock_info","rowCount":100,"firstRow":["大兴基地","1508722749876826113","山水盆","3#","规格:3#","个",-1,"0.0","2023-07-26T10:44:04Z"]}
```

## 结论

- 采购问题命中 `procurement/TPL-34`，未生成历史错误中的 `PRODUCTION.FLOWER_BIZ` 虚构 catalog。
- 销售问题命中 `flowerbiz/TPL-52`，目标保持 dbt ADS 逻辑表名，执行期由联邦 SQL qualifier 补全为 Trino 可执行路径。
- 库存问题命中 `warehouse/TPL-53/TPL-54`，不再提示去不存在的资产库资产。
- 低库存结果包含负库存样例，说明库存源数据存在异常库存，后续 ADS 建模需把负库存纳入质量告警。

## 边界

- 本轮验证的是 `warehouse` 语义包 + 可执行模板 + Trino 联邦弱路径。
- 库存 `inventory_ads_*` dbt 模型仍未落地，因此 F4/T01 仍保持 `IN_PROGRESS`。
