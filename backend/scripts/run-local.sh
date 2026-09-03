#!/usr/bin/env bash
# ============================================================
# 本地一键启动（方案 A：自动加载 .env + 打包 + java -jar 启动）
#
# 用法（在 backend 目录下执行）：
#   ./scripts/run-local.sh gateway          # 网关（8888）
#   ./scripts/run-local.sh cas              # 预约服务 CAS（18080）
#   ./scripts/run-local.sh kb               # 知识库 KB（8081）
#
# 说明：
#   - 每次先加载 backend/.env，再自动 mvn package（-DskipTests），最后 java -jar
#   - 想跳过打包直接跑（代码没改时更快）：先 --fast 参数
#   - 只改了配置、没改 Java 代码时，用 --fast 可省打包时间
# ============================================================

set -e
cd "$(dirname "$0")/.."          # 切到 backend/
source scripts/load-env.sh >/dev/null

SERVICE="${1:-}"
FAST="no"
[ "${SERVICE}" = "--fast" ] && { FAST="yes"; SERVICE="${2:-}"; }
[ "${SERVICE}" = "-fast" ] && { FAST="yes"; SERVICE="${2:-}"; }

# 各服务的 maven 模块路径 + 产出 jar
MODULE_PATH=""
JAR=""
case "${SERVICE}" in
  gateway) MODULE_PATH="gateway";                       JAR="gateway/target/gateway-1.0.0.jar" ;;
  cas)     MODULE_PATH="cas-service/cas-server";        JAR="cas-service/cas-server/target/cas-server-1.0.0.jar" ;;
  kb)      MODULE_PATH="kb-service";                    JAR="kb-service/target/kb-service-1.0.0.jar" ;;
  *)
    echo "用法: $0 [--fast] <gateway|cas|kb>"
    echo "  gateway  网关    :8888   gateway/target/gateway-1.0.0.jar"
    echo "  cas      预约    :18080  cas-service/cas-server/target/cas-server-1.0.0.jar"
    echo "  kb       知识库  :8081   kb-service/target/kb-service-1.0.0.jar"
    echo "  --fast 跳过打包，直接用已有 jar（改过 Java 代码则别用）"
    exit 1
    ;;
esac

# 打包（除非 --fast 且 jar 已存在）
if [ "${FAST}" = "yes" ] && [ -f "${JAR}" ]; then
  echo "▶ 跳过打包（--fast），直接用: ${JAR}"
else
  echo "▶ 打包 ${MODULE_PATH} ..."
  mvn -q -B -DskipTests -pl "${MODULE_PATH}" -am package
fi

[ -f "${JAR}" ] || { echo "✗ 找不到 ${JAR}，请去掉 --fast 重新打包"; exit 1; }

echo "▶ 启动 ${SERVICE}（端口见上，Ctrl+C 停止）..."
exec java -jar "${JAR}"
