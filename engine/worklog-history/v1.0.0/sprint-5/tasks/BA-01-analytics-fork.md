# BA-01: dts-analytics 核心代码 fork 与包名重构

**状态**: READY
**依赖**: SC-03

## 目标

完整 fork dts-analytics 源代码到 dts-copilot-analytics，重构包名和 Maven 坐标。

## 技术设计

### 包名重构

```
com.yuzhi.dts.analytics → com.yuzhi.dts.copilot.analytics
```

### Maven 坐标

```xml
<groupId>com.yuzhi.dts</groupId>
<artifactId>dts-copilot-analytics</artifactId>
<version>1.0.0-SNAPSHOT</version>
```

### 需要调整的配置

- `application.yml`: 端口改为 8092，数据源改为 copilot 数据库的 copilot_analytics schema
- Liquibase: `defaultSchemaName` 改为 `copilot_analytics`
- 静态资源路径: `/analytics` 保持不变

### 需要删除的代码

- `PlatformTrustedUserService`（sprint-4 替换）
- `PlatformInfraClient`（BA-02 替换）
- `PlatformAiNativeClient`（BA-04 替换）
- `AiConfigClient`（BA-04 替换）
- OIDC 认证配置（改用 API Key）

## 影响文件

- `dts-copilot-analytics/` 下所有 Java 文件（包名批量替换）
- `dts-copilot-analytics/pom.xml`（Maven 坐标修改）
- `dts-copilot-analytics/src/main/resources/config/application.yml`（配置修改）

## 完成标准

- [ ] 包名全部替换为 `com.yuzhi.dts.copilot.analytics`
- [ ] 无对 `com.yuzhi.dts.analytics` 的残留引用
- [ ] `mvn compile` 通过（允许因外部依赖缺失的暂时报错，后续 task 修复）
- [ ] Liquibase 迁移指向 `copilot_analytics` schema
