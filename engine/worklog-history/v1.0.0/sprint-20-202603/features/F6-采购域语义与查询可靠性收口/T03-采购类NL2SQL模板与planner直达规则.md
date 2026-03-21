# T03: 采购类 NL2SQL 模板与 planner 直达规则

**优先级**: P0
**状态**: DONE
**依赖**: T01, T02

## 目标

把采购域高频问句从“模型临场拼 SQL”收口成模板优先、planner 可控的直达路径。

## 技术设计

- 为采购域新增至少三类模板：
  - 某月某产品采购明细
  - 按采购人统计采购金额
  - 采购明细 + 采购人 + 金额组合问法
- planner 优先命中 authority SQL / backed template
- 失败回退时也要保留 authority SQL 的表链约束，不允许自由漂移到错表
- 对 `采购人 / 发起人` 的自然语言变体做统一路由

## 影响范围

- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/`
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/ConversationPlannerService.java`
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/AssetBackedPlannerPolicy.java`
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/TemplateMatcherService.java`

## 验证

- [x] 采购域模板覆盖“某月 + 某产品 + 按采购人/金额统计”类问法
- [x] planner 命中采购域时不再落到 `t_purchase_info.title` 或 `i_pendulum_purchase*`

## 完成标准

- [x] 采购域典型问法具备模板快路径或 planner 直达规则
- [x] 采购类查询可靠性不再依赖模型自由发挥
