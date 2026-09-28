# BG-01: 业务域盘点与高频问句清单

**优先级**: P0
**状态**: READY
**依赖**: IN-03, NV-07

## 目标

盘点 `adminapi/adminweb` 中与项目履约、现场业务相关的核心实体、API、页面，形成业务语义资产来源清单和各角色高频问句清单。

## 技术设计

### 1. 业务域盘点

以源码审查结果为基础，确认以下映射：

**项目履约域**

| 业务对象 | 主表 | 关键外键 | 关键状态字段 | 关键时间字段 |
|----------|------|----------|-------------|-------------|
| 客户 | `p_customer` | - | `status` (1=有效,2=其他) | `create_time` |
| 合同 | `p_contract` | `customer_id` | `status` (1=草稿,2=执行中,3=已结束) | `signing_time`, `start_date`, `end_date` |
| 项目点 | `p_project` | `contract_id` | `status` (1=正常,2=停用), `type` (1=租摆,2=节日摆) | `start_time`, `end_time` |
| 摆位 | `p_position` | `project_id` | `status` (0=正常,1=停用), `type` (1=室内,2=室外) | `create_time` |
| 项目绿植 | `p_project_green` | `project_id`, `position_id` | `status` (1=摆放中...7=已结束) | `pose_time` |
| 养护记录 | `t_curing_record` | `project_id` | `record_type` (1=养护,2=日报) | `curing_time` |

**现场业务域**

| 业务对象 | 主表 | 关键外键 | 关键状态字段 | 关键时间字段 |
|----------|------|----------|-------------|-------------|
| 报花业务 | `t_flower_biz_info` | `project_id` | `status` (-1~21), `biz_type` (1~12) | `apply_time`, `finish_time` |
| 报花明细 | `t_flower_biz_item` | `flower_biz_id` | `biz_type` (1~5,10) | `put_time`, `start_time`, `end_time` |
| 初摆 | `i_pendulum_info` | `project_id` | `status` (1~6) | `applicant_date`, `start_date`, `end_date` |
| 日常任务 | `t_daily_task_info` | `project_id` | `status` (-1,1,2,10), `task_type` (1~6) | `launch_time`, `start_time`, `finish_time` |

### 2. 高频问句清单

按用户角色从 adminweb 首页仪表盘、app 待处理入口、常用筛选维度提炼：

**项目经理（关注自己负责的项目）**
1. XX项目目前有多少在摆绿植？
2. XX项目这个月换花了多少次？
3. 我负责的项目中，哪个换花率最高？
4. XX项目上个月租金是多少？
5. XX项目有多少待审批的报花单？

**业务经理（关注客户和收款）**
6. 哪些合同快到期了（90天内）？
7. 上月应收款还有多少没收齐？
8. XX客户下面有几个项目？
9. 本月新签了几份合同？
10. 哪些项目的收款确认还在待处理？

**运营总监（关注全局对比）**
11. 当前在服项目一共多少个？
12. 各项目加花次数排行（本月）
13. 养护人均负责多少摆位？
14. 本月报花业务中，加花/换花/减花各多少？
15. 哪些项目的绿植数量变化最大？

**养护主管（关注现场执行）**
16. 今天有多少待处理的任务？
17. XX养护人负责哪些摆位？
18. 本月哪些摆位还没做过养护？
19. 进行中的初摆任务有几个？
20. XX项目的减花回收情况怎么样？

**财务（关注结算和开票）**
21. 上月未结算的项目有哪些？
22. XX客户累计欠款多少？
23. 本月已开票金额是多少？
24. 哪些项目的结算方式是固定月租？
25. 上季度各项目租金收入排名？

### 3. 参考源码范围

- `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/project/**`
- `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/flowerbiz/**`
- `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/tasknew/**`
- `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/pendulum/**`
- `adminweb/src/api/flower/**`
- `adminweb/src/views/flower/**`
- `adminweb/src/views/home/**`（首页仪表盘）
- `app/pages/**`（移动端操作页面）

## 完成标准

- [ ] 形成两个主题域的业务对象清单（含表名、状态码、时间字段）
- [ ] 形成 25+ 条高频问句清单，按角色分类
- [ ] 每条高频问句标注预期涉及的表和字段
- [ ] 标出高频业务词和易混淆词（报花 vs 加花、减花 vs 剪花、调花 vs 摆位调整）
- [ ] 为 BG-02~BG-04 提供可直接引用的来源清单
