# T02: Sprint-30 IT 证据包

**优先级**: P2
**状态**: BLOCKED
**依赖**: F1-F4

## 目标

汇总 Sprint-30 全部集成验证证据,确保标 DONE 前 `it/README.md` 矩阵每项都是真实可重跑证据。

## 技术设计

证据覆盖:
- F1:已暴露默认密码字面量零命中 + 凭据扫描基线输出
- F2:ODS 同步抽样行数/合计一致 + 覆盖对照表缺口为零
- F3:口径回归网绿 + biz_type 字典
- F4:财务 ads 产数、dbt test 绿、与 adminweb 报表对账误差达标
- 归档到 `it/evidence/<日期>-local/`

## 影响范围

- `it/README.md` 证据矩阵;`it/evidence/`

## 验证

- [ ] 每个 IT 项有命令输出/对账结果链接,可独立重跑

## 完成标准

- [ ] IT 证据矩阵全绿,无空占位,Sprint-30 可据此收口
