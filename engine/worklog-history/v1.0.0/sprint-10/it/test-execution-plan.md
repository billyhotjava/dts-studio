# Sprint-10 测试执行计划

> 文档版本: 1.0
> 适用范围: BG-10 园林业务语义层与 NL2SQL 落地
> 对应验收矩阵: `BG-10-dual-channel-nl2sql-matrix.md`

---

## Phase 1: 代码级单元测试（不依赖数据库/外部服务）

运行方式: `mvn test` 即可，无需启动任何外部服务。

---

### 1.1 IntentRouterService 路由测试

- **测试类**: `IntentRouterServiceTest.java`
- **测试方法**: JUnit 5 + Mockito，mock `Nl2SqlRoutingRuleRepository`
- **Mock 数据**: 与 `v1_0_0_009__nl2sql_routing.xml` 种子数据一致的 7 条路由规则

| 用例编号 | 测试场景 | 输入问句 | 期望结果 |
|---------|---------|---------|---------|
| R-01 | 客户项目查询 | "这个客户下面有几个项目" | domain=project, primaryView=v_project_overview |
| R-02 | 换花业务查询 | "上周的换花记录" | domain=flowerbiz, primaryView=v_flower_biz_detail |
| R-03 | 绿植库存查询 | "库里还有多少绿植" | domain=green, primaryView=v_project_green_current |
| R-04 | 收款查询 | "这个月收了多少款" | domain=settlement, primaryView=v_monthly_settlement |
| R-05 | 待办任务查询 | "待办任务有哪些" | domain=task, primaryView=v_task_progress |
| R-06 | 养护覆盖率 | "张三的养护覆盖率怎么样" | domain=curing, primaryView=v_curing_coverage |
| R-07 | 初摆进度 | "新项目初摆进度" | domain=pendulum, primaryView=v_pendulum_progress |
| R-08 | 跨域问题 | "XX项目的换花率" | 主域+secondaryViews 包含另一个域视图 |
| R-09 | 低置信度(无意义) | "数据库现在什么情况" | needsClarification=true |
| R-10 | 低置信度(模糊) | "帮我做个统计" | needsClarification=true |
| R-11 | 结算隔离 | "上月租金收入排名" | domain=settlement, primaryView=v_monthly_settlement, secondaryViews 为空 |
| R-12 | 空输入 | "" | needsClarification=true |
| R-13 | null 输入 | null | needsClarification=true |

---

### 1.2 TemplateMatcherService 模板匹配测试

- **测试类**: `TemplateMatcherServiceTest.java`
- **测试方法**: JUnit 5 + Mockito，mock `Nl2SqlQueryTemplateRepository`
- **Mock 数据**: 与 `v1_0_0_010__nl2sql_query_template.xml` 种子数据一致的关键模板

| 用例编号 | 测试场景 | 输入问句 | 期望结果 |
|---------|---------|---------|---------|
| T-01 | 项目绿植数量 (TPL-01) | "翠湖项目目前有多少在摆绿植" | matched=true, templateCode=TPL-01, project_name=翠湖 |
| T-02 | 在服项目总数 (TPL-02) | "当前在服项目一共多少个" | matched=true, templateCode=TPL-02 |
| T-03 | 加花排行 (TPL-06) | "各项目加花次数排行" | matched=true, templateCode=TPL-06 |
| T-04 | 待审批报花 (TPL-08) | "有多少待审批的报花单" | matched=true, templateCode=TPL-08 |
| T-05 | 项目月租金 (TPL-09) | "万科项目上个月租金" | matched=true, templateCode=TPL-09, month=上月YYYY-MM |
| T-06 | 未结算项目 (TPL-10) | "上月未结算的项目" | matched=true, templateCode=TPL-10, month=上月YYYY-MM |
| T-07 | 进行中任务 (TPL-13) | "进行中的任务有哪些" | matched=true, templateCode=TPL-13 |
| T-08 | 结算方式分布 (TPL-20) | "各结算方式的项目分布" | matched=true, templateCode=TPL-20 |
| T-09 | 无匹配降级 | "今天天气怎么样" | matched=false |
| T-10 | 空输入 | "" | matched=false |
| T-11 | 推荐问句 | getSuggestedQuestions(8) | 非空列表, 覆盖多个域 |
| T-12 | 时间参数(本月) | "这个月的换花次数" → month 提取 | month=当前YYYY-MM |
| T-13 | 时间参数(上月) | "上月未结算" → month 提取 | month=上月YYYY-MM |

---

### 1.3 BizEnumService 枚举服务测试

- **测试类**: 待补充（依赖枚举数据是否可 mock）
- 测试要点:
  - `formatEnumsAsContext()` 返回非空字符串，包含 "加花"/"换花" 等中文标签
  - `getLabel(code)` 对已知枚举码返回正确中文

