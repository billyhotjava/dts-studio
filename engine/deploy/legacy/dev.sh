#!/usr/bin/env bash
#
# dts-copilot 一键开发脚本
# 启动基础设施容器（PostgreSQL + Ollama），然后在本地启动 Java 服务和前端
#
# 用法: ./dev.sh [命令]
#   ./dev.sh          启动全部（基础设施 + 后端 + 前端）
#   ./dev.sh infra    仅启动基础设施容器
#   ./dev.sh backend  仅启动两个 Java 后端（需基础设施已运行）
#   ./dev.sh frontend 仅启动前端开发服务器（需后端已运行）
#   ./dev.sh stop     停止所有服务
#   ./dev.sh status   查看服务状态
#   ./dev.sh db       连接到 PostgreSQL 命令行
#   ./dev.sh logs     查看后端日志
#   ./dev.sh clean    停止并清除所有数据卷
#
set -euo pipefail

cd "$(dirname "$0")"

# ── 加载镜像版本 ──────────────────────────────────────────
if [[ -f imgversion.conf ]]; then
  set -a
  source imgversion.conf
  set +a
fi

# ── 颜色 ──────────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
NC='\033[0m'

step()  { echo -e "\n${CYAN}[$(date +%H:%M:%S)]${NC} ${GREEN}$1${NC}"; }
warn()  { echo -e "${YELLOW}  ⚠ $1${NC}"; }
fail()  { echo -e "${RED}  ✗ $1${NC}"; exit 1; }
ok()    { echo -e "${GREEN}  ✓ $1${NC}"; }
info()  { echo -e "${GRAY}  $1${NC}"; }

# ── 进程管理 ──────────────────────────────────────────────
ROOT_DIR="$(pwd)"
PID_DIR="$ROOT_DIR/.dev-pids"
mkdir -p "$PID_DIR"
LOG_DIR="$ROOT_DIR/.dev-logs"
mkdir -p "$LOG_DIR"

