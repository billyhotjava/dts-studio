# F1: 跨服务接入与指标目录同步(后端)

**优先级**: P0
**状态**: READY
**阶段**: P1
**依赖**: 无
**前缀**: IF
**服务**: dts-copilot-ai(`com.yuzhi.dts.copilot.ai`,Spring Boot / Java 21 / 端口 8091)
**对端**: dts-platform `GovernanceIndicatorResource`(`@RequestMapping("/api/governance")`,零改码)

## 目标

让 copilot-ai 以**单受限只读机器账号**(Keycloak realm `S10`,试用期权宜方案,对应设计 D5)连接 dts-platform,完成三件事:

1. **接入鉴权**:用 Keycloak client-credentials 拿 access token,以 `Authorization: Bearer` 调 dts-platform 的 governance 指标端点;平台零改码,仅由运维为该账号配只读角色。
2. **目录同步**:定时(~1h)+ 手动拉取 `status=已发布` 指标目录,分页聚合,缓存(内存 + 可选持久化扛重启),逐指标记录 `version`,落进 copilot 语义层供 F3 本地匹配(对应设计 ① 同步面)。
3. **取值客户端 + BFF**:封装 `dashboard/detail/drilldown` 取值方法并带显式降级(对应 ③ 取值面);对 dts-copilot-webapp 暴露「指标目录列表 + 取值」BFF REST 端点(供 F2 前端调用)。

口径计算与产数全部留在 dts-platform(单一口径源);copilot-ai 只读消费、缓存目录、转发取值,不重算口径。

## 设计依据

`docs/superpowers/specs/2026-05-31-dts-platform-indicator-federation-design.md` —— D1(服务联邦)、D3(目录同步进语义层)、D4(平台零改码)、D5(单机器账号)、D6(显式降级)。

## 复用的真实代码资产

| 资产 | 路径 | 复用点 |
|------|------|--------|
| 外部 HTTP 客户端范式 | `dts-copilot-ai/.../service/copilot/HttpAdminApiActionClient.java` | `java.net.http.HttpClient` + `@Value` 配置 + `connectTimeout`/`timeout` + `ObjectMapper` 解析 + 失败返回结构体不抛异常 |
| 内存目录 + 评分匹配范式 | `dts-copilot-ai/.../service/copilot/BusinessObjectCatalogService.java` | `@Service` + `entries()` 列表 + `findBestMatch` 评分;指标目录缓存对齐此风格供 F3 |
| 语义层装载范式 | `dts-copilot-ai/.../service/copilot/SemanticPackService.java` | `@PostConstruct` 装载 + `Map<String,...>` 缓存;指标目录作为新语义来源并列 |
| REST 控制器范式 | `dts-copilot-ai/.../web/rest/AgentChatResource.java`、`AiConfigResource.java` | `@RestController @RequestMapping("/api/ai/...")` + `ApiResponse<T>` 信封 |
| API 信封 | `dts-copilot-ai/.../web/rest/dto/ApiResponse.java` | `ApiResponse.ok(data)` / `ApiResponse.error(msg)` |
| 鉴权白名单 | `dts-copilot-ai/.../security/ApiKeyAuthFilter.java` | BFF 端点需登记白名单或走 API key;用户上下文走 `X-DTS-*` 头(`UserContextFilter`) |
| 平台端点与 DTO | `dts-platform/.../web/rest/GovernanceIndicatorResource.java`、`.../service/governance/dto/IndicatorDto.java` | 取值/目录契约源头 |

> 现状提示:copilot-ai **当前没有** `@Scheduled` / `@EnableScheduling` / glossary 同步实现(已 grep 确认),T02 需新增定时基础设施,这是本 Feature 的真实增量。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 机器账号鉴权 + PlatformIndicatorClient 骨架 | P0 | READY | 无 |
| T02 | IndicatorCatalogSyncService 目录同步(定时+手动) | P0 | READY | T01 |
| T03 | 指标目录领域模型 + 语义层落位 | P0 | READY | T02 |
| T04 | 取值客户端方法 + 显式降级 | P0 | READY | T01 |
| T05 | BFF REST 端点(给 webapp) | P0 | READY | T03, T04 |

## 完成标准

- [ ] copilot-ai 用机器账号(Keycloak `S10` client-credentials)能拿 token 并成功调 `GET /api/governance/indicators`,平台零改码。
- [ ] `IndicatorCatalogSyncService` 启动即拉一次、之后定时(~1h)刷新,并提供手动触发;只取 `status=已发布`,分页全量拉齐,逐指标记录 `version`。
- [ ] 同步结果以 `IndicatorCatalogEntry` 模型缓存(内存 + 可选持久化),进语义层供 F3 匹配;同步失败时保留上次缓存(stale-while-revalidate)不清空。
- [ ] `PlatformIndicatorClient` 提供 `getDashboard/getDetail/drilldown`,平台不可达/超时返回**显式降级结果**(非异常、非静默假数据),供 F3/前端退回现生成 SQL。
- [ ] copilot-ai 暴露 `GET /api/ai/indicators`(目录列表)与取值 BFF 端点,信封为 `ApiResponse<T>`,风格对齐现有 `/api/ai/*` 控制器,供 F2 调用。
- [ ] 所有跨服务超时/重试/降级有日志埋点;密钥不硬编码(走环境变量 / `copilot.platform.*` 配置)。
- [ ] 单元测试覆盖鉴权 token 获取、分页同步、降级分支;`it/README.md` 记录对平台的 live contract 验证证据。
