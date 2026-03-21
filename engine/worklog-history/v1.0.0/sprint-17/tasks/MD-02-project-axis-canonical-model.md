# MD-02 项目轴 Canonical Model

**优先级**: P0  
**状态**: DONE

## 目标

定义项目轴主数据的标准对象树、业务键、展示名和上下级关系。

## 范围

- 客户
- 合同
- 项目点
- 楼层 / 楼号 / 摆位
- 项目角色

## Canonical Model

### 对象树

1. `Customer`
2. `Contract`
3. `ContractDept`
4. `Project`
5. `FloorLayer / FloorNumber`
6. `Position`
7. `ProjectRole`

推荐关系：

`Customer -> Contract -> ContractDept -> Project -> Floor/Position`

### 稳定业务键建议

- `customer.code`
- `contract.code`
- `project.code`
- `position.code`

如果现网缺少稳定编码，应先补“展示编码 + 内部主键”的双键策略，不建议继续只依赖数据库自增 ID 对外识别。

### 展示名建议

- 客户：简称 + 全称
- 合同：合同标题 + 合同编号
- 项目点：项目点名称 + 项目点编号
- 摆位：楼层/楼号 + 摆位名称

### 不应纳入项目主数据本体的对象

- `p_project_green`
- `p_project_green_item`
- `p_contract_rentfee`
- `p_project_settlement_config`
- `p_project_settlement_record`

这些对象应归为配置型事实或交易事实。

## 关键挂接点

项目轴是后续这些域的统一锚点：

- 报花业务
- 任务执行
- 采购计划
- 配送与入库
- 月账单 / 对账 / 开票

因此项目点主数据需要保证：

1. 业务状态唯一  
2. 项目负责人、业务经理、监管人等角色口径统一  
3. 合同与结算配置可回溯，但不反向污染项目本体
