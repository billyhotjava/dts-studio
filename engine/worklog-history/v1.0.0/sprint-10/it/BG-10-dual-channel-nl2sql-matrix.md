# BG-10 验收矩阵

## 一、预制模板命中（准确率要求 100%）

| 编号 | 问句 | 期望模板 | 关键校验 |
|------|------|---------|---------|
| T-01 | XX项目目前有多少在摆绿植 | TPL-01 | 参数提取 project_name，查 v_project_green_current |
| T-02 | 当前在服项目一共多少个 | TPL-02 | 过滤 project_status_name='正常' |
| T-03 | 哪些合同快到期了 | TPL-03 | 默认 90 天，按 contract_end_date 排序 |
| T-04 | 本月加花次数排行 | TPL-06 | biz_type='加花'，按次数 DESC |
| T-05 | 有多少待审批的报花单 | TPL-08 | biz_status_name='审核中' |
| T-06 | XX项目上个月租金 | TPL-09 | 查 v_monthly_settlement，不查 v_project_green |
| T-07 | 上月未结算的项目 | TPL-10 | settlement_status_name='待结算' |
| T-08 | 进行中的任务有哪些 | TPL-13 | task_status_name='进行中' |
| T-09 | 养护人均负责多少摆位 | TPL-15 | 聚合 total_position_count |
| T-10 | 各结算方式的项目分布 | TPL-20 | GROUP BY settlement_type_name |

## 二、规则路由准确性（准确率要求 > 90%）

| 编号 | 问句 | 期望域 | 期望视图 |
|------|------|--------|---------|
| R-01 | 这个客户下面有几个项目 | project | v_project_overview |
| R-02 | 上周的换花记录 | flowerbiz | v_flower_biz_detail |
| R-03 | 库里还有多少绿植 | green | v_project_green_current |
| R-04 | 这个月收了多少款 | settlement | v_monthly_settlement |
| R-05 | 待办任务有哪些 | task | v_task_progress |
| R-06 | 张三的养护覆盖率怎么样 | curing | v_curing_coverage |
| R-07 | 新项目初摆进度 | pendulum | v_pendulum_progress |
| R-08 | XX项目的换花率 | flowerbiz + project | v_flower_biz_detail + v_project_overview |
| R-09 | 数据库现在什么情况 | 低置信度 | 触发澄清 |
| R-10 | 帮我做个统计 | 低置信度 | 触发澄清 |

## 三、NL2SQL 视图查询（准确率要求 > 70%）

| 编号 | 问句 | 期望 SQL 关键片段 | 校验点 |
|------|------|-------------------|--------|
| S-01 | 本月报花业务中加花换花减花各多少 | `GROUP BY biz_type_name` | 使用视图中文字段名 |
| S-02 | 上个月哪个项目换花最多 | `WHERE biz_type_name = '换花' ... ORDER BY ... DESC LIMIT 1` | 正确使用中文枚举值 |
| S-03 | 月租金超过 5 万的项目 | `FROM v_monthly_settlement WHERE total_rent > 50000` | 查结算视图，不查绿植 |
| S-04 | 各养护人负责的绿植数量 | `FROM v_project_green_current GROUP BY curing_user_name` | 使用正确视图 |
| S-05 | 节日摆项目有哪些 | `WHERE project_type_name = '节日摆'` | 使用中文翻译字段 |
| S-06 | 费用由公司承担的报花单 | `WHERE bear_cost_type_name = '公司'` | 枚举翻译正确 |
| S-07 | 紧急换花单有多少 | `WHERE is_urgent = '是' AND biz_type_name = '换花'` | 多条件组合 |
| S-08 | 室外摆位的绿植列表 | 需涉及 position.type 翻译 | 视图是否包含此字段 |

## 四、结算域隔离（准确率要求 > 85%）

| 编号 | 问句 | 期望行为 | 关键校验 |
|------|------|---------|---------|
| F-01 | XX项目上月租金 | 查 v_monthly_settlement | 不查 v_project_green_current.rent |
| F-02 | 各项目应收排名 | 查 v_monthly_settlement | 使用 total_rent 字段 |
| F-03 | 客户欠款情况 | 查 v_monthly_settlement | 使用 outstanding_amount |
| F-04 | 在摆绿植的月租合计 | 查 v_project_overview.total_rent | 标注为"参考值" |

## 五、权限与安全（全部通过）

| 编号 | 问句 | 期望行为 |
|------|------|---------|
| P-01 | SELECT * FROM p_project | 拦截：非视图表不允许查询 |
| P-02 | SELECT * FROM t_flower_biz_info | 拦截：非视图表不允许查询 |
| P-03 | 普通用户查 v_monthly_settlement | 拦截：需财务/管理者角色（如已实现） |
| P-04 | DROP TABLE v_project_overview | 拦截：SqlSandbox 禁止 DDL |
| P-05 | 语义模糊无法确定域 | 返回澄清问题，不执行 SQL |

## 六、枚举词典翻译（全部通过）

| 编号 | 场景 | 验证方式 |
|------|------|---------|
| E-01 | v_flower_biz_detail.biz_type_name | 值为"加花/换花/减花"等中文 |
| E-02 | v_flower_biz_detail.biz_status_name | 值为"草稿/审核中/已完成"等中文 |
| E-03 | v_project_overview.project_status_name | 值为"正常/停用" |
| E-04 | v_task_progress.task_type_name | 值为"销售类/内购类"等中文 |
| E-05 | v_pendulum_progress.pendulum_status_name | 值为"草稿/初摆中/已完成"等中文 |

## 七、交互体验（全部通过）

| 编号 | 场景 | 验证方式 |
|------|------|---------|
| U-01 | 新会话欢迎卡片 | 打开 Copilot 面板，显示欢迎语 + 4 组推荐问句 |
| U-02 | 推荐问句点击 | 点击推荐问句，自动填入并发送，返回正确结果 |
| U-03 | SQL 预览卡片 | AI 回复中的 SQL 以预览卡片展示，非纯文本 |
| U-04 | SQL 内联编辑 | 点击"编辑"，可修改 SQL 内容 |
| U-05 | SQL 内联执行 | 点击"执行查询"，结果在 Chat 内以表格展示 |
| U-06 | 执行失败引导 | SQL 执行失败时显示错误 + "编辑修正"按钮 |
| U-07 | 创建可视化跳转 | 点击"创建可视化"，跳转到 CardEditor 并预填 SQL |
| U-08 | 反馈点赞 | 点击 👍，按钮变为激活态 |
| U-09 | 反馈点踩 + 表单 | 点击 👎，弹出原因选择表单，提交后持久化 |
| U-10 | 反馈数据可查 | `ai_chat_feedback` 表可查到反馈记录，含 session/message/reason |

## 验收记录模板

```
日期：
环境：
数据源：
测试类别：[模板/路由/NL2SQL/结算/权限/枚举]
用户：
问句：
期望行为：
实际行为：
  - 匹配路径：[模板命中/规则路由/NL2SQL]
  - 路由域：
  - 目标视图：
  - 生成 SQL：
  - 执行结果：
是否通过：
备注：
```
