#!/usr/bin/env bash
# ============================================================
# 本地一键发布：push GitHub + 推服务器镜像仓库
# 推送镜像后，服务器 Jenkins（campusbrain-deploy，约 2 分钟轮询）
# 会自动构建并部署到 /opt/campusbrain → 无需手动碰服务器。
#
# 用法：  bash backend/scripts/publish.sh
# ============================================================
set -euo pipefail
REPO="$(git -C "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)" rev-parse --show-toplevel)"
cd "${REPO}"

BRANCH="${1:-master}"
MIRROR="codingkingliu@100.94.115.52:git/CampusBrain.git"

git status -sb | head -1
test -z "$(git status --porcelain)" || { echo "⚠️  有未提交改动，先 commit 再发。" >&2; exit 1; }

echo "── 1) push GitHub origin/${BRANCH} ──"
for i in 1 2 3; do
  if git push origin "${BRANCH}" 2>&1 | tail -2; then break; fi
  echo "  GitHub 不稳，重试 ${i}..."; sleep 3
done

echo "── 2) push 服务器镜像 ${MIRROR} ──"
for i in 1 2 3; do
  if git push "${MIRROR}" "${BRANCH}" 2>&1 | tail -2; then break; fi
  echo "  镜像 push 重试 ${i}..."; sleep 3
done

echo ""
echo "✅ 已推送。Jenkins 将在 ~2 分钟内自动发版（或手动：服务器 Jenkins → Build Now）。"
