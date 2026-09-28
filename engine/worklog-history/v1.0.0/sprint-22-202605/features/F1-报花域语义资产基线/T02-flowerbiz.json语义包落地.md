# T02: flowerbiz.json 语义包落地

**优先级**: P0
**状态**: READY
**依赖**: T01, F0-T01, F0-T03

## 目标

按 `procurement.json` 的格式标准，新增 `flowerbiz.json` 语义包，让 `SemanticPackService` 加载报花域时返回完整 objects / synonyms / fewShots / guardrails。

## flowerbiz.json 骨架

路径：`dts-copilot-ai/src/main/resources/semantic-packs/flowerbiz.json`

```json
{
  "domain": "flowerbiz",
  "description": "馨懿诚绿植租摆报花主题域，覆盖加摆/撤摆/换花/调花/坏账/销售/赠花/配料/盆架等 13 种业务类型 + 7 个状态。authority 口径基于 dts-stack dbt 项目 xycyl 命名空间产出（sprint-22 F4 落地）。",

  "objects": [
    {
      "name": "报花租赁汇总",
      "view": "xycyl_ads.xycyl_ads_flowerbiz_lease_summary",
      "description": "项目 × 月粒度的加摆/撤摆/换花/调花汇总。仅 bizType=1/2/3/4 已结束单。金额已按 amount_direction 拆分。",
      "keyDimensions": ["项目", "客户", "业务月份"],
      "keyMeasures": ["加摆金额", "撤摆金额", "净增金额", "报花单数"],
      "commonFilters": ["业务月份", "项目", "客户"],
      "defaultTimeField": "业务月份"
    },
    {
      "name": "报花租赁明细",
      "view": "xycyl_ads.xycyl_ads_flowerbiz_lease_detail",
      "description": "单据级明细，可查具体某次加摆/撤摆。",
      "keyDimensions": ["项目", "摆位", "绿植品种", "养护人", "申请人"],
      "keyMeasures": ["数量", "金额"],
      "commonFilters": ["项目", "申请时间", "完成时间", "状态", "业务类型"]
    },
    {
      "name": "待处理报花单",
      "view": "xycyl_ads.xycyl_ads_flowerbiz_pending",
      "description": "未结束的报花单（status != 5 且 != -1），含已停留天数。",
      "keyDimensions": ["项目", "申请人", "状态", "业务类型"],
      "keyMeasures": ["金额", "已停留天数"],
      "commonFilters": ["状态", "已停留天数", "项目"]
    },
    {
      "name": "报花销售汇总",
      "view": "xycyl_ads.xycyl_ads_flowerbiz_sale_summary",
      "description": "项目 × 月粒度的销售/赠送汇总。仅 bizType=7/8。**与租赁完全分开**。",
      "keyDimensions": ["项目", "客户", "业务月份"],
      "keyMeasures": ["销售金额", "赠送数量", "销售单数"],
      "commonFilters": ["业务月份", "项目", "客户"]
    },
    {
      "name": "报花坏账汇总",
      "view": "xycyl_ads.xycyl_ads_flowerbiz_baddebt_summary",
      "description": "客户坏账汇总。bizType=6。",
      "keyDimensions": ["项目", "客户", "业务月份", "坏账类型"],
      "keyMeasures": ["坏账金额"],
      "commonFilters": ["业务月份", "客户"]
    },
    {
      "name": "报花变更日志",
      "view": "xycyl_ads.xycyl_ads_flowerbiz_change_log",
      "description": "报花单的变更记录（金额/起租期/规格变更）。",
      "keyDimensions": ["报花单ID", "变更类型"],
      "keyMeasures": ["变更前金额", "变更后金额"],
      "commonFilters": ["变更时间", "变更类型"]
    },
    {
      "name": "报花回收明细",
      "view": "xycyl_ads.xycyl_ads_flowerbiz_recovery_detail",
      "description": "撤摆/调拨触发的回收清单。recovery_type 表去处。",
      "keyDimensions": ["项目", "回收去处", "库房", "回收人"],
      "keyMeasures": ["回收数量", "实际回收数量"],
      "commonFilters": ["回收时间", "回收去处"]
    },
    {
      "name": "养护人报花工作量",
      "view": "xycyl_ads.xycyl_ads_flowerbiz_curing_workload",
      "description": "养护人 × 月的报花单数（按 biz_category 分）。",
      "keyDimensions": ["养护人", "业务月份", "业务类别"],
      "keyMeasures": ["报花单数", "金额"],
      "commonFilters": ["业务月份", "养护人"]
    }
  ],

  "synonyms": [
    { "term": "加摆", "field": "biz_type=2", "views": ["xycyl_ads_flowerbiz_lease_summary", "xycyl_ads_flowerbiz_lease_detail"] },
    { "term": "加花", "field": "biz_type=2", "views": ["xycyl_ads_flowerbiz_lease_summary"] },
    { "term": "新增绿植", "field": "biz_type=2", "views": ["xycyl_ads_flowerbiz_lease_summary"] },
    { "term": "撤摆", "field": "biz_type=3", "views": ["xycyl_ads_flowerbiz_lease_summary", "xycyl_ads_flowerbiz_recovery_detail"] },
    { "term": "撤花", "field": "biz_type=3", "views": ["xycyl_ads_flowerbiz_lease_summary"] },
    { "term": "减花", "field": "biz_type=3", "views": ["xycyl_ads_flowerbiz_lease_summary"] },
    { "term": "退植", "field": "biz_type=3", "views": ["xycyl_ads_flowerbiz_recovery_detail"] },
    { "term": "换花", "field": "biz_type=1", "views": ["xycyl_ads_flowerbiz_lease_detail"] },
    { "term": "调花", "field": "biz_type=4", "views": ["xycyl_ads_flowerbiz_lease_detail"] },
    { "term": "调拨", "field": "biz_type=4", "views": ["xycyl_ads_flowerbiz_lease_detail"] },
    { "term": "售花", "field": "biz_type=7", "views": ["xycyl_ads_flowerbiz_sale_summary"] },
    { "term": "销售", "field": "biz_type=7", "views": ["xycyl_ads_flowerbiz_sale_summary"] },
    { "term": "卖花", "field": "biz_type=7", "views": ["xycyl_ads_flowerbiz_sale_summary"] },
    { "term": "赠花", "field": "biz_type=8", "views": ["xycyl_ads_flowerbiz_sale_summary"] },
    { "term": "赠送", "field": "biz_type=8", "views": ["xycyl_ads_flowerbiz_sale_summary"] },
    { "term": "坏账", "field": "biz_type=6", "views": ["xycyl_ads_flowerbiz_baddebt_summary"] },
    { "term": "死账", "field": "biz_type=6", "views": ["xycyl_ads_flowerbiz_baddebt_summary"] },
    { "term": "审核中", "field": "status=1", "views": ["xycyl_ads_flowerbiz_pending"] },
    { "term": "备货中", "field": "status=2", "views": ["xycyl_ads_flowerbiz_pending"] },
    { "term": "待结算", "field": "status=4", "views": ["xycyl_ads_flowerbiz_pending"] },
    { "term": "已结束", "field": "status=5", "views": ["xycyl_ads_flowerbiz_lease_summary"] },
    { "term": "起租", "field": "start_lease_time", "views": ["xycyl_ads_flowerbiz_lease_detail"] },
    { "term": "减租", "field": "end_lease_time", "views": ["xycyl_ads_flowerbiz_lease_detail"] },
    { "term": "养护人", "field": "curing_user", "views": ["xycyl_ads_flowerbiz_curing_workload", "xycyl_ads_flowerbiz_lease_detail"] },
    { "term": "申请人", "field": "applicant", "views": ["xycyl_ads_flowerbiz_lease_detail"] },
    { "term": "项目经理", "field": "project_manager", "views": ["xycyl_ads_flowerbiz_lease_detail"] },
    { "term": "业务经理", "field": "biz_manager", "views": ["xycyl_ads_flowerbiz_lease_detail"] },
    { "term": "净增", "field": "net_amount", "views": ["xycyl_ads_flowerbiz_lease_summary"] },
    { "term": "回收", "field": "recovery", "views": ["xycyl_ads_flowerbiz_recovery_detail"] },
    { "term": "报损", "field": "recovery_type=1", "views": ["xycyl_ads_flowerbiz_recovery_detail"] },
    { "term": "回购", "field": "recovery_type=2", "views": ["xycyl_ads_flowerbiz_recovery_detail"] },
    { "term": "留用", "field": "recovery_type=3", "views": ["xycyl_ads_flowerbiz_recovery_detail"] }
  ],

  "fewShots": [
    {
      "question": "本月各项目加摆撤摆净增减多少？",
      "sql": "SELECT 项目, 客户, ROUND(SUM(加摆金额),2) AS 加摆, ROUND(SUM(撤摆金额),2) AS 撤摆, ROUND(SUM(净增金额),2) AS 净增 FROM xycyl_ads.xycyl_ads_flowerbiz_lease_summary WHERE 业务月份 = DATE_FORMAT(CURDATE(),'%Y-%m') GROUP BY 项目, 客户 ORDER BY 净增 DESC"
    },
    {
      "question": "万象城最近的报花单",
      "sql": "SELECT * FROM xycyl_ads.xycyl_ads_flowerbiz_lease_detail WHERE 项目 LIKE '%万象城%' ORDER BY 申请时间 DESC LIMIT 20"
    },
    {
      "question": "审核中超过 7 天的报花单",
      "sql": "SELECT * FROM xycyl_ads.xycyl_ads_flowerbiz_pending WHERE 状态 = '审核中' AND 已停留天数 > 7 ORDER BY 已停留天数 DESC"
    },
    {
      "question": "本月销售金额前 10",
      "sql": "SELECT 项目, 客户, ROUND(销售金额,2) FROM xycyl_ads.xycyl_ads_flowerbiz_sale_summary WHERE 业务月份 = DATE_FORMAT(CURDATE(),'%Y-%m') ORDER BY 销售金额 DESC LIMIT 10"
    },
    {
      "question": "本月坏账客户",
      "sql": "SELECT 客户, ROUND(SUM(坏账金额),2) FROM xycyl_ads.xycyl_ads_flowerbiz_baddebt_summary WHERE 业务月份 = DATE_FORMAT(CURDATE(),'%Y-%m') GROUP BY 客户 ORDER BY 2 DESC"
    },
    {
      "question": "李师傅本月经手多少报花单",
      "sql": "SELECT 业务类别, 报花单数, ROUND(金额,2) FROM xycyl_ads.xycyl_ads_flowerbiz_curing_workload WHERE 养护人 LIKE '%李师傅%' AND 业务月份 = DATE_FORMAT(CURDATE(),'%Y-%m')"
    },
    {
      "question": "本月报损了多少花",
      "sql": "SELECT 项目, SUM(实际回收数量) AS 报损数 FROM xycyl_ads.xycyl_ads_flowerbiz_recovery_detail WHERE 回收去处='报损' AND DATE_FORMAT(回收时间,'%Y-%m') = DATE_FORMAT(CURDATE(),'%Y-%m') GROUP BY 项目 ORDER BY 报损数 DESC"
    },
    {
      "question": "起租期被改过的报花单",
      "sql": "SELECT * FROM xycyl_ads.xycyl_ads_flowerbiz_change_log WHERE 变更类型 = '起租减租变更' ORDER BY 变更时间 DESC"
    },
    {
      "question": "万象城从签约至今总加摆金额",
      "sql": "SELECT ROUND(SUM(加摆金额),2) FROM xycyl_ads.xycyl_ads_flowerbiz_lease_summary WHERE 项目 LIKE '%万象城%'"
    },
    {
      "question": "本月新签客户的首次报花",
      "sql": "WITH first_biz AS (SELECT 客户, MIN(业务月份) AS first_month FROM xycyl_ads.xycyl_ads_flowerbiz_lease_summary GROUP BY 客户) SELECT s.* FROM xycyl_ads.xycyl_ads_flowerbiz_lease_summary s JOIN first_biz f ON f.客户=s.客户 AND f.first_month=s.业务月份 WHERE s.业务月份 = DATE_FORMAT(CURDATE(),'%Y-%m')"
    }
  ],

  "guardrails": [
    "**禁止跨 bizType 直接 SUM**：报花单 13 种 bizType 的金额方向不同（加花+正、撤花-负、调花0），SUM 必须按 bizType 或 biz_category 分组。优先用 ads 层已经按 amount_direction 处理过的字段（加摆金额/撤摆金额/净增金额）。",
    "**禁止租赁与销售混合聚合**：bizType=7/8 走另一条结算链，不在 lease_summary 里。问'本月所有收入'时必须 UNION lease_summary + sale_summary。",
    "**禁止用 create_time 作业务时间**：业务时间用 start_lease_time（已被 dws/ads 层按月归口）。create_time 仅用于审计。",
    "**禁止从底层 t_flower_biz_* 现场拼**：所有报花问句走 xycyl_ads.* 层，避免 13 bizType / 7 状态 / 软外键 / 状态竞态等陷阱。",
    "**金额聚合必须 ROUND(...,2)**：避免精度漂移。",
    "**已结束 vs 进行中的边界**：lease_summary / sale_summary / baddebt_summary 默认仅含 status=5 已结束单；'待处理'类问句必须走 xycyl_ads_flowerbiz_pending。",
    "**回收数量 vs 实际回收数量**：业务上常关心实际回收数量（real_recovery_number），而非计划回收数量。",
    "**起租时间可被事后修改**：通过 t_flower_rent_time_log 跟踪。问'最近租期变更过的'走 xycyl_ads_flowerbiz_change_log。"
  ]
}
```

## 影响范围

- `dts-copilot-ai/src/main/resources/semantic-packs/flowerbiz.json` —— 新增（**主交付物**）
- 不改 `SemanticPackService.java` —— 现有加载逻辑足够
- 不改其它 pack（procurement / project-fulfillment / field-operations）

## 验证

- [ ] `mvn test -pl dts-copilot-ai` 通过
- [ ] 启动 copilot-ai，日志显示 flowerbiz 域已注册
- [ ] JSON schema 校验：`objects[].view` 都存在（如 F4 未完成可暂时跳过）
- [ ] 不与 procurement.json / project-fulfillment.json 字段冲突

## 完成标准

- [ ] flowerbiz.json 落地，覆盖 8 业务对象、30+ 同义词、10 fewShots、8 guardrails
- [ ] sprint README 列出的 8 类样本问句都能在 fewShots 中找到对应或近邻
- [ ] guardrails 显式覆盖 6 个真实陷阱
