# BG-04: 预制查询模板 TOP 20

**优先级**: P0
**状态**: READY
**依赖**: BG-02, BG-03

## 目标

为各角色高频问句建立预制查询模板，通过意图匹配 + 参数提取直接生成参数化 SQL，绕过 NL2SQL 生成环节，实现 100% 准确率覆盖 60%+ 的实际使用场景。

## 技术设计

### 模板存储结构

在 `copilot_ai` 库中新建 `nl2sql_query_template` 表：

```sql
CREATE TABLE nl2sql_query_template (
    id              BIGSERIAL PRIMARY KEY,
    template_code   VARCHAR(64)   NOT NULL UNIQUE,  -- 模板编码
    domain          VARCHAR(32)   NOT NULL,          -- 业务域（project/flowerbiz/settlement/task/curing/pendulum）
    role_hint       VARCHAR(32),                     -- 适用角色提示（manager/biz/ops/finance/curing）
    intent_patterns TEXT          NOT NULL,           -- 意图匹配模式（JSON 数组，支持正则和关键词）
    question_samples TEXT         NOT NULL,           -- 示例问句（JSON 数组，用于模糊匹配）
    sql_template    TEXT          NOT NULL,           -- 参数化 SQL 模板
    parameters      TEXT,                            -- 参数定义（JSON，含类型和默认值）
    target_view     VARCHAR(128),                    -- 目标视图
    description     VARCHAR(256),                    -- 模板说明
    priority        INT DEFAULT 0,                   -- 优先级（高优先级优先匹配）
    is_active       BOOLEAN DEFAULT true,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);
```

### 预制模板清单

#### 项目域 (project)

**TPL-01: 项目在摆绿植数**
```yaml
intent_patterns: ["(项目|XX).*(绿植|花).*(多少|几|数量|总数)"]
question_samples:
  - "XX项目目前有多少在摆绿植？"
  - "XX项目的绿植数量"
  - "查一下XX项目有几盆花"
sql_template: |
  SELECT project_name, count(*) as 在摆绿植数, sum(good_number) as 绿植总量
  FROM v_project_green_current
  WHERE project_name LIKE concat('%', :project_name, '%')
  GROUP BY project_name
parameters: { "project_name": { "type": "string", "required": true } }
```

**TPL-02: 在服项目总数**
```yaml
intent_patterns: ["(在服|正常|当前|活跃).*(项目).*(多少|几|总数|数量)"]
question_samples:
  - "当前在服项目一共多少个？"
  - "有多少正常运营的项目？"
  - "活跃项目数"
sql_template: |
  SELECT project_status_name, count(*) as 项目数
  FROM v_project_overview
  WHERE project_status_name = '正常'
  GROUP BY project_status_name
parameters: {}
```

**TPL-03: 合同即将到期**
```yaml
intent_patterns: ["(合同).*(到期|快到期|即将到期|过期)"]
question_samples:
  - "哪些合同快到期了？"
  - "90天内到期的合同"
  - "即将到期的合同有哪些？"
sql_template: |
  SELECT project_name, customer_name, contract_title, contract_end_date,
         (contract_end_date - CURRENT_DATE) as 剩余天数
  FROM v_project_overview
  WHERE contract_status_name = '执行中'
    AND contract_end_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL ':days days'
  ORDER BY contract_end_date ASC
parameters: { "days": { "type": "integer", "default": 90 } }
```

**TPL-04: 客户项目查询**
```yaml
intent_patterns: ["(客户|XX).*(有|下面|名下).*(几个|多少|哪些).*(项目)"]
question_samples:
  - "XX客户下面有几个项目？"
  - "XX客户名下的项目有哪些？"
sql_template: |
  SELECT customer_name, project_name, project_status_name, green_count, total_rent
  FROM v_project_overview
  WHERE customer_name LIKE concat('%', :customer_name, '%')
  ORDER BY project_name
parameters: { "customer_name": { "type": "string", "required": true } }
```

#### 报花业务域 (flowerbiz)

