# BG-05: Join Contract 与 Allowed Tables 编译

**状态**: READY
**依赖**: BG-03, BG-04

## 目标

把语义包编译成运行时可消费的受控上下文，限制模型在每个主题域里只使用允许的表集合和 join 路径。

## 技术设计

- 为每个主题域编译：
  - allowed tables
  - allowed joins
  - default time fields
  - default dimensions / filters
- 输出给两侧消费：
  - `ai` 侧 prompt/context compiler
  - `analytics` 侧 execution / permission bridge

## 原则

- 不允许模型自由拼接整库
- 不允许跨主题域随意 join
- 默认优先使用受控 join 路径和时间字段

## 完成标准

- [ ] 每个优先主题域都有一份编译后的表集合
- [ ] 非法 join 可在运行前被阻断
- [ ] 运行时上下文可直接注入 Copilot
- [ ] 为 BG-07、BG-08 提供统一输入
