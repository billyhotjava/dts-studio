# SC-08: CI 构建脚本与冒烟验证

**状态**: READY
**依赖**: SC-01~07

## 目标

创建统一的构建和冒烟测试脚本，验证项目骨架的完整性。

## 技术设计

### 构建脚本

```bash
#!/bin/bash
# build.sh — 编译并打包所有模块
set -euo pipefail

cd "$(dirname "$0")"

echo "=== Building dts-copilot ==="
mvn clean package -DskipTests

echo "=== Building Docker images ==="
docker compose build

echo "=== Build complete ==="
```

### 冒烟测试脚本

```bash
#!/bin/bash
# smoke-test.sh — 启动服务并验证健康状态
set -euo pipefail

docker compose up -d
sleep 30  # 等待服务启动

# 证书生成（如尚未生成）
bash services/certs/gen-certs.sh

# 健康检查（内部 HTTP）
curl -fsS http://localhost:8091/actuator/health || exit 1
curl -fsS http://localhost:8092/api/health || exit 1
curl -fsS http://localhost:11434/api/tags || exit 1

# HTTPS 验证（通过 Traefik）
curl --cacert services/certs/ca.crt -fsS https://copilot.local/api/ai/health || exit 1
curl --cacert services/certs/ca.crt -fsS https://copilot.local/analytics/ || exit 1

# 数据库 schema 验证
docker exec dts-copilot-postgres psql -U copilot -d copilot \
    -c "SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'copilot_%'" \
    | grep -q copilot_ai || exit 1

echo "=== Smoke test passed ==="
docker compose down
```

## 影响文件

- `dts-copilot/build.sh`（新建）
- `dts-copilot/smoke-test.sh`（新建）

## 完成标准

- [ ] `./build.sh` 编译通过，Docker 镜像构建成功
- [ ] `./smoke-test.sh` 全部检查通过
- [ ] 脚本幂等，可重复执行
