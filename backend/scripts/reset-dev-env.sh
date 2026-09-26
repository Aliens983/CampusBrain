#!/usr/bin/env bash
#
# reset-dev-env.sh —— 开发环境一键重置（效果等同于在一台全新机器上启动）
#
# 清空内容：
#   1. MySQL：cas_db / knowledge_base / cas_it 三个开发库 DROP + 重建（flyway_schema_history 随之消失）
#      → 下次应用启动时 Flyway 执行 V1 全量基线 + V2/V3 种子，表结构自动重建
#   2. Redis：kb-redis / cas-redis 执行 FLUSHALL（验证码、限流、缓存、在途幂等键全清）
#   3. Qdrant：删除全部 collection（kb_chunks / qa_semantic_cache），KB 启动时自动重建
#   4. Elasticsearch：删除 kb_* 索引，KB 启动时自动重建
#
# 不处理（如需连队列/Nacos 配置一并清空，用 docker compose down -v 删数据卷）：
#   RabbitMQ 消息、Nacos 配置、MinIO 文件
#
# 特性：容器未运行的组件自动跳过；默认需输入 yes 确认，-y 跳过确认；
#       --mysql-only 只清 MySQL。
#
# 用法：
#   ./scripts/reset-dev-env.sh           # 交互式确认
#   ./scripts/reset-dev-env.sh -y        # 直接执行
#   ./scripts/reset-dev-env.sh --mysql-only -y
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../.env"

ASSUME_YES=0
MYSQL_ONLY=0
for arg in "$@"; do
    case "$arg" in
        -y|--yes) ASSUME_YES=1 ;;
        --mysql-only) MYSQL_ONLY=1 ;;
        *) echo "未知参数: $arg" >&2; exit 2 ;;
    esac
done

# ---------- 加载 .env（与 docker compose 同一份配置） ----------
if [[ -f "$ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
fi
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-}"
KB_MYSQL_ROOT_PASSWORD="${KB_MYSQL_ROOT_PASSWORD:-}"
KB_REDIS_PASSWORD="${KB_REDIS_PASSWORD:-}"
CAS_REDIS_PASSWORD="${CAS_REDIS_PASSWORD:-}"
QDRANT_API_KEY="${QDRANT_API_KEY:-}"

# 组件访问坐标（默认走 127.0.0.1 回环映射端口，可用环境变量覆盖）
QDRANT_BASE="${QDRANT_BASE:-http://127.0.0.1:6333}"
ES_BASE="${ES_BASE:-http://127.0.0.1:9200}"

c_yellow=$'\033[33m'; c_green=$'\033[32m'; c_red=$'\033[31m'; c_reset=$'\033[0m'
info()  { echo "${c_green}[reset]${c_reset} $*"; }
warn()  { echo "${c_yellow}[skip]${c_reset} $*"; }
fatal() { echo "${c_red}[error]${c_reset} $*" >&2; exit 1; }

container_running() { docker inspect -f '{{.State.Running}}' "$1" 2>/dev/null | grep -qx true; }

# ---------- 确认 ----------
echo "${c_yellow}即将清空开发环境的全部业务数据（MySQL 库将被 DROP 重建）：${c_reset}"
echo "  - MySQL: cas-mysql/cas_db, kb-mysql/knowledge_base, cas-it-mysql/cas_it"
if [[ "$MYSQL_ONLY" -eq 0 ]]; then
    echo "  - Redis: kb-redis, cas-redis (FLUSHALL)"
    echo "  - Qdrant @ $QDRANT_BASE（删除全部 collection）"
    echo "  - Elasticsearch @ $ES_BASE（删除 kb_* 索引）"
fi
if [[ "$ASSUME_YES" -ne 1 ]]; then
    read -r -p "确认重置请输入 yes: " answer
    [[ "$answer" == "yes" ]] || fatal "已取消"
fi

# ---------- 1. MySQL ----------
# 参数：容器名 root密码 库名
reset_mysql_db() {
    local container="$1" password="$2" dbname="$3"
    if ! container_running "$container"; then
        warn "$container 未运行，跳过库 $dbname"
        return
    fi
    info "重置 MySQL: $container/$dbname"
    docker exec -i "$container" mysql -uroot ${password:+-p"$password"} \
        --default-character-set=utf8mb4 -e "
            DROP DATABASE IF EXISTS \`$dbname\`;
            CREATE DATABASE \`$dbname\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
        "
}

reset_mysql_db cas-mysql      "$MYSQL_ROOT_PASSWORD"    cas_db
reset_mysql_db kb-mysql       "$KB_MYSQL_ROOT_PASSWORD" knowledge_base
reset_mysql_db cas-it-mysql   root                      cas_it

if [[ "$MYSQL_ONLY" -eq 1 ]]; then
    info "仅重置 MySQL 完成（--mysql-only）。重启应用后 Flyway 自动全量建表。"
    exit 0
fi

# ---------- 2. Redis ----------
flush_redis() {
    local container="$1" password="$2"
    if ! container_running "$container"; then
        warn "$container 未运行，跳过"
        return
    fi
    info "清空 Redis: $container (FLUSHALL)"
    docker exec "$container" redis-cli ${password:+-a "$password"} FLUSHALL >/dev/null
}

flush_redis kb-redis  "$KB_REDIS_PASSWORD"
flush_redis cas-redis "$CAS_REDIS_PASSWORD"

# ---------- 3. Qdrant（宿主机 curl 打回环端口；qdrant 镜像内无 curl/wget） ----------
if curl -fsS --max-time 3 "${QDRANT_BASE}/readyz" >/dev/null 2>&1; then
    auth_header=()
    [[ -n "$QDRANT_API_KEY" ]] && auth_header=(-H "api-key: $QDRANT_API_KEY")
    # 不依赖 jq：collection 名仅含字母/数字/下划线，直接正则提取
    collections="$(curl -fsS "${auth_header[@]}" "${QDRANT_BASE}/collections" \
        | grep -oE '"name"[[:space:]]*:[[:space:]]*"[^"]+"' | grep -oE '"[^"]+"$' | tr -d '"' || true)"
    if [[ -z "$collections" ]]; then
        info "Qdrant 无 collection，跳过"
    else
        while IFS= read -r name; do
            [[ -z "$name" ]] && continue
            info "删除 Qdrant collection: $name"
            curl -fsS -X DELETE "${auth_header[@]}" "${QDRANT_BASE}/collections/${name}" >/dev/null
        done <<< "$collections"
    fi
else
    warn "Qdrant @ $QDRANT_BASE 不可达，跳过"
fi

# ---------- 4. Elasticsearch（只删 kb_ 前缀业务索引） ----------
if curl -fsS --max-time 3 "${ES_BASE}/_cat/indices?h=index" >/dev/null 2>&1; then
    kb_indices="$(curl -fsS "${ES_BASE}/_cat/indices/kb_*?h=index" 2>/dev/null || true)"
    if [[ -z "$kb_indices" ]]; then
        info "Elasticsearch 无 kb_* 索引，跳过"
    else
        while IFS= read -r idx; do
            [[ -z "$idx" ]] && continue
            info "删除 ES 索引: $idx"
            curl -fsS -X DELETE "${ES_BASE}/${idx}" >/dev/null
        done <<< "$kb_indices"
    fi
else
    warn "Elasticsearch @ $ES_BASE 不可达，跳过"
fi

info "重置完成。重启 cas-server / kb-service 后：Flyway 全量建表 + 种子数据，Qdrant collection / ES 索引由应用自动重建。"
