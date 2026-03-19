#!/usr/bin/env bash
#
# dts-copilot 一键启动脚本（生产模式）
# 用法: ./start.sh [--skip-build] [--no-tls] [--pull-model]
#
set -euo pipefail

cd "$(dirname "$0")"

# ── 加载镜像版本 ──────────────────────────────────────────
if [[ -f imgversion.conf ]]; then
  set -a
  source imgversion.conf
  set +a
fi

# ── 加载运行配置 ──────────────────────────────────────────
if [[ -f .env ]]; then
  set -a
  source .env
  set +a
fi

# ── 参数解析 ──────────────────────────────────────────────
SKIP_BUILD=false
NO_TLS=false
PULL_MODEL=false

for arg in "$@"; do
  case "$arg" in
    --skip-build)  SKIP_BUILD=true ;;
    --no-tls)      NO_TLS=true ;;
    --pull-model)  PULL_MODEL=true ;;
    --help|-h)
      echo "用法: ./start.sh [选项]"
      echo ""
      echo "选项:"
      echo "  --skip-build   跳过 Maven 编译和前端构建（使用已有构建产物）"
      echo "  --no-tls       不启动 Traefik，直接通过内部端口访问"
      echo "  --pull-model   启动后自动拉取默认 LLM 模型 (qwen2.5-coder:7b)"
      echo "  -h, --help     显示帮助"
      exit 0
      ;;
    *) echo "未知参数: $arg (使用 --help 查看帮助)"; exit 1 ;;
  esac
done

# ── 颜色输出 ──────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

step()  { echo -e "\n${CYAN}[$(date +%H:%M:%S)]${NC} ${GREEN}$1${NC}"; }
warn()  { echo -e "${YELLOW}  ⚠ $1${NC}"; }
fail()  { echo -e "${RED}  ✗ $1${NC}"; }
ok()    { echo -e "${GREEN}  ✓ $1${NC}"; }

echo -e "${CYAN}╔═══════════════════════════════════════╗${NC}"
echo -e "${CYAN}║     DTS Copilot - 一键启动            ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════╝${NC}"

# ── 1. 环境检查 ───────────────────────────────────────────
step "检查运行环境..."

command -v docker >/dev/null 2>&1 || { fail "未安装 Docker"; exit 1; }
docker compose version >/dev/null 2>&1 || { fail "未安装 Docker Compose V2"; exit 1; }
ok "Docker $(docker --version | grep -oP '\d+\.\d+\.\d+')"

# ── 2. 生成 HTTPS 证书 ───────────────────────────────────
if [[ "$NO_TLS" == false ]]; then
  step "检查 HTTPS 证书..."
  if [[ ! -f services/certs/server.crt || ! -f services/certs/server.key ]]; then
    bash services/certs/gen-certs.sh
    ok "证书已生成"
  else
    ok "证书已存在，跳过生成"
  fi
fi

# ── 3. 编译打包 ───────────────────────────────────────────
if [[ "$SKIP_BUILD" == false ]]; then
  step "编译 Java 模块..."
  if command -v mvn >/dev/null 2>&1; then
    mvn clean package -DskipTests -q
    ok "Maven 编译完成"
  elif [[ -f ./mvnw ]]; then
    ./mvnw clean package -DskipTests -q
    ok "Maven Wrapper 编译完成"
  else
    warn "未找到 Maven，跳过编译（确保 target/*.jar 已存在）"
  fi

  step "检查前端构建产物..."
  if [[ -d dts-copilot-webapp/dist ]]; then
    ok "前端已构建"
  elif [[ -f dts-copilot-webapp/package.json ]]; then
    if command -v pnpm >/dev/null 2>&1; then
      (cd dts-copilot-webapp && pnpm install --frozen-lockfile 2>/dev/null || pnpm install && pnpm build)
      ok "前端构建完成"
    elif command -v npm >/dev/null 2>&1; then
      (cd dts-copilot-webapp && npm install && npm run build)
      ok "前端构建完成 (npm)"
    else
      warn "未找到 pnpm/npm，跳过前端构建"
    fi
  fi
else
  step "跳过编译（--skip-build）"
fi

# ── 4. 构建 Docker 镜像 ──────────────────────────────────
step "构建 Docker 镜像..."
if [[ "$NO_TLS" == true ]]; then
  docker compose build copilot-ai copilot-analytics 2>&1 | tail -5
else
  docker compose build 2>&1 | tail -5
fi
ok "镜像构建完成"

# ── 5. 启动服务 ───────────────────────────────────────────
step "启动所有服务..."
COMPOSE_PROFILES=""
if [[ "${LLM_PROVIDER:-}" == "ollama" ]]; then
  COMPOSE_PROFILES="ollama"
