# SC-01: Maven 多模块项目骨架搭建

**状态**: READY
**依赖**: 无

## 目标

创建 dts-copilot Maven 多模块项目，包含 parent pom 和两个子模块（copilot-ai、copilot-analytics），配置 Spring Boot 3.4.5 和基本依赖。

## 技术设计

### 项目结构

```
dts-copilot/
├── pom.xml                          # parent pom
├── dts-copilot-ai/
│   ├── pom.xml
│   └── src/main/java/com/yuzhi/dts/copilot/ai/
│       ├── CopilotAiApplication.java
│       └── config/
│           └── ApplicationProperties.java
├── dts-copilot-analytics/
│   ├── pom.xml
│   └── src/main/java/com/yuzhi/dts/copilot/analytics/
│       ├── CopilotAnalyticsApplication.java
│       └── config/
│           └── ApplicationProperties.java
└── dts-copilot-webapp/              # 前端（sprint-6 填充）
    └── package.json
```

### Parent POM 关键配置

```xml
<groupId>com.yuzhi.dts</groupId>
<artifactId>dts-copilot</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>pom</packaging>

<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.5</version>
</parent>

<modules>
    <module>dts-copilot-ai</module>
    <module>dts-copilot-analytics</module>
</modules>

<properties>
    <java.version>21</java.version>
    <jhipster.version>8.11.0</jhipster.version>
    <mapstruct.version>1.6.3</mapstruct.version>
    <liquibase.version>4.29.2</liquibase.version>
</properties>
```

### copilot-ai 核心依赖

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-actuator
- liquibase-core
- postgresql driver
- jackson-databind
- mapstruct
- caffeine cache

### copilot-analytics 核心依赖

- spring-boot-starter-web
- spring-boot-starter-jdbc
- spring-boot-starter-actuator
- liquibase-core
- postgresql driver
- apache-poi (Excel 导出)
- mapstruct

## 影响文件

- `dts-copilot/pom.xml`（新建）
- `dts-copilot/dts-copilot-ai/pom.xml`（新建）
- `dts-copilot/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/CopilotAiApplication.java`（新建）
- `dts-copilot/dts-copilot-ai/src/main/resources/application.yml`（新建）
- `dts-copilot/dts-copilot-analytics/pom.xml`（新建）
- `dts-copilot/dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/CopilotAnalyticsApplication.java`（新建）
- `dts-copilot/dts-copilot-analytics/src/main/resources/application.yml`（新建）

## 完成标准

- [ ] `mvn clean compile` 编译通过，无错误
- [ ] CopilotAiApplication 可启动（忽略数据库连接，验证 Spring 上下文加载）
- [ ] CopilotAnalyticsApplication 可启动
- [ ] 两个子模块的 application.yml 包含端口、数据源、actuator 基本配置
