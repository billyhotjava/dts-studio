# CS-05: Webapp 系统配置页面与导航入口

**状态**: READY
**依赖**: CS-04

## 目标

在 `dts-copilot-webapp` 增加管理员系统配置页面和侧边栏入口。

## 技术设计

- 新增路由 `/admin/settings/copilot`
- 在 admin 区增加“系统配置”入口
- 页面分为站点设置、LLM Provider、Copilot API Key 三块

## 完成标准

- [ ] 管理员可从侧边栏进入配置页
- [ ] 页面可以增删改查 Provider
- [ ] 页面可以创建/轮换/吊销 API Key

