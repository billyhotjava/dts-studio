# CS-02: Analytics 聚合配置接口（站点设置 + Provider 代理）

**状态**: READY
**依赖**: CS-01, BA-03

## 目标

在 `dts-copilot-analytics` 提供管理员聚合接口，统一承接站点设置和 Provider 管理。

## 技术设计

- 新增 `/api/admin/copilot/settings/site`
- 新增 `/api/admin/copilot/providers`
- 所有接口要求 session 用户为 `superuser`
- Provider 相关操作由 analytics 服务端代理到 `dts-copilot-ai`

## 完成标准

- [ ] superuser 可读写站点设置
- [ ] superuser 可管理 Provider
- [ ] 代理失败时返回统一错误信息

