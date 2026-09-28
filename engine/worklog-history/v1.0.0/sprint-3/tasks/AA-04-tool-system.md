# AA-04: Tool 注册与执行管线抽取（裁剪治理专用 Tool）

**状态**: READY
**依赖**: AA-03

## 目标

从 dts-platform 抽取 Tool 注册表和执行管线，裁剪数据治理专用 Tool，保留通用能力，预留业务扩展点。

## 技术设计

### 来源文件

- `dts-platform/service/ai/tool/ToolRegistry.java`
- `dts-platform/service/ai/tool/ToolExecutionService.java`
- `dts-platform/service/ai/tool/DtsTool.java`（接口）
- `dts-platform/service/ai/pipeline/*`（Filter Chain）

### 保留的 Tool

| Tool | 用途 |
|------|------|
| ExecuteQueryTool | 执行 SQL 查询 |
| ValidateSqlTool | 验证 SQL 语法 |
| ExplainSqlTool | 解释 SQL 执行计划 |
| OptimizeSqlTool | SQL 优化建议 |
| SchemaLookupTool | 查询表/列元数据 |
| CatalogSearchTool | 搜索数据目录（适配为数据源搜索） |

### 裁剪的 Tool

- DataLineageTool（数据血缘）
- DwModelingTool（数据仓库建模）
- DataQualityTool（数据质量）
- CreateDbtModelTool（dbt 模型创建）
- TriggerPipelineTool（触发数据管道）
- 其他治理专用 Tool

### 扩展点设计

```java
public interface CopilotTool {
    String name();
    String description();
    JsonNode schema();  // JSON Schema 定义参数
    ToolResult execute(ToolContext context, JsonNode arguments);
}

// 业务系统可通过 Spring Bean 注册自定义 Tool
@Component
public class MyBusinessTool implements CopilotTool { ... }
```

### 执行管线（Filter Chain）

保留核心过滤器：
- PermissionFilter — 权限检查
- GuardrailFilter — 安全防护
- AuditFilter — 操作审计
- ExecutionFilter — 实际执行

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/tool/CopilotTool.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/tool/ToolRegistry.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/tool/ToolExecutionService.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/tool/builtin/*`（内置 Tool）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/tool/pipeline/*`（过滤器链）

## 完成标准

- [ ] ToolRegistry 可注册/注销 Tool
- [ ] 内置 6 个通用 Tool 注册成功
- [ ] 执行管线过滤器链正常工作
- [ ] 自定义 CopilotTool 可通过 Spring Bean 自动注册
- [ ] Agent 可通过 Tool 名称调用已注册的 Tool
