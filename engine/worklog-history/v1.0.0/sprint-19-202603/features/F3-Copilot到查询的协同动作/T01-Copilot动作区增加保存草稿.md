# T01: Copilot 动作区增加保存草稿

**优先级**: P0
**状态**: READY
**依赖**: F1/T02

## 目标

为 Copilot SQL/报表型回答增加“保存草稿”动作。

## 技术设计

- 仅对结构化分析结果展示
- 保存 question / sql / explanation / datasource / session context

## 影响范围

- Copilot UI
- analytics draft create API

## 验证

- [ ] 前端动作测试
- [ ] 后端 create API 集成测试

## 完成标准

- [ ] 用户不离开对话也能沉淀结果
