# MD-01 主数据盘点与归属审计

**优先级**: P0  
**状态**: DONE

## 目标

完成 `adminapi / adminweb / app / rs_cloud_flower` 四方对照，明确主数据候选对象、当前归属模块和归属冲突。

## 范围

- 项目轴：客户、合同、项目点、摆位
- 物品轴：物品、分类、属性、规格、价格、别名
- 共享参考：库房、供应商、人员组织、字典

## 审计结论

### 一、项目轴主数据已经相对成型

后端归属集中在 `project/*`：

- `/project/customer`
- `/project/contract`
- `/project/project`
- `/project/position`
- `/project/floorLayer`
- `/project/floorNumber`
- `/project/role`
- `/project/goodAlias`

对应控制器见：

- [CustomerController.java](/opt/prod/prs/source/adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/project/controller/CustomerController.java)
- [ContractController.java](/opt/prod/prs/source/adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/project/controller/ContractController.java)
- [ProjectController.java](/opt/prod/prs/source/adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/project/controller/ProjectController.java)
- [PositionController.java](/opt/prod/prs/source/adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/project/controller/PositionController.java)

PC 页面归属也相对一致：

- `project/customer/*`
- `project/contract/*`
- `project/project/*`

业务库对应表族也清晰：

- `p_customer`
- `p_customer_user`
- `p_contract`
- `p_contract_dept`
- `p_project`
- `p_floor_layer`
- `p_floor_number`
- `p_position`
- `p_project_role`
- `p_project_good_alias`

### 二、物品轴主数据基本成型，但价格与别名已经向业务侧外溢

后端主归属在 `base/*`：

- `/base/goods`
- `/base/goodsClassify`
- `/base/goodsAttribute`
- `/base/goodsPrice`

对应控制器见：

- [GoodsController.java](/opt/prod/prs/source/adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/base/controller/GoodsController.java)
- [GoodsClassifyController.java](/opt/prod/prs/source/adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/base/controller/GoodsClassifyController.java)
- [GoodsAttributeController.java](/opt/prod/prs/source/adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/base/controller/GoodsAttributeController.java)
- [GoodsPriceController.java](/opt/prod/prs/source/adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/base/controller/GoodsPriceController.java)

数据库表族：

- `b_goods`
- `b_goods_classify`
- `b_goods_attribute`
- `b_goods_attribute_item`
- `b_goods_price`
- `b_goods_price_attribute_item`
- `b_goods_price_record`

但项目侧已经存在 `p_project_good_alias`，说明物品语义开始向项目局部语义漂移，需要纳入主数据治理，而不是让项目页继续各自解释。

### 三、库房是明确主数据，库存不是

后端：

- 库房：`/store/house`
- 库存结果面：`/store/info`

对应控制器：

- [StorehouseInfoController.java](/opt/prod/prs/source/adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/storehouse/controller/StorehouseInfoController.java)
- [StockInfoController.java](/opt/prod/prs/source/adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/storehouse/controller/StockInfoController.java)

数据库：

- `s_storehouse_info`
- `s_stock_info`
- `s_stock_item`

关键判断：

- `s_storehouse_info` 属于参考主数据
- `s_stock_info / s_stock_item` 属于库存结果面或流水派生面，不应反向作为主数据源

### 四、供应商存在明显缺口

前端能看到供应商相关页面和概念：

- [list-supply.vue](/opt/prod/prs/source/adminweb/src/views/flower/project/contract/list-supply.vue)
- [addOrUpdate-supply.vue](/opt/prod/prs/source/adminweb/src/views/flower/project/contract/addOrUpdate-supply.vue)

但后端没有看到独立的 `SupplierController` 或独立供应商主数据模块。  
这说明供应商当前更像“挂在合同/业务页面里的信息”，不是清晰的一等主数据。

### 五、移动端证明主数据消费面已经跨端

`app` 侧已存在大量选择器和轻量查询页：

- `components/flower/select-project.vue`
- `components/flower/select-customer.vue`
- `components/flower/select-goods.vue`
- `components/flower/select-store-house.vue`
- `pages/project/list-project.vue`
- `api/flowers/project.js`
- `api/flowers/good.js`

这说明主数据治理不能只面向 PC 端，要同时考虑移动端执行链对项目、物品、库房的依赖。

## 归属冲突清单

1. 物品别名落在项目侧，而不是主数据侧统一治理  
2. 供应商没有清晰独立归属  
3. 库存结果面具备“新增/修改数量/修改价格”能力，容易侵入物品与库存主语义  
4. 合同租金、项目绿植、结算配置等配置型事实靠近项目主数据，容易被误当成项目主数据自身

## 输出结论

- 项目轴和物品轴已经具备成为 canonical master data 的基础
- 供应商、字典、状态码和结算参考数据需要独立提升
- 库存、采购、账单、凭证必须明确降级为交易/派生事实消费面
