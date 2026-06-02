# T01: 空白域建模 onboarding checklist

**优先级**: P2
**状态**: DONE
**依赖**: F4

## 目标

把财务切片跑通的步骤抽象成可复用清单,让后续库存/督导/薪资域照单执行,降低每域建模成本。

## 技术设计

清单覆盖(从本 sprint 实践提炼):
1. 源表盘点 + 口径陷阱识别(对照业务域地图 §3)
2. ODS 同步补全(去明文凭据,多 tenant)
3. dbt 5 层建模(stg→dwd→dws→ads,4 列金额标准,枚举展开)
4. 口径 guardrail 入语义包 + 回归用例
5. 语义对象/fewShots 落地 + planner 命中验证
6. 与 adminweb 内建报表/业务库对账(误差阈值)
7. IT 证据归档

- 形式:`assets/blank-domain-onboarding-checklist.md`,带"财务域填好的样例列"。

## 影响范围

- `assets/` checklist 文档

## 验证

- [x] 用库存域纸面演练一遍,确认每步可落地

## 完成标准

- [x] checklist 成文且经一次纸面演练验证可用

## 证据

- `assets/blank-domain-onboarding-checklist.md`
