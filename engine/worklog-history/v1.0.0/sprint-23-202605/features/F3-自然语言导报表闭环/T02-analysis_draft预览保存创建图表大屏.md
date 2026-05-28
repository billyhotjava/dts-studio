# T02: `analysis_draft` 预览、保存、创建图表/大屏

**优先级**: P0  
**状态**: DONE  
**依赖**: T01

## 目标

把 Copilot 生成的 `REPORT_DRAFT` 转成可复用报表资产，避免“对话里看过一次就丢”。

## 流程

```text
REPORT_DRAFT
  -> preview/run
  -> save analysis_draft
  -> create card
  -> add to screen
```

## 验证

- [x] 报表草稿可预览数据。
- [x] 保存草稿后刷新页面仍可找回。
- [x] 草稿创建的 card 使用同一份 SQL 和展示配置。
- [x] card 加入 screen 后可以正常执行。
- [x] 草稿到 card/screen 的链路保留来源问句和质量等级。

## 完成标准

- [x] 自然语言生成报表可以晋升成正式 BI 资产。