fi

if [[ "$NO_TLS" == true ]]; then
  COMPOSE_PROFILES="${COMPOSE_PROFILES}" docker compose up -d copilot-postgres copilot-ai copilot-analytics ${LLM_PROVIDER:+copilot-ollama}
else
  COMPOSE_PROFILES="${COMPOSE_PROFILES}" docker compose --profile "${COMPOSE_PROFILES:-default}" up -d
fi
ok "容器已启动"

# ── 6. 等待健康检查 ───────────────────────────────────────
step "等待服务就绪..."

wait_for() {
  local name="$1" url="$2" max_wait="${3:-90}"
  local elapsed=0
  while [[ $elapsed -lt $max_wait ]]; do
    if curl -fsS "$url" >/dev/null 2>&1; then
      ok "$name 就绪 (${elapsed}s)"
      return 0
    fi
    sleep 3
    elapsed=$((elapsed + 3))
    printf "."
  done
  echo ""
  fail "$name 未就绪（超时 ${max_wait}s）"
  return 1
}

wait_for "PostgreSQL" "http://localhost:5432" 30 2>/dev/null || true  # pg_isready 不是 HTTP
# 用 docker 检查 postgres
for i in $(seq 1 20); do
  if docker exec dts-copilot-postgres pg_isready -U copilot -d copilot >/dev/null 2>&1; then
    ok "PostgreSQL 就绪"
    break
  fi
  [[ $i -eq 20 ]] && fail "PostgreSQL 未就绪"
  sleep 2
done

if [[ "${LLM_PROVIDER:-}" == "ollama" ]]; then
  wait_for "Ollama" "http://localhost:11434/api/tags" 60
fi
wait_for "copilot-ai"        "http://localhost:8091/actuator/health" 90
wait_for "copilot-analytics"  "http://localhost:8092/api/health" 90

# ── 7. 拉取 LLM 模型（仅 Ollama 模式）────────────────────
if [[ "$PULL_MODEL" == true && "${LLM_PROVIDER:-}" == "ollama" ]]; then
  step "拉取默认 LLM 模型..."
  MODEL="${OLLAMA_DEFAULT_MODEL:-qwen2.5-coder:7b}"
  if docker exec dts-copilot-ollama ollama list 2>/dev/null | grep -q "${MODEL%%:*}"; then
    ok "模型 $MODEL 已存在"
  else
    warn "正在拉取 $MODEL（可能需要几分钟）..."
    docker exec dts-copilot-ollama ollama pull "$MODEL"
    ok "模型拉取完成"
  fi
elif [[ "$PULL_MODEL" == true ]]; then
  info "当前使用公有云 LLM (${LLM_PROVIDER:-deepseek})，无需拉取模型"
fi

# ── 8. 打印访问信息 ───────────────────────────────────────
echo ""
echo -e "${CYAN}╔═══════════════════════════════════════╗${NC}"
echo -e "${CYAN}║     DTS Copilot 启动完成！            ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════╝${NC}"
echo ""
echo -e "  ${GREEN}服务状态:${NC}"
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || docker compose ps
echo ""
echo -e "  ${GREEN}访问地址:${NC}"
if [[ "$NO_TLS" == true ]]; then
  echo "    copilot-ai        http://localhost:8091"
  echo "    copilot-analytics  http://localhost:8092"
  echo "    Ollama            http://localhost:11434"
else
  echo "    HTTPS 入口         https://copilot.local  (需添加 hosts: 127.0.0.1 copilot.local)"
  echo "    copilot-ai        http://localhost:8091   (内部直连)"
  echo "    copilot-analytics  http://localhost:8092   (内部直连)"
  echo "    Ollama            http://localhost:11434"
fi
echo ""
echo -e "  ${GREEN}下一步:${NC}"
echo "    1. 生成 API Key:"
echo "       curl -X POST http://localhost:8091/api/auth/keys \\"
echo "         -H 'X-Admin-Secret: change-me-in-production' \\"
echo "         -H 'Content-Type: application/json' \\"
echo "         -d '{\"name\": \"my-app\"}'"
echo ""
if [[ "$PULL_MODEL" == false ]]; then
  echo "    2. 拉取 LLM 模型（首次需要）:"
  echo "       docker exec dts-copilot-ollama ollama pull qwen2.5-coder:7b"
  echo "       或重新运行: ./start.sh --skip-build --pull-model"
  echo ""
fi
echo -e "  ${GREEN}停止服务:${NC}"
echo "    docker compose down"
echo ""