**TPL-05: 项目报花次数统计**
```yaml
intent_patterns: ["(项目|XX).*(换花|加花|减花|报花).*(多少|几|次数|次)"]
question_samples:
  - "XX项目这个月换花了多少次？"
  - "本月XX项目的加花次数"
  - "XX项目报花情况"
sql_template: |
  SELECT project_name, biz_type_name, count(*) as 次数, sum(plant_number) as 总数量
  FROM v_flower_biz_detail
  WHERE project_name LIKE concat('%', :project_name, '%')
    AND biz_month = :month
  GROUP BY project_name, biz_type_name
  ORDER BY 次数 DESC
parameters: {
  "project_name": { "type": "string", "required": true },
  "month": { "type": "string", "default": "CURRENT_MONTH" }
}
```

**TPL-06: 各项目加花排行**
```yaml
intent_patterns: ["(项目|各项目).*(加花|换花|报花).*(排行|排名|最多)"]
question_samples:
  - "各项目加花次数排行"
  - "本月哪个项目加花最多？"
  - "报花排行榜"
sql_template: |
  SELECT project_name, count(*) as 次数, sum(plant_number) as 总数量
  FROM v_flower_biz_detail
  WHERE biz_type_name = :biz_type
    AND biz_month = :month
  GROUP BY project_name
  ORDER BY 次数 DESC
  LIMIT :top_n
parameters: {
  "biz_type": { "type": "string", "default": "加花" },
  "month": { "type": "string", "default": "CURRENT_MONTH" },
  "top_n": { "type": "integer", "default": 10 }
}
```

**TPL-07: 报花业务类型分布**
```yaml
intent_patterns: ["(本月|这个月|上月).*(报花|业务).*(加花|换花|减花).*(各|分别|分布|各多少)"]
question_samples:
  - "本月报花业务中，加花/换花/减花各多少？"
  - "这个月各类型报花数量"
sql_template: |
  SELECT biz_type_name, count(*) as 业务单数, sum(plant_number) as 总数量
  FROM v_flower_biz_detail
  WHERE biz_month = :month
    AND biz_status_name NOT IN ('作废', '驳回', '草稿')
  GROUP BY biz_type_name
  ORDER BY 业务单数 DESC
parameters: { "month": { "type": "string", "default": "CURRENT_MONTH" } }
```

**TPL-08: 待审批报花单**
```yaml
intent_patterns: ["(待审批|审核中|未审批).*(报花|业务单)"]
question_samples:
  - "有多少待审批的报花单？"
  - "审核中的业务单有哪些？"
sql_template: |
  SELECT biz_code, biz_type_name, project_name, apply_user_name, apply_time
  FROM v_flower_biz_detail
  WHERE biz_status_name = '审核中'
  ORDER BY apply_time DESC
parameters: {}
```

#### 结算域 (settlement)

**TPL-09: 项目月租金查询**
```yaml
intent_patterns: ["(项目|XX).*(上月|上个月|本月|这个月).*(租金|应收|收入)"]
question_samples:
  - "XX项目上个月租金是多少？"
  - "本月各项目租金"
sql_template: |
  SELECT project_name, customer_name, settlement_month, total_rent as 应收租金,
         received_amount as 已收, outstanding_amount as 未收
  FROM v_monthly_settlement
  WHERE project_name LIKE concat('%', :project_name, '%')
    AND settlement_month = :month
parameters: {
  "project_name": { "type": "string" },
  "month": { "type": "string", "default": "LAST_MONTH" }
}
```

**TPL-10: 未结算项目**
```yaml
intent_patterns: ["(未结算|待结算|没结算).*(项目)"]
question_samples:
  - "上月未结算的项目有哪些？"
  - "还有哪些项目没结算？"
sql_template: |
  SELECT project_name, customer_name, settlement_month, total_rent, settlement_status_name
  FROM v_monthly_settlement
  WHERE settlement_status_name = '待结算'
    AND settlement_month = :month
  ORDER BY total_rent DESC
parameters: { "month": { "type": "string", "default": "LAST_MONTH" } }
```