---

### 1.4 CopilotQueryService 权限验证测试

- **测试类**: `CopilotQueryServiceTest.java`
- **测试方法**: JUnit 5 + Mockito，mock `DatasetQueryService`

| 用例编号 | 测试场景 | 输入 SQL | 期望结果 |
|---------|---------|---------|---------|
| P-01 | 白名单视图通过 | `SELECT project_name, count(*) FROM v_project_overview GROUP BY project_name` | 通过 |
| P-02 | 原始表拦截 | `SELECT * FROM p_project WHERE status = 1` | 抛出 ForbiddenQueryException |
| P-03 | 原始表拦截 | `SELECT * FROM t_flower_biz_info` | 抛出 ForbiddenQueryException |
| P-04 | DDL 拦截 | `DROP TABLE v_project_overview` | 不包含 FROM/JOIN，通过 validateSqlSources 但应由其他层拦截 |
| P-05 | 子查询别名不误拦截 | `SELECT * FROM v_project_overview po JOIN v_flower_biz_detail fb ON po.project_name = fb.project_name` | 通过（po、fb 为短别名） |
| P-06 | 多视图 JOIN 通过 | `SELECT ... FROM v_project_overview JOIN v_monthly_settlement ON ...` | 通过 |
| P-07 | JOIN 含非法表 | `SELECT ... FROM v_project_overview JOIN p_project ON ...` | 抛出 ForbiddenQueryException |
| P-08 | 空 SQL | `""` | 抛出 ForbiddenQueryException |
| P-09 | null SQL | null | 抛出 ForbiddenQueryException |
| P-10 | 结算视图财务限制检测 | `SELECT * FROM v_monthly_settlement` | isFinanceRestricted=true |
| P-11 | 非结算视图 | `SELECT * FROM v_task_progress` | isFinanceRestricted=false |

---

### 1.5 SemanticPackService 语义包加载测试

- **测试类**: 待补充
- 测试要点:
  - JSON 语义包文件加载成功，不抛异常
  - `getFewShots(domain)` 返回非空列表
  - `getContextForDomain(domain)` 返回包含视图 DDL 的上下文字符串

---

## Phase 2: API 级集成测试（需要运行的服务）

### 2.1 前置条件

| 服务/组件 | 端口 | 说明 |
|----------|------|------|
| copilot-ai | 8080 | AI 服务 (NL2SQL + 路由 + 模板) |
| copilot-analytics | 8081 | 分析服务 (SQL 执行 + 权限) |
| PostgreSQL | 5432 | Copilot 元数据库 (Liquibase 已执行) |
| MySQL (业务库) | 3306 | 园林业务数据 (视图已创建) |

检查清单:
- [ ] `copilot-ai` 健康检查: `GET /actuator/health` 返回 `UP`
- [ ] `copilot-analytics` 健康检查: `GET /actuator/health` 返回 `UP`
- [ ] 种子数据已加载: `nl2sql_routing_rule` 表有 7 条记录
- [ ] 种子数据已加载: `nl2sql_query_template` 表有 20 条记录
- [ ] 业务视图可查: `SELECT 1 FROM v_project_overview LIMIT 1` 不报错

---

### 2.2 API 测试清单

| 编号 | 接口 | 方法 | 测试内容 | 期望 |
|------|------|------|---------|------|
| API-01 | `/api/ai/nl2sql/suggestions` | GET | 返回推荐问句列表 | 200, 非空数组, 每项含 question 字段 |
| API-02 | `/api/ai/nl2sql/feedback` | POST | 提交 thumbs_up 反馈 | 200, 数据库 `ai_chat_feedback` 可查 |
| API-03 | `/api/ai/nl2sql/feedback` | POST | 提交 thumbs_down + 原因 | 200, reason 字段已持久化 |
| API-04 | `/api/analytics/copilot/execute` | POST | 合法视图 SQL | 200, 返回列头 + 数据行 |
| API-05 | `/api/analytics/copilot/execute` | POST | 非法表 SQL: `SELECT * FROM p_project` | 403, 返回错误信息 |
| API-06 | `/api/analytics/copilot/execute` | POST | DDL: `DROP TABLE v_project_overview` | 403 |
| API-07 | `/api/ai/agent/chat/send` | POST | 发送 "当前在服项目一共多少个" | 200, 回复含 SQL, SQL 引用 v_project_overview |
| API-08 | `/api/ai/agent/chat/send` | POST | 发送 "本月加花次数排行" | 200, 回复含 SQL, SQL 引用 v_flower_biz_detail |

