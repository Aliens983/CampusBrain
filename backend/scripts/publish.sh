#!/usr/bin/env bash
# ============================================================
# 本地一键发布：push 到 GitHub main 分支
# Jenkins（campusbrain-deploy）直接轮询 GitHub，约 2 分钟内发现
# 新提交后自动构建并部署到服务器 /opt/campusbrain → 无需手动碰服务器。
#
# 用法：  bash backend/scripts/publish.sh [分支名，默认 main]
# ============================================================
set -euo pipefail
REPO="$(git -C "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)" rev-parse --show-toplevel)"
cd "${REPO}"

BRANCH="${1:-main}"

git status -sb | head -1
test -z "$(git status --porcelain)" || { echo "⚠️  有未提交改动，先 commit 再发。" >&2; exit 1; }

echo "── push GitHub origin/${BRANCH}（带重试）──"
for i in 1 2 3; do
  if git push origin "${BRANCH}" 2>&1 | tail -2; then break; fi
  echo "  GitHub 不稳，重试 ${i}..."; sleep 3
done

echo ""
echo "✅ 已推送。Jenkins 将在 ~2 分钟内自动发版（或手动：服务器 Jenkins → Build Now）。"
echo "   进度：http://100.94.115.52:8080（Tailscale）→ 任务 campusbrain-deploy"