**TPL-11: 客户欠款查询**
```yaml
intent_patterns: ["(客户|XX).*(欠款|未收|应收未收|欠|拖欠)"]
question_samples:
  - "XX客户累计欠款多少？"
  - "哪些客户还有欠款？"
sql_template: |
  SELECT customer_name, sum(outstanding_amount) as 累计欠款,
         count(DISTINCT project_name) as 涉及项目数
  FROM v_monthly_settlement
  WHERE outstanding_amount > 0
    AND (:customer_name IS NULL OR customer_name LIKE concat('%', :customer_name, '%'))
  GROUP BY customer_name
  ORDER BY 累计欠款 DESC
parameters: { "customer_name": { "type": "string" } }
```

**TPL-12: 项目租金排名**
```yaml
intent_patterns: ["(项目).*(租金|收入).*(排名|排行)"]
question_samples:
  - "上季度各项目租金收入排名"
  - "项目租金排行"
sql_template: |
  SELECT project_name, customer_name, sum(total_rent) as 累计租金
  FROM v_monthly_settlement
  WHERE settlement_month BETWEEN :start_month AND :end_month
  GROUP BY project_name, customer_name
  ORDER BY 累计租金 DESC
  LIMIT :top_n
parameters: {
  "start_month": { "type": "string", "required": true },
  "end_month": { "type": "string", "required": true },
  "top_n": { "type": "integer", "default": 10 }
}
```

#### 任务域 (task)

**TPL-13: 待处理任务**
```yaml
intent_patterns: ["(待处理|待办|进行中|未完成).*(任务)"]
question_samples:
  - "今天有多少待处理的任务？"
  - "进行中的任务有哪些？"
sql_template: |
  SELECT task_code, task_title, task_type_name, project_name, leading_user_name,
         launch_time, completion_rate
  FROM v_task_progress
  WHERE task_status_name = '进行中'
  ORDER BY launch_time DESC
parameters: {}
```

**TPL-14: 初摆任务进度**
```yaml
intent_patterns: ["(初摆).*(任务|进度|进行中|待完成)"]
question_samples:
  - "进行中的初摆任务有几个？"
  - "初摆进度怎么样？"
sql_template: |
  SELECT pendulum_code, pendulum_title, pendulum_status_name, project_name,
         applicant_name, applicant_date, total_budget_cost, actual_cost
  FROM v_pendulum_progress
  WHERE pendulum_status_name IN ('初摆中', '待审批')
  ORDER BY applicant_date DESC
parameters: {}
```

#### 养护域 (curing)

**TPL-15: 养护人负责摆位**
```yaml
intent_patterns: ["(养护人|XX).*(负责|管理).*(摆位|多少)"]
question_samples:
  - "XX养护人负责哪些摆位？"
  - "养护人均负责多少摆位？"
sql_template: |
  SELECT curing_user_name, sum(total_position_count) as 负责摆位总数
  FROM v_curing_coverage
  WHERE curing_month = :month
    AND (:curing_user IS NULL OR curing_user_name LIKE concat('%', :curing_user, '%'))
  GROUP BY curing_user_name
  ORDER BY 负责摆位总数 DESC
parameters: {
  "month": { "type": "string", "default": "CURRENT_MONTH" },
  "curing_user": { "type": "string" }
}
```

**TPL-16: 未养护摆位**
```yaml
intent_patterns: ["(没养护|未养护|还没养护).*(摆位)"]
question_samples:
  - "本月哪些摆位还没做过养护？"
  - "未养护的摆位有多少？"
sql_template: |
  SELECT project_name, curing_user_name, total_position_count, covered_position_count,
         (total_position_count - covered_position_count) as 未养护摆位数, coverage_rate
  FROM v_curing_coverage
  WHERE curing_month = :month
    AND coverage_rate < 1.0
  ORDER BY 未养护摆位数 DESC
parameters: { "month": { "type": "string", "default": "CURRENT_MONTH" } }
```

#### 综合对比域

