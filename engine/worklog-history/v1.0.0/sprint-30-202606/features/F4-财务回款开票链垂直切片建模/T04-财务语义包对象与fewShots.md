# T04: 财务语义包对象 + fewShots + guardrails

**优先级**: P1
**状态**: DONE
**依赖**: T01,T02,T03

## 目标

为财务回款/开票链建 semantic-pack 对象,让 NL2SQL 能命中权威 ads,并把口径护栏(两链不混、含税vs不含税、三级金额、回款率定义)写入。

## 技术设计

- 新建/扩展 finance 语义包,对象映射到 T01-T03 的 ads:
  - 月度结算应收回款(`xycyl_ads_finance_month_settlement`)
  - 开票进度(`xycyl_ads_finance_invoice_progress`)
  - 收款明细(`xycyl_ads_finance_collection`)
- synonyms:应收/实收/折后/回款率/开票率/账单率/账龄等业务词。
- fewShots:典型问句→SQL(覆盖三级金额选列、两链分别统计、含税口径)。
- guardrails:复用 F3/T01 的九条,补财务专项(回款率分母用折后实收;坏账不计收入)。

## 影响范围

- `dts-copilot-ai/src/main/resources/semantic-packs/`(finance pack)
- SemanticPackService 注册;schema/加载测试

## 验证

- [x] 语义包加载、schema 测试绿
- [x] fewShots 问句命中对应 ads(口径回归网断言)

## 完成标准

- [x] 财务语义对象可被 NL2SQL 命中,口径护栏齐备

## 证据

- `dts-copilot-ai/src/main/resources/semantic-packs/finance.json`
- `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/SemanticPackCaliberGuardrailTest.java`
