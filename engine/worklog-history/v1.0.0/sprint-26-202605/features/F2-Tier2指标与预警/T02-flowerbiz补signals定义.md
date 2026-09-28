# T02: flowerbiz.json 补 signals 定义

**优先级**: P1
**状态**: DONE
**依赖**: T01

## 目标

在 `signals` 节定义阈值预警规则，让系统能主动识别风险并给出建议（含可选的 linkedActions 指向 F3 动作）。

## 技术设计

示例信号：

```jsonc
{ "name": "坏账风险", "object": "项目", "severity": "high",
  "when": "坏账率 > 0.15 AND 连续欠费月数 >= 2",
  "advice": "建议发起坏账处理；可一键生成坏账处理单草稿",
  "linkedActions": ["创建坏账处理单"] },
{ "name": "欠费预警", "object": "客户", "severity": "medium",
  "when": "应收-回款 > 阈值 AND 账龄 > 60 天",
  "advice": "建议跟进回款" }
```

- `when` 条件可引用 T01 的 metrics 与跨对象字段（沿 links 求值）。
- 阈值先给保守默认，业务可调；阈值集中在 pack，不散落代码。

## 影响范围

- `flowerbiz.json` 的 `signals` 节。

## 验证

- [x] signals 引用的 metrics/字段均已定义。
- [x] linkedActions 名称已集中声明；是否存在于 actions 由 F3 完成后闭合校验。

## 完成标准

- [x] 至少覆盖"坏账风险""欠费预警"两类，带 severity/advice。

## 证据

- `it/test_flowerbiz_signals.sh`
- `it/evidence/20260530-local/flowerbiz-signals.md`
