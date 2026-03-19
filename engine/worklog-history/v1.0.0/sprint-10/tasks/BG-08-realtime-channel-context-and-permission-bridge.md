# BG-08: 直连通道上下文编译与权限桥接

**状态**: READY
**依赖**: BG-05, BG-07

## 目标

让直连通道不再是“AI 直接 JDBC 查库”，而是受编译后的业务上下文、允许表集合、join contract 和 `analytics` 权限控制共同约束。

## 技术设计

- 直连通道仅允许：
  - 明细列表
  - 简单聚合
  - 单域排行
- 上下文编译产物包含：
  - 业务对象
  - allowed tables
  - allowed joins
  - 时间字段提示
  - 同义词和 few-shot
- 查询权限必须从 `analytics` 侧桥接到 Copilot 执行链
- 逐步把“最终执行权”从 `ai` 裸 JDBC 工具收回到 `analytics`

## 完成标准

- [ ] Copilot 直连查询受 allowed tables 限制
- [ ] Copilot 直连查询受 join contract 限制
- [ ] Copilot 直连查询受 analytics 权限控制
- [ ] 非授权或越域查询会被明确拒绝
