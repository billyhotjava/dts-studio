# Sprint-22 IT

本目录用于存放双轨产出物的回归脚本、验收矩阵、真人联调清单与运行记录：

- **轨 1**（dts-copilot 智能层）：flowerbiz.json + 路由 + 模板 / few-shots 的语义化回归
- **轨 2**（dts-stack 治理层）：dbt 产出 vs adminapi 实时一致性回归

## 自动化回归

### 轨 1：报花问句回归（dts-copilot 路径）

- 脚本：`it/test_flowerbiz_query_regression.sh`
- 覆盖：12 条报花典型问句的远程数据回归
- 运行方式：

```bash
FLOWER_DB_PASSWORD='***' \
bash worklog/v1.0.0/sprint-22-202605/it/test_flowerbiz_query_regression.sh
```

### 轨 2：dbt 产出 vs adminapi 实时一致性（F4 产出）

- 脚本：`it/test_xycyl_dbt_consistency.sh`
- 覆盖：5+ 条代表性 SQL 在两个 datasource（dbt 产出 / adminapi 实时）跑出结果做 diff
- 期望：行数 100% 一致，数量差 ≤ 1，金额差 < 0.01 元
- 运行方式：

```bash
COPILOT_API_KEY='cpk_xxx' \
DBT_DS_ID=99 SRC_DS_ID=1 \
bash worklog/v1.0.0/sprint-22-202605/it/test_xycyl_dbt_consistency.sh
```

## 验收矩阵

- 文档：`it/acceptance-matrix.md`
- 用途：对齐报花 NL2SQL + 字段语义 + 字典翻译 + 13 bizType 分组 + 销售/坏账分链 + 跨域路由 + Guardrails + 双轨衔接 9 维验收

## 回归基线

- 文档：`it/flowerbiz-query-regression.md`
- 用途：12 条报花典型问句的"问句 → 期望路由 → 期望视图 → SQL 关键特征 → 期望结果"基线

## 真人联调

- 清单：`features/F3-报花域回归与验收/T03-验收矩阵更新与真人联调.md`
- 运行记录：`it/flowerbiz-acceptance-runs.md`
- 5 类消费者（养护人 / 项目经理 / 业务经理 / 财务 / 老板）各自典型问句联调

## 输出要求

每次真人联调结束后，至少记录：

- 日期
- 环境（远程域名）
- 使用账号
- A/B/C/D/E 5 类消费者通过情况
- 失败时对应路径、截图、日志
- 结论：是否纳入 sprint-22 DONE
