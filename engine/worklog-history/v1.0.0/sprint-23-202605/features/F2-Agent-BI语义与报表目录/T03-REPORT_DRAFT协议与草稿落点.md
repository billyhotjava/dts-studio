# T03: `REPORT_DRAFT` 协议与草稿落点

**优先级**: P0  
**状态**: DONE  
**依赖**: T01, T02

## 目标

统一 Agent 生成新报表时的输出协议，让前端、`analysis_draft`、图表和大屏复用同一个结果。

## 建议协议

```json
{
  "responseKind": "REPORT_DRAFT",
  "domain": "flowerbiz",
  "question": "从2025年5月到现在租赁收入按月趋势",
  "dataSurface": "L1_DBT_MART",
  "qualityLevel": "MEDIUM",
  "qualityNotes": ["客户关联存在缺口，客户维度仅供核验"],
  "sql": "select ...",
  "explanation": "按 apply_month 聚合租赁类报花金额",
  "display": {
    "type": "line",
    "xField": "month",
    "yField": "amount",
    "title": "租赁收入月趋势"
  },
  "actions": ["preview", "save_draft", "create_card", "add_to_screen"]
}
```

## 草稿落点

优先复用 `analysis_draft`：

- `question`：原始问句。
- `sql_text`：安全 SQL。
- `response_kind`：`REPORT_DRAFT`。
- `suggested_display`：展示建议。
- `quality_level` / `quality_notes`：数据质量。
- `linked_card_id` / `linked_screen_id`：后续晋升结果。

## 验证

- [x] SSE/普通响应都能携带 `responseKind=REPORT_DRAFT`。
- [x] 前端能渲染报表卡并提供预览。
- [x] 草稿保存后能从查询资产中心打开。
- [x] 草稿可创建 card，card 可加入 screen。

## 完成标准

- [x] 自然语言生成的新报表具备从对话到报表资产的完整链路。
