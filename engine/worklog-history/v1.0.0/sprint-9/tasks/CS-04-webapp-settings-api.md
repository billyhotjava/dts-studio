# CS-04: Webapp 配置 API 客户端

**状态**: READY
**依赖**: CS-02, CS-03, FE-03

## 目标

为前端配置页补充类型化 API 客户端，统一调用 analytics 聚合接口。

## 技术设计

- 在 `analyticsApi.ts` 中新增 site settings、provider、api key 管理方法
- 统一处理错误返回和一次性 raw key 展示数据

## 完成标准

- [ ] 前端可通过统一 API 层调用所有配置接口
- [ ] 类型定义覆盖 Provider 和 API Key 响应

