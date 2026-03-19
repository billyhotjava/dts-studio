# EL-05: 意图路由扩展（视图层 vs 主题层判定）

**优先级**: P1
**状态**: READY
**依赖**: EL-03, EL-04

## 目标

扩展 Sprint-10 的 IntentRouterService，增加"视图层 vs 主题层"的二次路由判定。趋势/统计类问题优先路由到主题层表，明细类走视图。

## 技术设计

### 路由判定规则

在现有域路由之后，增加数据层判定：

```java
// 主题层关键词
private static final Set<String> MART_KEYWORDS = Set.of(
    "趋势", "变化", "对比", "环比", "同比",
    "近3月", "近半年", "近一年", "最近几个月",
    "月度", "每月", "各月", "按月",
    "增长", "下降", "波动"
);

public enum DataLayer {
    VIEW,  // Sprint-10 视图层
    MART   // Sprint-11 主题层
}

// 在 RoutingResult 中增加 dataLayer 字段
public record RoutingResult(
    String domain,
    String primaryView,
    List<String> secondaryViews,
    double confidence,
    boolean needsClarification,
    DataLayer dataLayer       // NEW
) {}
```

### 主题层表到域的映射

| 域 | 视图层 | 主题层 |
|----|--------|--------|
| project | v_project_overview | mart_project_fulfillment_daily |
| flowerbiz | v_flower_biz_detail | fact_field_operation_event |
| green | v_project_green_current | mart_project_fulfillment_daily |
| settlement | v_monthly_settlement | mart_project_fulfillment_daily |
| task | v_task_progress | mart_project_fulfillment_daily |
| curing | v_curing_coverage | mart_project_fulfillment_daily |

### 降级策略

主题层不可用时（表为空或同步延迟 > 阈值）自动回退到视图层：

```java
if (dataLayer == DataLayer.MART && !isMartAvailable(targetMartTable)) {
    log.warn("Mart table {} not available, falling back to view layer", targetMartTable);
    dataLayer = DataLayer.VIEW;
}
```

## 完成标准

- [ ] RoutingResult 增加 dataLayer 字段
- [ ] 趋势关键词集定义
- [ ] 二次路由逻辑实现
- [ ] 主题层降级到视图层逻辑
- [ ] 路由单元测试覆盖趋势/明细场景
