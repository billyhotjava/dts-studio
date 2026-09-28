# Sprint-30: 业务域数据面补全地基（风险清除·血缘补全·口径固化·财务回款开票垂直切片）

**时间**: 2026-06
**前缀**: DF (Data Foundation)
**状态**: IN_PROGRESS
**目标**: 把 2026-06-01 的业务域全景勘探（`docs/business/xycyl-operational-domain-map.md`）落成可执行工程——先清安全红线、补 ODS 血缘断点、把口径铁律固化为机器可检护栏,再以「财务回款/开票链」走一个空白域的完整垂直切片建模,沉淀可复用范式。

## 背景

六域源码级勘探(147 表 / 184 controller)产出全景地图,暴露三类必须先处理的问题:
1. **安全红线**:dts-stack Airflow DAG JSON 曾明文硬编码生产 MySQL/PG 密码(5 个 tenant),代码侧已清除,生产侧仍需轮换。
2. **血缘断点**:`t_change_info`、`t_warehousing_info` 等被 mart 设计引用,却无 Airflow ODS 同步,下游无法构建。
3. **口径地雷**:`biz_type` 三套同名枚举、两条结算链不可混 SUM、月对账三级金额、销售摊入双重计数等(详见地图 §3),只能在治理层确定性编码一次。

dts-stack 覆盖矩阵显示:报花/客户已建,**库存·财务回款/开票·督导·薪资·在摆历史 全空白**。本 sprint 不贪多——先打地基 + 选口径最敏感、业务价值最高的**财务回款/开票链**做垂直切片,为后续库存/督导/薪资域复制范式。

## 设计依据

- `docs/business/xycyl-operational-domain-map.md`（业务域全景地图,本 sprint 的事实源）
- 沿用 sprint-22/25/26 的 dbt 5 层范式(ods→stg→dwd→dws→ads)与 sprint-26 F0/T03 口径回归基线风格

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 状态 | 说明 |
|----|---------|---------|--------|------|------|
| F1 | 安全红线清除与配置治理 | 3 | P0 | DONE | 已清除 Airflow Addax JSON 明文密码 |
| F2 | ODS 血缘断点补全 | 4 | P0 | DONE | 已补 change/warehousing + 财务源表 ODS |
| F3 | 口径铁律固化为护栏与回归 | 3 | P1 | DONE | §3 铁律→guardrails+回归网 |
| F4 | 财务回款/开票链垂直切片建模 | 5 | P1 | DONE | 月对账应收/折后/回款+开票+收款 mart+语义包 |
| F5 | 范式固化与 IT 证据 | 2 | P2 | DONE | 空白域 onboarding checklist + 证据包 |
| F6 | Trino 联邦查询网关与跨库 Join | 5 | P1 | DONE | 方案 B:恢复 dts-trino,真实跨 PG/MySQL Join |

## 依赖顺序

```
F1(安全) ──> F2(ODS血缘) ──┐
       └───> F3(口径护栏) ─┴──> F4(财务垂直切片) ──> F5(范式+IT)
                                                   └──> F6(Trino联邦Join)
```

## 本 sprint 不做

- 不一次性建模全部空白域(库存/督导/薪资/在摆历史顺延,本轮只产出可复用范式)。
- 不改 adminapi 业务逻辑;ODS 仅只读同步,口径治理在 dts-stack/dts-copilot 侧。
- 不做 agent 自动建模 authoring(那是独立 brainstorm 方向,本 sprint 先把人工范式跑通)。

## 完成标准

- [x] Airflow DAG 无明文密码,凭据迁移到 Variables/环境变量,全仓凭据扫描基线建立
- [x] mart/语义对象引用的源表均有 ODS 同步(血缘断点闭环,缺口清单清零)
- [x] §3 九条口径铁律全部写入 semantic-pack guardrails,并有机器可跑的口径回归网(绿)
- [x] 财务回款/开票链 ads mart 落地 + 语义包对象/fewShots/guardrails,与 adminweb 内建报表口径对账 SQL 成文
- [x] `it/README.md` 有真实可重跑证据(密码清除验证、ODS 同步、口径回归、财务对账)
- [x] 产出空白域建模 onboarding checklist,供库存/督导/薪资域复用
- [x] dts-trino 恢复,并通过 Trino 对 PG 数仓和 MySQL 业务库做真实跨库 Join

## 相邻 sprint 关系

- 输入:sprint-26 报花本体化、sprint-29 指标联邦、本轮业务域地图。
- 输出:财务域垂直切片范式 → 后续库存/督导/薪资域 sprint 复用;清理后的 ODS 配置 → 指标联邦真实指标的前置。
