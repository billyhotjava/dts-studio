# T02: 草稿服务与 REST API

**优先级**: P0
**状态**: READY
**依赖**: T01

## 目标

提供草稿的创建、列表、详情、归档等资产管理接口。

## 技术设计

- 新增 service / resource
- 提供 `POST/GET/DETAIL/ARCHIVE/DELETE`
- 默认按当前用户隔离

## 影响范围

- analytics service
- analytics web/rest

## 验证

- [ ] Resource 单测
- [ ] 草稿列表过滤测试

## 完成标准

- [ ] 草稿可创建和读取
- [ ] 草稿不会混入正式 query 资源