**TPL-17: 项目换花率排行**
```yaml
intent_patterns: ["(换花率|换花.*(比例|率)|换花最多)"]
question_samples:
  - "哪个项目换花率最高？"
  - "各项目换花比例排行"
sql_template: |
  SELECT po.project_name,
         COALESCE(fb.换花次数, 0) as 换花次数,
         po.green_count as 在摆绿植数,
         CASE WHEN po.green_count > 0
              THEN ROUND(COALESCE(fb.换花次数, 0)::numeric / po.green_count, 2)
              ELSE 0 END as 换花率
  FROM v_project_overview po
  LEFT JOIN (
    SELECT project_name, count(*) as 换花次数
    FROM v_flower_biz_detail
    WHERE biz_type_name = '换花' AND biz_month = :month
    GROUP BY project_name
  ) fb ON po.project_name = fb.project_name
  WHERE po.project_status_name = '正常'
  ORDER BY 换花率 DESC
  LIMIT :top_n
parameters: {
  "month": { "type": "string", "default": "CURRENT_MONTH" },
  "top_n": { "type": "integer", "default": 10 }
}
```

**TPL-18: 绿植数量变化最大的项目**
```yaml
intent_patterns: ["(绿植).*(变化|变动|增减).*(最大|最多)"]
question_samples:
  - "哪些项目的绿植数量变化最大？"
  - "本月绿植增减变动排行"
sql_template: |
  SELECT project_name,
         sum(CASE WHEN biz_type_name = '加花' THEN plant_number ELSE 0 END) as 加花数,
         sum(CASE WHEN biz_type_name = '减花' THEN plant_number ELSE 0 END) as 减花数,
         sum(CASE WHEN biz_type_name = '加花' THEN plant_number ELSE 0 END)
           - sum(CASE WHEN biz_type_name = '减花' THEN plant_number ELSE 0 END) as 净变化
  FROM v_flower_biz_detail
  WHERE biz_month = :month
    AND biz_type_name IN ('加花', '减花')
    AND biz_status_name NOT IN ('作废', '驳回', '草稿')
  GROUP BY project_name
  ORDER BY abs(净变化) DESC
  LIMIT :top_n
parameters: {
  "month": { "type": "string", "default": "CURRENT_MONTH" },
  "top_n": { "type": "integer", "default": 10 }
}
```

**TPL-19: 减花回收情况**
```yaml
intent_patterns: ["(减花|回收).*(情况|统计|数量)"]
question_samples:
  - "XX项目的减花回收情况怎么样？"
  - "本月减花统计"
sql_template: |
  SELECT project_name, count(*) as 减花单数, sum(plant_number) as 减花总量,
         sum(biz_total_cost) as 涉及成本
  FROM v_flower_biz_detail
  WHERE biz_type_name = '减花'
    AND biz_month = :month
    AND (:project_name IS NULL OR project_name LIKE concat('%', :project_name, '%'))
  GROUP BY project_name
  ORDER BY 减花总量 DESC
parameters: {
  "month": { "type": "string", "default": "CURRENT_MONTH" },
  "project_name": { "type": "string" }
}
```

**TPL-20: 结算方式分布**
```yaml
intent_patterns: ["(结算方式|固定月租).*(哪些|分布|项目)"]
question_samples:
  - "哪些项目的结算方式是固定月租？"
  - "各结算方式的项目分布"
sql_template: |
  SELECT settlement_type_name, count(*) as 项目数,
         sum(total_rent) as 月租金合计
  FROM v_project_overview
  WHERE project_status_name = '正常'
  GROUP BY settlement_type_name
parameters: {}
```

## 匹配引擎设计

1. **第一层：正则匹配** — 用 `intent_patterns` 快速过滤候选模板
2. **第二层：语义相似度** — 用 `question_samples` 做 embedding 相似度排序
3. **第三层：参数提取** — 从用户问句中提取命名参数（项目名、月份、用户名等）
4. **兜底**：无匹配 → 走 NL2SQL 动态生成

参数默认值约定：
- `CURRENT_MONTH`: 当前月份（YYYY-MM）
- `LAST_MONTH`: 上个月
- 日期参数：支持"本月/上月/上季度/今年"等自然语言解析

## 完成标准

- [ ] `nl2sql_query_template` 表创建
- [ ] 20+ 模板入库，覆盖 6 个业务域
- [ ] 模板匹配引擎实现（正则 + embedding 双层）
- [ ] 参数提取支持项目名、客户名、月份、人名等常见维度
- [ ] 所有模板 SQL 在视图层可执行并返回正确结果
- [ ] 未命中模板时平滑降级到 NL2SQL