示例 cURL (API-04):
```bash
curl -X POST http://localhost:8081/api/analytics/copilot/execute \
  -H 'Content-Type: application/json' \
  -d '{
    "sql": "SELECT project_status_name, count(*) as cnt FROM v_project_overview GROUP BY project_status_name",
    "datasourceId": 1
  }'
```

---

### 2.3 端到端 Copilot 对话测试

按以下步骤验证完整链路:

**场景 E2E-01: 模板命中路径**
1. 发送 "当前在服项目一共多少个"
2. 验证: 回复包含 SQL
3. 验证: SQL 中包含 `v_project_overview` 和 `project_status_name = '正常'`
4. 验证: 结果包含项目数统计

**场景 E2E-02: NL2SQL 路径**
1. 发送 "本月报花业务中加花换花减花各多少"
2. 验证: 回复包含 SQL
3. 验证: SQL 中包含 `GROUP BY biz_type_name`
4. 验证: SQL 引用 `v_flower_biz_detail`

**场景 E2E-03: 结算隔离**
1. 发送 "各项目应收排名"
2. 验证: SQL 引用 `v_monthly_settlement`
3. 验证: SQL 不引用 `v_project_green_current`

**场景 E2E-04: 澄清触发**
1. 发送 "帮我做个统计"
2. 验证: 回复为澄清提示，不直接生成 SQL

---

## Phase 3: 前端交互测试（手工/Playwright）

### 3.1 WelcomeCard

| 编号 | 场景 | 操作 | 期望结果 |
|------|------|------|---------|
| UI-01 | 欢迎卡片展示 | 打开 Copilot 面板 | 显示欢迎语 + 推荐问句 |
| UI-02 | 推荐问句可见 | 查看欢迎卡片 | 4 组推荐问句全部可见，覆盖不同域 |
| UI-03 | 问句点击发送 | 点击任一推荐问句 | 自动填入输入框并发送，返回回复 |

### 3.2 InlineSqlPreview

| 编号 | 场景 | 操作 | 期望结果 |
|------|------|------|---------|
| UI-04 | SQL 预览卡片 | 发送能产生 SQL 的问句 | AI 回复中 SQL 以卡片样式展示 |
| UI-05 | 编辑模式切换 | 点击 SQL 卡片的"编辑"按钮 | SQL 变为可编辑文本框 |
| UI-06 | 内联执行 | 点击"执行查询"按钮 | 在 Chat 内以表格展示查询结果 |
| UI-07 | 执行失败显示 | 构造一条会失败的 SQL 并执行 | 显示错误信息 + "编辑修正"按钮 |

### 3.3 FeedbackButtons

| 编号 | 场景 | 操作 | 期望结果 |
|------|------|------|---------|
| UI-08 | 点赞 | 点击回复消息的 thumbs_up 按钮 | 按钮变为激活态 |
| UI-09 | 点踩弹出表单 | 点击 thumbs_down 按钮 | 弹出原因选择表单 |
| UI-10 | 提交反馈 | 填写原因后提交 | 提示成功，`ai_chat_feedback` 表可查 |

---

## Phase 4: 业务场景回归（需要真实业务数据）

按验收矩阵 (`BG-10-dual-channel-nl2sql-matrix.md`) 的全部 7 个维度逐条执行:

1. **预制模板命中** (T-01 ~ T-10): 10 条，准确率要求 100%
2. **规则路由准确性** (R-01 ~ R-10): 10 条，准确率要求 > 90%
3. **NL2SQL 视图查询** (S-01 ~ S-08): 8 条，准确率要求 > 70%
4. **结算域隔离** (F-01 ~ F-04): 4 条，准确率要求 > 85%
5. **权限与安全** (P-01 ~ P-05): 5 条，全部通过
6. **枚举词典翻译** (E-01 ~ E-05): 5 条，全部通过
7. **交互体验** (U-01 ~ U-10): 10 条，全部通过

每条用例使用验收矩阵中的"验收记录模板"记录结果。

---

## 执行顺序建议

```
Phase 1 (单元测试)    ──> 开发自测阶段，随代码提交
Phase 2 (API 集成)    ──> 联调环境部署后
Phase 3 (前端交互)    ──> 前端组件开发完成后
Phase 4 (业务回归)    ──> 全链路可用、业务数据就绪后
```

---

## 缺陷分级

| 级别 | 定义 | 处理方式 |
|------|------|---------|
| P0 - 阻断 | 核心链路不通（路由失败/SQL 执行报错/权限绕过） | 立即修复，阻断发布 |
| P1 - 严重 | 高频问句匹配错误 / 结算隔离失效 | 当日修复 |
| P2 - 一般 | 低频问句路由偏差 / 枚举翻译缺失 | Sprint 内修复 |
| P3 - 建议 | UI 细节 / 提示文案优化 | 排入下一 Sprint |
