# T04: adminapi/adminweb 四大重点域对象目录扩展

**优先级**: P0  
**状态**: DONE  
**依赖**: T02

## 目标

根据 adminapi 和 adminweb 的真实业务结构扩展业务对象目录，避免 Sprint-24 的业务对象问答器只覆盖采购配送记录。

重点覆盖：

- 报花：报花单据、换花/调花、摆位/实摆绿植。
- 采购：采购计划、采购汇总/明细、配送记录。
- 项目：项目点、合同。
- 财务/运营：结算、报销/预支/支出、银行流水、凭证、开票/收款。
- 仓库：库存、入库、出库、调拨、报损、退货。

## 技术设计

- 以 `adminapi/rs-modules/rs-flowers-base` 的 controller 包和 `adminweb/src/views/flower` 页面作为业务证据。
- 将页面路径、只读接口、关键字段、常见问法和质量提示写入业务对象目录。
- Planner 在报表目录前优先检查业务对象目录，但业务对象关键词保持具体，避免抢走“项目经营 TOP”“租赁收入趋势”这类经营报表。
- 所有对象统一输出：
  - `responseKind=BUSINESS_INSIGHT`
  - `dataSurface=L0_BUSINESS_OBJECT_PROFILE`
  - `qualityLevel=MEDIUM`
  - `sourceRefs` 包含业务对象、页面或只读接口证据。

## 影响范围

- `dts-copilot-ai/src/main/java/.../BusinessObjectCatalogService.java`
- `BusinessObjectCatalogServiceTest`
- `AssetBackedPlannerPolicyTest`
- `worklog/v1.0.0/sprint-24-202605/assets/adminapi-adminweb-business-object-map.md`

## 验证

- [x] `BusinessObjectCatalogServiceTest` 覆盖报花、采购、项目、财务、仓库域。
- [x] `AssetBackedPlannerPolicyTest` 覆盖报花单据、项目点、银行流水命中业务对象路径。
- [x] 保留“租赁收入 TOP”不命中业务对象，避免影响经营报表路径。

## 完成标准

- [x] 首批对象目录不再局限采购，覆盖四个重点域及仓库。
- [x] Planner 对“报花单据状态分布”“项目点状态统计”“银行流水未核对有多少”返回 `BUSINESS_INSIGHT`。
- [x] 业务对象回答包含页面路径、关键字段、只读来源和质量提示。
