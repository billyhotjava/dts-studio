# BG-02: 语义对象/字段/关系模型基线

**状态**: READY
**依赖**: BG-01

## 目标

在 `dts-copilot-analytics` 现有元数据模型上补齐业务语义所需的最小结构，使业务对象、默认时间字段、关系提示可以被持久化和读取。

## 技术设计

- 复用现有：
  - `analytics_table`
  - `analytics_field`
  - `analytics_metric`
  - `analytics_synonym`
- 补充最小业务语义结构：
  - 业务对象标识
  - 默认时间字段
  - 默认状态字段
  - 关系提示 / join hint
- 保持与现有 metadata sync 兼容，不破坏当前数据库导入流程

## 影响范围

- `dts-copilot-analytics` domain / repository / service / liquibase
- 语义读取接口
- 召回服务输入结构

## 完成标准

- [ ] 现有 metadata sync 可继续运行
- [ ] 业务语义字段可读写
- [ ] 语义对象和字段提示可被召回服务消费
- [ ] 补充最小单元测试验证结构兼容性
