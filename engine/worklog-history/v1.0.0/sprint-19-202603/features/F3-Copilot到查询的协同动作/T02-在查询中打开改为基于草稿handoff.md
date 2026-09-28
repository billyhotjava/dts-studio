# T02: 在查询中打开改为基于草稿 handoff

**优先级**: P0
**状态**: READY
**依赖**: T01

## 目标

将“在查询中打开”从 SQL URL 传参切换为草稿 ID handoff。

## 技术设计

- 先确保存在草稿
- 再跳转到查询编辑器
- 编辑器按草稿详情加载

## 影响范围

- Copilot action
- query route
- editor loader

## 验证

- [ ] handoff 流程测试

## 完成标准

- [ ] 查询页能稳定还原 Copilot 结果和来源