save_pid()  { echo "$2" > "$PID_DIR/$1.pid"; }
get_pid()   { [[ -f "$PID_DIR/$1.pid" ]] && cat "$PID_DIR/$1.pid" || echo ""; }
is_running() {
  local pid
  pid=$(get_pid "$1")
  [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null
}

# ── 基础设施 ──────────────────────────────────────────────
start_infra() {
  step "启动基础设施容器（PostgreSQL + Ollama）..."

  docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
  ok "容器已启动"

  # 等待 PostgreSQL
  step "等待 PostgreSQL 就绪..."
  for i in $(seq 1 30); do
    if docker exec dts-copilot-postgres pg_isready -U copilot -d copilot >/dev/null 2>&1; then
      ok "PostgreSQL 就绪"
      break
    fi
    [[ $i -eq 30 ]] && fail "PostgreSQL 启动超时"
    sleep 2
  done

  # Ollama 仅在 LLM_PROVIDER=ollama 时检查
  if [[ "${LLM_PROVIDER:-}" == "ollama" ]]; then
    step "等待 Ollama 就绪..."
    for i in $(seq 1 20); do
      if curl -fsS http://localhost:11434/api/tags >/dev/null 2>&1; then
        ok "Ollama 就绪"
        break
      fi
      [[ $i -eq 20 ]] && warn "Ollama 未就绪（AI 功能将不可用）"
      sleep 2
    done
  else
    info "LLM 使用公有云 (${LLM_PROVIDER:-deepseek})，跳过 Ollama"
  fi
}

# ── 后端 ──────────────────────────────────────────────────
start_backend() {
  step "启动 copilot-ai (端口 8091)..."

  # 检查 Maven
  local MVN="mvn"
  [[ -f ./mvnw ]] && MVN="./mvnw"
  command -v $MVN >/dev/null 2>&1 || fail "未找到 Maven，请安装 Maven 3.9+ 或创建 mvnw"

  # 编译
  step "编译 Java 模块..."
  $MVN clean compile -q -DskipTests 2>&1 || fail "编译失败"
  ok "编译完成"

  # 环境变量
  export PG_HOST=localhost
  export PG_PORT=5432
  export PG_DB=copilot
  export PG_USER=copilot
  export PG_PASSWORD=copilot_dev
  export OLLAMA_BASE_URL=http://localhost:11434
  export COPILOT_ADMIN_SECRET=change-me-in-production
  export COPILOT_AI_BASE_URL=http://localhost:8091

  # 启动 copilot-ai
  if is_running "copilot-ai"; then
    warn "copilot-ai 已在运行 (PID: $(get_pid copilot-ai))"
  else
    step "启动 copilot-ai..."
    $MVN -pl dts-copilot-ai spring-boot:run \
      -Dspring-boot.run.jvmArguments="-Xmx512m" \
      > "$LOG_DIR/copilot-ai.log" 2>&1 &
    save_pid "copilot-ai" $!
    ok "copilot-ai 启动中 (PID: $!, 日志: $LOG_DIR/copilot-ai.log)"
  fi

  # 等待 copilot-ai 就绪
  step "等待 copilot-ai 就绪..."
  for i in $(seq 1 60); do
    if curl -fsS http://localhost:8091/actuator/health >/dev/null 2>&1; then
      ok "copilot-ai 就绪 (${i}s)"
      break
    fi
    # 检查进程是否还活着
    if ! is_running "copilot-ai"; then
      fail "copilot-ai 启动失败，查看日志: tail -50 $LOG_DIR/copilot-ai.log"
    fi
    [[ $i -eq 60 ]] && { warn "copilot-ai 等待超时，查看日志: tail -50 $LOG_DIR/copilot-ai.log"; }
    sleep 2
  done

  # 启动 copilot-analytics
  if is_running "copilot-analytics"; then
    warn "copilot-analytics 已在运行 (PID: $(get_pid copilot-analytics))"
  else
    step "启动 copilot-analytics..."
    $MVN -pl dts-copilot-analytics spring-boot:run \
      -Dspring-boot.run.jvmArguments="-Xmx512m" \
      > "$LOG_DIR/copilot-analytics.log" 2>&1 &
    save_pid "copilot-analytics" $!
    ok "copilot-analytics 启动中 (PID: $!, 日志: $LOG_DIR/copilot-analytics.log)"
  fi

  # 等待 copilot-analytics 就绪
  step "等待 copilot-analytics 就绪..."
  for i in $(seq 1 60); do
    if curl -fsS http://localhost:8092/api/health >/dev/null 2>&1; then
      ok "copilot-analytics 就绪 (${i}s)"
      break
    fi
    if ! is_running "copilot-analytics"; then
      fail "copilot-analytics 启动失败，查看日志: tail -50 $LOG_DIR/copilot-analytics.log"
    fi
    [[ $i -eq 60 ]] && { warn "copilot-analytics 等待超时，查看日志: tail -50 $LOG_DIR/copilot-analytics.log"; }
    sleep 2
  done
}

# ── 前端 ──────────────────────────────────────────────────
start_frontend() {
  step "启动前端开发服务器 (端口 3003)..."

  if is_running "copilot-webapp"; then
    warn "前端已在运行 (PID: $(get_pid copilot-webapp))"
    return
  fi

  if [[ ! -d dts-copilot-webapp ]]; then
    warn "前端目录不存在，跳过"
    return
  fi

  cd dts-copilot-webapp

  # 安装依赖
  if [[ ! -d node_modules ]]; then
    step "安装前端依赖..."
    if command -v pnpm >/dev/null 2>&1; then
      pnpm install
    elif command -v npm >/dev/null 2>&1; then
      npm install
    else
      warn "未找到 pnpm/npm，跳过前端启动"
      cd ..
      return
    fi
    ok "依赖安装完成"
  fi

  # 启动 Vite dev server
  if command -v pnpm >/dev/null 2>&1; then
    pnpm dev > "$LOG_DIR/copilot-webapp.log" 2>&1 &
  else
    npm run dev > "$LOG_DIR/copilot-webapp.log" 2>&1 &
  fi
  save_pid "copilot-webapp" $!

  cd ..
  ok "前端启动中 (PID: $(get_pid copilot-webapp), http://localhost:3003)"
  info "Vite 自动代理: /api/ai → :8091, /api → :8092"
}

# ── 停止 ──────────────────────────────────────────────────
stop_all() {
  step "停止所有服务..."

  # 停止 Java 和前端进程
  for svc in copilot-ai copilot-analytics copilot-webapp; do
    local pid
    pid=$(get_pid "$svc")
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      # 杀掉进程组（包含 Maven 子进程）
      kill -- -"$pid" 2>/dev/null || kill "$pid" 2>/dev/null || true
      ok "已停止 $svc (PID: $pid)"
    fi
    rm -f "$PID_DIR/$svc.pid"
  done

  # 也清理可能残留的 Java 进程
  pkill -f "CopilotAiApplication" 2>/dev/null || true
  pkill -f "CopilotAnalyticsApplication" 2>/dev/null || true

  # 停止 Docker 容器
  docker compose -f docker-compose.yml -f docker-compose.dev.yml down 2>/dev/null || true
  ok "基础设施容器已停止"

  echo ""
  ok "所有服务已停止"
}

# ── 状态 ──────────────────────────────────────────────────
show_status() {
  echo -e "\n${CYAN}=== DTS Copilot 开发环境状态 ===${NC}\n"

  echo -e "${GREEN}Docker 容器:${NC}"
  docker compose -f docker-compose.yml -f docker-compose.dev.yml ps 2>/dev/null || echo "  (未运行)"
  echo ""

  echo -e "${GREEN}本地服务:${NC}"
  for svc in copilot-ai copilot-analytics copilot-webapp; do
    local pid
    pid=$(get_pid "$svc")
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      echo -e "  ${GREEN}●${NC} $svc (PID: $pid)"
    else
      echo -e "  ${RED}○${NC} $svc (未运行)"
    fi
  done
  echo ""

  echo -e "${GREEN}端点检查:${NC}"
  for check in "PostgreSQL|pg_isready via docker" \
               "Ollama|http://localhost:11434/api/tags" \
               "copilot-ai|http://localhost:8091/actuator/health" \
               "copilot-analytics|http://localhost:8092/api/health" \
               "webapp|http://localhost:3003"; do
    local name="${check%%|*}" url="${check##*|}"
    if [[ "$name" == "PostgreSQL" ]]; then
      if docker exec dts-copilot-postgres pg_isready -U copilot -d copilot >/dev/null 2>&1; then
        echo -e "  ${GREEN}✓${NC} $name"
      else
        echo -e "  ${RED}✗${NC} $name"
      fi
    elif curl -fsS "$url" >/dev/null 2>&1; then
      echo -e "  ${GREEN}✓${NC} $name ($url)"
    else
      echo -e "  ${RED}✗${NC} $name ($url)"
    fi
  done
}

# ── 数据库连接 ────────────────────────────────────────────
connect_db() {
  docker exec -it dts-copilot-postgres psql -U copilot -d copilot
}

# ── 查看日志 ──────────────────────────────────────────────
show_logs() {
  local svc="${1:-all}"
  if [[ "$svc" == "all" ]]; then
    echo -e "${CYAN}=== 可用日志 ===${NC}"
    ls -la "$LOG_DIR"/*.log 2>/dev/null || echo "  (无日志文件)"
    echo ""
    echo "用法: ./dev.sh logs [copilot-ai|copilot-analytics|copilot-webapp]"
  elif [[ -f "$LOG_DIR/$svc.log" ]]; then
    tail -f "$LOG_DIR/$svc.log"
  else
    fail "日志文件不存在: $LOG_DIR/$svc.log"
  fi
}

# ── 清理 ──────────────────────────────────────────────────
clean_all() {
  step "停止并清除所有数据..."
  stop_all
  docker compose -f docker-compose.yml -f docker-compose.dev.yml down -v 2>/dev/null || true
  rm -rf "$PID_DIR" "$LOG_DIR"
  ok "所有数据已清除（包括数据库卷）"
}

# ── 主入口 ────────────────────────────────────────────────
CMD="${1:-}"

case "$CMD" in
  infra)
    start_infra
    echo ""
    echo -e "${GREEN}基础设施已就绪。接下来可以:${NC}"
    echo "  - 在 IDE 中启动 CopilotAiApplication (端口 8091)"
    echo "  - 在 IDE 中启动 CopilotAnalyticsApplication (端口 8092)"
    echo "  - 环境变量: PG_HOST=localhost OLLAMA_BASE_URL=http://localhost:11434"
    ;;

  backend)
    start_backend
    ;;

  frontend)
    start_frontend
    ;;

  stop)
    stop_all
    ;;

  status)
    show_status
    ;;

  db)
    connect_db
    ;;

  logs)
    show_logs "${2:-all}"
    ;;

  clean)
    echo -e "${RED}⚠ 这将删除所有数据卷（包括数据库数据）。确认? [y/N]${NC}"
    read -r confirm
    [[ "$confirm" == "y" || "$confirm" == "Y" ]] && clean_all || echo "已取消"
    ;;

  --help|-h)
    echo "DTS Copilot 开发环境管理"
    echo ""
    echo "用法: ./dev.sh [命令]"
    echo ""
    echo "命令:"
    echo "  (无)       启动全部（基础设施 + 后端 + 前端）"
    echo "  infra      仅启动基础设施（PostgreSQL + Ollama）"
    echo "  backend    仅启动 Java 后端服务"
    echo "  frontend   仅启动前端开发服务器"
    echo "  stop       停止所有服务"
    echo "  status     查看服务状态"
    echo "  db         连接到 PostgreSQL 命令行"
    echo "  logs [svc] 查看日志 (copilot-ai / copilot-analytics / copilot-webapp)"
    echo "  clean      停止并清除所有数据卷"
    echo "  -h,--help  显示帮助"
    echo ""
    echo "常用流程:"
    echo "  # 全自动启动"
    echo "  ./dev.sh"
    echo ""
    echo "  # IDE 开发（只启动基础设施，Java 在 IDE 中 Debug）"
    echo "  ./dev.sh infra"
    echo ""
    echo "  # 查看 copilot-ai 日志"
    echo "  ./dev.sh logs copilot-ai"
    ;;

  "")
    echo -e "${CYAN}╔═══════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║   DTS Copilot - 开发环境一键启动      ║${NC}"
    echo -e "${CYAN}╚═══════════════════════════════════════╝${NC}"

    start_infra
    start_backend
    start_frontend

    echo ""
    echo -e "${CYAN}╔═══════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║   开发环境启动完成！                   ║${NC}"
    echo -e "${CYAN}╚═══════════════════════════════════════╝${NC}"
    echo ""
    echo -e "  ${GREEN}访问地址:${NC}"
    echo "    前端 (HMR)         http://localhost:3003"
    echo "    copilot-ai         http://localhost:8091"
    echo "    copilot-analytics   http://localhost:8092"
    echo "    PostgreSQL         localhost:5432 (copilot/copilot_dev)"
    echo "    Ollama             http://localhost:11434"
    echo ""
    echo -e "  ${GREEN}常用命令:${NC}"
    echo "    ./dev.sh status    查看状态"
    echo "    ./dev.sh logs copilot-ai   查看 AI 服务日志"
    echo "    ./dev.sh db        连接数据库"
    echo "    ./dev.sh stop      停止所有"
    echo ""
    ;;

  *)
    echo "未知命令: $CMD"
    echo "使用 ./dev.sh --help 查看帮助"
    exit 1
    ;;
esac
