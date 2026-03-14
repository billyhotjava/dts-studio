# AK-01: API Key 数据模型与管理服务

**状态**: READY
**依赖**: SC-02

## 目标

实现 API Key 的数据模型、生成算法、CRUD 管理服务和 REST API。

## 技术设计

### API Key 格式

```
cpk_<32字符随机字符串>
例: cpk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
```

- 前缀 `cpk_`（copilot key）便于识别
- 数据库只存储 SHA-256 哈希值，原始 Key 仅在创建时返回一次
- 保留前 8 字符用于展示（`key_prefix`）

### 数据模型（扩展 SC-02 的骨架表）

```sql
ALTER TABLE copilot_ai.api_key ADD COLUMN description TEXT;
ALTER TABLE copilot_ai.api_key ADD COLUMN ip_whitelist TEXT[];
ALTER TABLE copilot_ai.api_key ADD COLUMN rate_limit INTEGER DEFAULT 1000;  -- 每小时
ALTER TABLE copilot_ai.api_key ADD COLUMN created_by VARCHAR(128);
ALTER TABLE copilot_ai.api_key ADD COLUMN last_used_at TIMESTAMP;
ALTER TABLE copilot_ai.api_key ADD COLUMN usage_count BIGINT DEFAULT 0;
```

### REST API

```
POST   /api/auth/keys              — 生成新 Key（需 Admin Secret）
GET    /api/auth/keys              — 列出所有 Key（脱敏）
GET    /api/auth/keys/{id}         — Key 详情
DELETE /api/auth/keys/{id}         — 吊销 Key
PUT    /api/auth/keys/{id}/rotate  — 轮换 Key
```

### 管理员认证

初始管理通过环境变量 `COPILOT_ADMIN_SECRET` 配置，使用 `X-Admin-Secret` 头认证。

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/domain/ApiKey.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/repository/ApiKeyRepository.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/auth/ApiKeyService.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/ApiKeyResource.java`（新建）
- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_005__api_key_extend.xml`（新建）

## 完成标准

- [ ] API Key 生成格式正确（`cpk_` 前缀）
- [ ] 数据库只存储哈希值
- [ ] CRUD API 正常工作
- [ ] Key 吊销后不可使用
- [ ] Key 轮换生成新 Key 并废弃旧 Key
