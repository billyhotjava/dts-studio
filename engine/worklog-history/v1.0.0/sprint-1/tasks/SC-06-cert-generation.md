# SC-06: 证书生成脚本与本地 CA

**状态**: READY
**依赖**: 无

## 目标

建立 dts-copilot 的证书管理体系，提供自动化证书生成脚本，支持开发自签名和生产外部证书两种模式。复用 dts-stack 的 gen-certs.sh 方案并适配。

## 技术设计

### 证书管理架构

```
services/certs/
├── gen-certs.sh           — 证书生成脚本（从 dts-stack 适配）
├── ca.key                 — 本地 CA 私钥（自动生成，gitignore）
├── ca.crt                 — 本地 CA 证书（自动生成）
├── server.key             — 服务器私钥（自动生成，gitignore）
├── server.crt             — 服务器证书完整链（server + CA）
├── server.only.crt        — 服务器证书（不含 CA）
├── server.p12             — PKCS#12 格式（Java 应用使用）
├── truststore.p12         — Java truststore（信任本地 CA）
└── .gitignore             — 排除私钥和生成物
```

### 两种模式

**开发模式（自签名）**：
```bash
# 自动生成本地 CA + 通配符证书
BASE_DOMAIN=copilot.local bash services/certs/gen-certs.sh
```

生成 `*.copilot.local` 通配符证书，开发者在 `/etc/hosts` 中添加：
```
127.0.0.1  copilot.local
```

**生产模式（外部证书）**：
```bash
# 将真实证书放到 services/certs/ 目录即可
cp /path/to/real/server.crt services/certs/server.crt
cp /path/to/real/server.key services/certs/server.key
```

脚本检测到已有合法证书时跳过生成。

### gen-certs.sh 核心逻辑（从 dts-stack 适配）

1. **检查现有证书**：如果 `server.crt` 和 `server.key` 存在且有效（SAN 匹配、密钥对匹配），跳过生成
2. **生成本地 CA**：RSA 2048，10 年有效期，带 CA 扩展
3. **生成服务器证书**：RSA 2048，825 天有效期，SAN 包含 `*.${BASE_DOMAIN}` 和 `${BASE_DOMAIN}`
4. **生成 PKCS#12**：`server.p12`，密码可配置（默认 `changeit`）
5. **生成 truststore**：`truststore.p12`，导入本地 CA，供 Java 服务间 HTTPS 通信
6. **幂等性**：重复运行不会覆盖有效证书

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `BASE_DOMAIN` | `copilot.local` | 证书域名，通配符为 `*.copilot.local` |
| `P12_PASSWORD` | `changeit` | PKCS#12 密码 |
| `TRUSTSTORE_PASSWORD` | `changeit` | Java truststore 密码 |
| `SUBJECT_O` | `DTS-Copilot` | 证书 Organization 字段 |

### .gitignore

```
# 私钥和生成物不提交
services/certs/*.key
services/certs/*.crt
services/certs/*.p12
services/certs/*.jks
# 但保留脚本
!services/certs/gen-certs.sh
!services/certs/.gitignore
```

### 开发者体验

```bash
# 首次设置（一次性）
echo "127.0.0.1 copilot.local" | sudo tee -a /etc/hosts
cd dts-copilot && bash services/certs/gen-certs.sh

# 信任本地 CA（macOS）
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain services/certs/ca.crt

# 信任本地 CA（Ubuntu）
sudo cp services/certs/ca.crt /usr/local/share/ca-certificates/copilot-ca.crt
sudo update-ca-certificates

# 之后浏览器访问 https://copilot.local 无警告
```

## 影响文件

- `dts-copilot/services/certs/gen-certs.sh`（新建，从 dts-stack 适配）
- `dts-copilot/services/certs/.gitignore`（新建）
- `dts-copilot/.env`（修改：添加证书相关环境变量）

## 完成标准

- [ ] `gen-certs.sh` 执行成功，生成完整证书链
- [ ] 生成的证书 SAN 包含 `*.copilot.local` 和 `copilot.local`
- [ ] `server.p12` 和 `truststore.p12` 格式正确
- [ ] 脚本幂等：已有合法证书时跳过，仅补充缺失的 truststore
- [ ] 生产模式：放入外部证书后脚本识别并跳过生成
- [ ] 私钥和证书文件被 .gitignore 排除
- [ ] openssl verify 验证证书链完整
