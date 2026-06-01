# Sprint-30 集成测试（IT）

本目录存放"数据面补全地基"的验收证据。**所有证据必须真实可重跑,不接受空占位**(沿用 sprint-25/26/29 风格)。

## 证据结构

```
it/
  README.md                  # 本文件,证据索引
  sql/                       # 口径陷阱问句集、对账 SQL、覆盖校验脚本
  evidence/<日期>-local/     # 每次本地验证的结果快照
```

## 证据矩阵

| Feature | 验收点 | 证据位置 | 状态 |
|---------|--------|---------|------|
| F1 | Airflow DAG 无明文密码 | `grep Devops123@` 零命中 + evidence | 待产出 |
| F1 | 凭据扫描基线 | 扫描脚本 + evidence | 待产出 |
| F2 | t_change_info ODS 同步 | DAG + 抽样行数 | 待产出 |
| F2 | 仓储出入库 ODS | DAG + 抽样行数 | 待产出 |
| F2 | 财务源表 ODS | DAG + 金额合计一致 | 待产出 |
| F2 | ODS 覆盖对照表缺口为零 | 覆盖校验脚本 + 对照表 | 待产出 |
| F3 | §3 九条 guardrail 入 pack | SemanticPack schema 测试绿 | 待产出 |
| F3 | 口径回归网 | JUnit/脚本 + tsv | 待产出 |
| F3 | biz_type 枚举字典 | assets 字典文档 | 待产出 |
| F4 | 月对账应收/折后/回款 ads | dbt test + 抽样对账 | 待产出 |
| F4 | 开票进度 ads | dbt test + 抽样对账 | 待产出 |
| F4 | 收款明细 ads | 交叉校验误差 | 待产出 |
| F4 | 财务语义对象 NL2SQL 命中 | 口径回归网断言 | 待产出 |
| F4 | 与 adminweb 内建报表对账 | 对账 SQL + 误差 <0.5% | 待产出 |
| F5 | 空白域 onboarding checklist | assets checklist + 纸面演练 | 待产出 |
| F5 | Sprint-30 IT 证据包 | 本矩阵全绿 | 待产出 |

## 重跑约定

每个 `test_*.sh` 可独立执行,结果写入 `evidence/<日期>-local/`。标 DONE 前本矩阵所有"待产出"必须替换为真实证据链接。
