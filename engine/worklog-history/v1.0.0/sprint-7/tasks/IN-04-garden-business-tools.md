# IN-04: 园林业务 Tool 扩展示例

**状态**: READY
**依赖**: AA-04, IN-03

## 目标

开发 2-3 个园林业务专用 Tool 示例，展示如何通过 CopilotTool 接口扩展 AI 助手的业务能力。

## 技术设计

### 示例 Tool 1: 项目查询 Tool

```java
@Component
public class GardenProjectQueryTool implements CopilotTool {
    @Override public String name() { return "query_garden_projects"; }
    @Override public String description() {
        return "查询园林项目列表，支持按状态、区域、客户筛选";
    }
    // 通过注册的数据源执行查询
}
```

### 示例 Tool 2: 花卉统计 Tool

```java
@Component
public class FlowerStatsTool implements CopilotTool {
    @Override public String name() { return "flower_statistics"; }
    @Override public String description() {
        return "统计花卉租赁数据：在租数量、到期提醒、养护状态";
    }
}
```

### 示例 Tool 3: 财务摘要 Tool

```java
@Component
public class FinanceSummaryTool implements CopilotTool {
    @Override public String name() { return "finance_summary"; }
    @Override public String description() {
        return "查询财务摘要：收款情况、应收账款、费用报销统计";
    }
}
```

### 扩展方式

业务 Tool 作为独立 Spring Boot Starter 或直接在 copilot-ai 中以 Bean 注册：

```java
// 方式一：直接注册（简单）
@Component 即可自动注入 ToolRegistry

// 方式二：外部 Starter（推荐生产使用）
// dts-copilot-garden-tools 独立 JAR，通过 classpath 自动发现
```

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/tool/garden/GardenProjectQueryTool.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/tool/garden/FlowerStatsTool.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/tool/garden/FinanceSummaryTool.java`（新建）

## 完成标准

- [ ] 3 个园林业务 Tool 注册成功
- [ ] Agent 可通过自然语言触发 Tool 调用
- [ ] 例："查询在租项目总数" → 调用 GardenProjectQueryTool → 返回结果
- [ ] Tool 调用结果在聊天面板中正确展示
