# T02: 首批业务对象目录与 Planner 命中

**优先级**: P0  
**状态**: DONE  
**依赖**: T01

## 目标

新增首批业务对象目录，先覆盖“采购管理 > 配送记录”，让用户问配送记录状态、类型、接收人、字段分布时，Agent 走业务对象问答路径。

## 技术设计

- 新增业务对象目录服务，声明：
  - 对象编码：`prs.procurement.delivery_record`
  - 页面路径：`采购管理 > 配送记录`
  - 数据面：`L0_BUSINESS_OBJECT_PROFILE`
  - 只读来源：ODS/业务对象字段画像
  - 关键字段：标题、状态、类型、起始、目的地、配送人、配送时间、接收人、接收时间
  - 常用问法：状态分布、类型分布、接收人统计、超时/未接收排查
- Planner 命中该对象后返回 `BUSINESS_INSIGHT`。
- Prompt 要求优先输出结构化表格和字段画像说明，禁止直接业务写操作。

## 影响范围

- `dts-copilot-ai/src/main/java/.../BusinessObjectCatalogService.java`
- `AssetBackedPlannerPolicy`
- 对应单元测试

## 验证

- [x] `AssetBackedPlannerPolicyTest` 覆盖“配送记录状态分布”。
- [x] 业务对象目录测试覆盖对象字段、页面路径、sourceRefs。

## 完成标准

- [x] Planner 输出 `reportCode=prs.procurement.delivery_record.profile`。
- [x] `dataSurface=L0_BUSINESS_OBJECT_PROFILE`。
- [x] Prompt 包含“采购管理 > 配送记录”“字段画像”“只读 ODS”。
