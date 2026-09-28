#!/usr/bin/env bash
set -euo pipefail

# ========================================
# 注册园林平台 MySQL 数据源到 dts-copilot
# ========================================
# 使用前请确保：
# 1. dts-copilot-ai 已启动
# 2. 已生成 API Key
# 3. 园林平台 MySQL 可从 copilot 网络访问

COPILOT_AI_URL="${COPILOT_AI_URL:-http://localhost:8091}"
API_KEY="${API_KEY:?请设置 API_KEY 环境变量}"
GARDEN_DB_HOST="${GARDEN_DB_HOST:-localhost}"
GARDEN_DB_PORT="${GARDEN_DB_PORT:-3306}"
GARDEN_DB_NAME="${GARDEN_DB_NAME:-flowers}"
GARDEN_DB_USER="${GARDEN_DB_USER:-copilot_reader}"
GARDEN_DB_PASSWORD="${GARDEN_DB_PASSWORD:?请设置 GARDEN_DB_PASSWORD 环境变量}"

echo "=== 注册园林平台数据源 ==="
echo "Copilot AI: $COPILOT_AI_URL"
echo "Garden DB:  $GARDEN_DB_HOST:$GARDEN_DB_PORT/$GARDEN_DB_NAME"
echo ""

# Register data source
response=$(curl -s -w "\n%{http_code}" -X POST "$COPILOT_AI_URL/api/datasources" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"馨懿诚园林业务库\",
    \"dbType\": \"mysql\",
    \"jdbcUrl\": \"jdbc:mysql://$GARDEN_DB_HOST:$GARDEN_DB_PORT/$GARDEN_DB_NAME?useUnicode=true&characterEncoding=utf8&useSSL=false\",
    \"username\": \"$GARDEN_DB_USER\",
    \"password\": \"$GARDEN_DB_PASSWORD\"
  }")

http_code=$(echo "$response" | tail -1)
body=$(echo "$response" | sed '$d')

echo "HTTP Status: $http_code"
echo "$body" | python3 -m json.tool 2>/dev/null || echo "$body"

if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
  echo ""
  echo "=== 数据源注册成功 ==="
else
  echo ""
  echo "=== 数据源注册失败，请检查配置 ==="
  exit 1
fi

echo ""
echo "建议创建只读 MySQL 用户："
echo "  CREATE USER 'copilot_reader'@'%' IDENTIFIED BY 'your_password';"
echo "  GRANT SELECT ON $GARDEN_DB_NAME.* TO 'copilot_reader'@'%';"
echo "  FLUSH PRIVILEGES;"
