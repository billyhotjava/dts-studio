# T01: semantic-pack 口径护栏扩展

**优先级**: P1
**状态**: BLOCKED
**依赖**: F1

## 目标

把业务域地图 §3 的九条口径铁律写成 semantic-pack guardrails(自然语言约束 + 必要的结构化标注),覆盖 flowerbiz/finance 语义包。

## 技术设计

逐条落地为 guardrail:
1. biz_type 多表多义——JOIN 前确认所在表枚举。
2. 两条结算链(租摆 a_month_accounting 不含税含折扣 / 售赠坏 a_sale_account 含税)不可混 SUM;坏账 receivable 是损失。
3. 月对账三级金额(名义/应收折前/折后实收)+ 已回款,选列规则。
4. 销售摊入租摆(source_type=8)双重计数防护。
5. 合同租金回写覆盖 p_project_green.rent,历史读 p_project_green_sett。
6. 库存成本加权平均(非 FIFO);good_price_id(SKU)而非 good_name 关联。
7. JSON 列(biz_ids_json/draft_item_json)需展开,不能等值 JOIN。
8. varchar 金额(a_sale_account_rent_item.rent)聚合前 CAST。
9. 额外费用(t_flower_extra_cost) ≠ 费用报销(f_expense_account_info);SpEL 开关影响凭证。

- 复用 sprint-26 引入的 semantic-pack schema,必要时给 metrics/guardrails 增字段表达"口径标签"。

## 影响范围

- `dts-copilot-ai/src/main/resources/semantic-packs/flowerbiz.json` 及新建 finance pack(F4 共用)
- 向后兼容校验(沿用 sprint-26 schema 测试)

## 验证

- [ ] 九条 guardrail 全部入 pack,SemanticPack 加载/schema 测试绿
- [ ] guardrail 文案可被 planner 在澄清/护栏分支引用

## 完成标准

- [ ] §3 九条铁律在语义包中可机读、可被 planner 消费
