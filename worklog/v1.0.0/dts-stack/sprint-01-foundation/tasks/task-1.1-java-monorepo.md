# Task 1.1: Java Monorepo — Parent POM & Shared Module

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this task.

**Sprint:** 1 — Foundation
**Points:** 5
**Status:** TODO

## Files

- Create: `dts-stack/source/pom.xml` (parent POM)
- Create: `dts-stack/source/dts-common/pom.xml`
- Create: `dts-stack/source/dts-common/src/main/java/com/dts/common/config/GrpcServerConfig.java`
- Create: `dts-stack/source/dts-common/src/main/java/com/dts/common/config/KafkaConfig.java`
- Create: `dts-stack/source/dts-common/src/main/java/com/dts/common/security/JwtContextInterceptor.java`
- Create: `dts-stack/source/dts-common/src/main/java/com/dts/common/security/RequestContext.java`
- Create: `dts-stack/source/dts-common/src/main/java/com/dts/common/audit/AuditEventPublisher.java`
- Create: `dts-stack/source/dts-common/src/main/java/com/dts/common/exception/DtsException.java`
- Create: `dts-stack/source/dts-common/src/main/java/com/dts/common/exception/ErrorCode.java`
- Test: `dts-stack/source/dts-common/src/test/java/com/dts/common/security/RequestContextTest.java`

## Step 1: Create parent POM

```xml
<!-- dts-stack/source/pom.xml -->
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
  </parent>
  <groupId>com.dts</groupId>
  <artifactId>dts-parent</artifactId>
  <version>3.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <properties>
    <java.version>21</java.version>
    <grpc.version>1.65.1</grpc.version>
    <protobuf.version>3.25.5</protobuf.version>
    <kafka.version>3.7.0</kafka.version>
    <flyway.version>10.15.0</flyway.version>
    <jooq.version>3.19.10</jooq.version>
  </properties>

  <modules>
    <module>dts-common</module>
  </modules>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-bom</artifactId>
        <version>${grpc.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>
```

## Step 2: Create dts-common module with shared classes

```java
// RequestContext.java
public record RequestContext(
    String userId,
    String tenantId,
    String traceId,
    List<String> roles,
    List<String> permissions
) {
    public static final Context.Key<RequestContext> CTX_KEY =
        Context.key("request-context");
}
```

```java
// JwtContextInterceptor.java
public class JwtContextInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String jwt = headers.get(
            Metadata.Key.of("authorization", ASCII_STRING_MARSHALLER));
        RequestContext ctx = parseJwt(jwt);
        Context context = Context.current()
            .withValue(RequestContext.CTX_KEY, ctx);
        return Contexts.interceptCall(context, call, headers, next);
    }
}
```

```java
// AuditEventPublisher.java
public class AuditEventPublisher {
    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;
    private static final String TOPIC = "dts.audit.events";

    public void publish(String eventType, String source, Object data) {
        RequestContext ctx = RequestContext.CTX_KEY.get();
        AuditEvent event = new AuditEvent(
            UUID.randomUUID().toString(),
            eventType, source,
            ctx.userId(), ctx.tenantId(), ctx.traceId(),
            Instant.now(), data
        );
        kafkaTemplate.send(TOPIC, event.traceId(), event);
    }
}
```

## Step 3: Write tests

```java
@Test
void requestContext_shouldBeImmutable() {
    var ctx = new RequestContext("u1", "t1", "tr1",
        List.of("admin"), List.of("data:read"));
    assertEquals("u1", ctx.userId());
    assertEquals("t1", ctx.tenantId());
}
```

Run: `mvn clean test -pl dts-common`
Expected: PASS

## Step 4: Verify full compilation

Run: `cd dts-stack/source && mvn clean compile`
Expected: BUILD SUCCESS

## Step 5: Commit

```bash
git add dts-stack/source/pom.xml dts-stack/source/dts-common/
git commit -m "feat(s1): initialize Java monorepo with dts-common module"
```
