# F1: 跨服务接入与指标目录同步(后端)

**优先级**: P0
**状态**: DONE
**阶段**: P1
**依赖**: 无
**前缀**: IF
**服务**: dts-copilot-analytics(webapp-facing BFF);dts-copilot-ai(语义同步/路由目标态)
**对端**: dts-platform `GovernanceIndicatorResource`(`@RequestMapping("/api/governance")`,指标业务零改动;服务认证白名单最小改动)

## 目标

让 copilot 以**单受限只读机器账号**(Keycloak realm `S10`,试用期权宜方案,对应设计 D5)连接 dts-platform,完成三件事。当前实现先按现有调用链把 webapp-facing BFF 落在 `dts-copilot-analytics`,避免 webapp 绕远路或新增反向依赖;copilot-ai 的目录同步与指标优先路由仍留在 T02/T03/F3。

1. **接入鉴权**:复用 dts-platform 既有服务间认证契约,以 `X-DTS-Service: dts-copilot` + `X-DTS-Service-Token` 调 governance 指标端点;平台只补只读白名单/配置,不改指标业务逻辑。
2. **目录同步**:定时(~1h)+ 手动拉取 `status=已发布` 指标目录,分页聚合,缓存(内存 + 可选持久化扛重启),逐指标记录 `version`,落进 copilot 语义层供 F3 本地匹配(对应设计 ① 同步面)。
3. **取值客户端 + BFF**:封装 `dashboard/detail/drilldown` 取值方法并带显式降级(对应 ③ 取值面);对 dts-copilot-webapp 暴露「指标目录列表 + 取值」BFF REST 端点(供 F2 前端调用)。

口径计算与产数全部留在 dts-platform(单一口径源);copilot 只读消费、缓存/转发取值,不重算口径。

## 当前进展(2026-05-31)

- 已完成 analytics 侧 `PlatformIndicatorClient`:支持 `DTS_PLATFORM_BASE_URL`、优先服务头(`DTS_PLATFORM_SERVICE_NAME/TOKEN`),并保留静态 `DTS_PLATFORM_AUTH_TOKEN` 或 `DTS_PLATFORM_TOKEN_URL/CLIENT_ID/CLIENT_SECRET` 作为 fallback;目录和取值失败均返回 `degraded:true`,不向 UI 抛裸异常;`dashboard/detail/drilldown` 取值支持 TTL 微缓存。
- 已完成 analytics BFF REST:`GET /api/platform/indicators`、`/dashboard`、`/{indicatorId}/detail`、`/{indicatorId}/drilldown`,通过 webapp 代理路径 `/api/analytics/platform/indicators*` 被 F2 消费。
- 已完成 copilot-ai 本地目录同步和语义层落位:`IndicatorCatalogSyncService` 启动/定时刷新已发布指标目录,`IndicatorCatalogStore` 提供 `all/byCode/byId/status`,失败保留 stale cache,version 变化写入 `SyncResult.caliberChangedCodes`。
- Live Contract 已补:服务头访问 `/api/governance/indicators*` 返回 200,analytics BFF 返回 `degraded=false`;使用 `codex_sprint29_live_metric` 本地发布指标 fixture 完成 catalog/dashboard/detail/drilldown 样本对账。

## 设计依据

`docs/superpowers/specs/2026-05-31-dts-platform-indicator-federation-design.md` —— D1(服务联邦)、D3(目录同步进语义层)、D4(平台指标业务零改动)、D5(单机器账号/服务认证)、D6(显式降级)。

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
| T01 | 机器账号鉴权 + PlatformIndicatorClient 骨架 | P0 | DONE | 无 |
| T02 | IndicatorCatalogSyncService 目录同步(定时+手动) | P0 | DONE | T01 |
| T03 | 指标目录领域模型 + 语义层落位 | P0 | DONE | T02 |
| T04 | 取值客户端方法 + 显式降级 | P0 | DONE | T01 |
| T05 | BFF REST 端点(给 webapp) | P0 | DONE | T04 |

## 完成标准

- [x] analytics BFF 支持机器账号配置(服务头 token,静态 token 或 Keycloak `S10` client-credentials fallback)并可调 `GET /api/governance/indicators`;live 环境已证实服务认证 200。
- [x] `IndicatorCatalogSyncService` 启动即拉一次、之后定时(~1h)刷新;只取 `status=已发布`,分页全量拉齐,逐指标记录 `version`。
- [x] 同步结果以 `IndicatorCatalogEntry` 模型缓存进语义层供 F3 匹配;同步失败时保留上次缓存(stale-while-revalidate)不清空。
- [x] `PlatformIndicatorClient` 提供 `getDashboard/getDetail/drilldown`,平台不可达/超时返回**显式降级结果**(非异常、非静默假数据),供前端退回/提示。
- [x] analytics 暴露 `/api/platform/indicators*` BFF 端点,供 F2 通过 `/api/analytics/platform/indicators*` 调用。
- [x] 所有跨服务超时/降级有日志;密钥不硬编码(走 `DTS_PLATFORM_*` 环境变量配置)。
- [x] 单元测试覆盖 token、目录解析、取值降级分支;`it/README.md` 已记录本地 mock/contract 与 live 服务认证证据。
