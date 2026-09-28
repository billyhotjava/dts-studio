# CS-01: 模板快速通道

**优先级**: P0
**状态**: READY
**依赖**: -

## 目标

当 `ChatGroundingService.buildContext()` 命中预制查询模板并生成了 `resolvedSql` 时，跳过整个 ReAct 循环和 LLM 调用，直接用模板 SQL + 业务语义组装响应文本返回。

## 技术设计

### 改动位置

`AgentExecutionService.executeChat()` — 在 `resolveProvider()` 之前插入短路判断：

```java
// Template fast-path: skip LLM entirely when template matched with SQL
if (groundingContext.templateCode() != null && groundingContext.resolvedSql() != null) {
    String response = formatTemplateResponse(groundingContext);
    return new ChatExecutionResult(response, groundingContext.resolvedSql(), groundingContext);
}
```

### formatTemplateResponse 逻辑

```java
private String formatTemplateResponse(GroundingContext ctx) {
    StringBuilder sb = new StringBuilder();
    if (ctx.domain() != null) {
        sb.append("根据您的问题，已从 **").append(ctx.domain())
          .append("** 业务域匹配到预制查询模板");
        if (ctx.templateCode() != null) {
            sb.append("（").append(ctx.templateCode()).append("）");
        }
        sb.append("。\n\n");
    }
    sb.append("```sql\n").append(ctx.resolvedSql().trim()).append("\n```\n");
    if (ctx.primaryView() != null) {
        sb.append("\n查询目标视图：`").append(ctx.primaryView()).append("`");
    }
    return sb.toString();
}
```

## 预期效果

模板命中的业务问题（约占高频问句 60-70%）从 10-25s 降到 **<500ms**。

## 验收标准

- [ ] 发送 "哪些合同快到期了" 等模板命中问句，响应 <1s
- [ ] 返回内容包含 SQL 代码块和业务域标识
- [ ] 非模板问句仍走 ReAct 循环，行为不变
