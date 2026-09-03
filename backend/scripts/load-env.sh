#!/usr/bin/env bash
# ============================================================
# 本地开发环境加载脚本（方案 A：配置全环境变量化后使用）
#
# 用法：
#   cd backend && source scripts/load-env.sh
#   之后再执行 mvn / java -jar / npm 等，进程即可读到 .env 里的变量
#
# 原理：把 backend/.env 里所有变量 export 到当前 shell。
# .env 已被 gitignore，只存在于本地/服务器，不进仓库。
# ============================================================

set -a
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../.env"

if [ -f "${ENV_FILE}" ]; then
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  echo "✅ 已加载 $(grep -cE '^[A-Z_]+=.' "${ENV_FILE}" 2>/dev/null || echo 0) 个变量 ← ${ENV_FILE}"
else
  echo "⚠️  未找到 ${ENV_FILE}"
  echo "   请先执行: cp backend/.env.example backend/.env  并填入真实值"
fi
set +a
