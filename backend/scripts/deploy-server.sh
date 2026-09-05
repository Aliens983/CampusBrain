#!/usr/bin/env bash
# ============================================================
# 服务器持续部署脚本（Jenkins / 手动一键发版共用）
#
# 前提：在 /opt/campusbrain 的 git 仓库内执行（代码已是最新），
#       且在 /opt/campusbrain/backend 目录下运行（compose 项目名/卷才正确）。
# 用法：  cd /opt/campusbrain/backend && bash scripts/deploy-server.sh
#
# 职责（幂等，HEAD 未变则直接退出）：
#   1. 判定相对上次部署的改动，得出要重建的服务集合
#   2. mvn 全量打包 backend（可靠；避免 cas-server 嵌套模块 -pl 漏编的坑）
#   3. 只 docker build 改动服务的镜像（frontend 仅当其源码改动才构建，需联网拉 npm）
#   4. TAG=deploy docker compose up -d 对应服务（复用 /opt 下的 .env）
#   5. 冒烟：容器无 Restarting / 前端与网关链路 200 / Nacos 三服务注册
#   6. 记录本次部署的 commit 到 .last-deploy-commit（gitignore）
# ============================================================
set -euo pipefail

# ---------- 定位与状态文件 ----------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND="$(cd "${SCRIPT_DIR}/.." && pwd)"           # /opt/campusbrain/backend
REPO="$(cd "${BACKEND}/.." && pwd)"                 # /opt/campusbrain
LAST_FILE="${BACKEND}/.last-deploy-commit"
cd "${BACKEND}"

HEAD=$(git -C "${REPO}" rev-parse HEAD)
PREV=$(cat "${LAST_FILE}" 2>/dev/null || echo "")

echo "═══ deploy-server ═══"
echo "repo HEAD : ${HEAD}"
echo "上次部署  : ${PREV:-<首次/无>}"

# 幂等：HEAD 未变且已有部署记录 → 无事可做
if [ -n "${PREV}" ] && [ "${PREV}" = "${HEAD}" ]; then
  echo "✅ HEAD 未变（已部署此 commit），跳过。"
  exit 0
fi

# ---------- 判定改动 → 服务集合 ----------
CHANGED=""
if [ -n "${PREV}" ] && git -C "${REPO}" rev-parse --verify --quiet "${PREV}" >/dev/null 2>&1; then
  CHANGED=$(git -C "${REPO}" diff --name-only "${PREV}" "${HEAD}" || true)
else
  CHANGED="<全量>"   # 首次：全量部署
fi
echo "改动文件数: ${CHANGED}"

svc() { # svc <set_name> <path_pattern>
  local name="$1" pat="$2"
  if [ "${CHANGED}" = "<全量>" ] || printf '%s\n' "${CHANGED}" | grep -qE "${pat}"; then
    echo "${name}"
  fi
}

TO_BUILD=""
[ -n "$(svc gateway   '^backend/(gateway|common-auth)/')" ]    && TO_BUILD="${TO_BUILD} gateway"
[ -n "$(svc cas       '^backend/(cas-service|common-auth)/')" ] && TO_BUILD="${TO_BUILD} cas"
[ -n "$(svc kb        '^backend/(kb-service|common-auth)/')" ]  && TO_BUILD="${TO_BUILD} kb"
[ -n "$(svc frontend  '^frontend/')" ]                          && TO_BUILD="${TO_BUILD} frontend"
INFRA_CHANGED="no"
[ -n "$(svc infra     '^(Jenkinsfile|backend/docker-compose[^/]*\.ya?ml|backend/scripts/|\.github/)')" ] && INFRA_CHANGED="yes"
# 只改 docs / 无关文件 → 什么都不用重建
if [ -z "${TO_BUILD}" ] && [ "${INFRA_CHANGED}" = "no" ]; then
  echo "⚠️  本次无 backend/frontend/infra 实质改动，仅记录部署点。"
  echo "${HEAD}" > "${LAST_FILE}"
  exit 0
fi
echo "将重建:${TO_BUILD:-（仅 infra）}  infra=${INFRA_CHANGED}"

# ---------- 1) mvn 全量打包 ----------
echo "═══ mvn package（-DskipTests）═══"
if ! mvn -B -DskipTests package > /tmp/deploy-mvn.log 2>&1; then
  tail -40 /tmp/deploy-mvn.log >&2
  echo "❌ mvn 打包失败，终止（详见 /tmp/deploy-mvn.log）" >&2
  exit 1
fi
grep -E "BUILD (SUCCESS|FAILURE)" /tmp/deploy-mvn.log | tail -1

# ---------- 2) 构建改动服务的镜像 ----------
for s in ${TO_BUILD}; do
  case "${s}" in
    gateway)  echo "── docker build gateway ──";  docker build -q -t gateway:deploy     -f gateway/Dockerfile      gateway/      ;;
    cas)      echo "── docker build cas-service ──"; docker build -q -t cas-service:deploy -f cas-service/Dockerfile cas-service/  ;;
    kb)       echo "── docker build kb-service ──";  docker build -q -t kb-service:deploy  -f kb-service/Dockerfile  kb-service/   ;;
    frontend) echo "── docker build frontend（多阶段，较慢）──"; docker build -q -t frontend:deploy "${REPO}/frontend" ;;
  esac
done

# ---------- 3) compose up（改动服务 + infra 变化时全量重排）----------
UP_TARGETS=""
for s in ${TO_BUILD}; do
  case "${s}" in gateway) UP_TARGETS="${UP_TARGETS} gateway";; cas) UP_TARGETS="${UP_TARGETS} cas-service";; kb) UP_TARGETS="${UP_TARGETS} kb-service";; frontend) UP_TARGETS="${UP_TARGETS} frontend";; esac
done
[ "${INFRA_CHANGED}" = "yes" ] && UP_TARGETS=""   # infra 变 → 全量 up -d 重排
echo "═══ compose up${UP_TARGETS:+ ${UP_TARGETS}}（TAG=deploy）═══"
# shellcheck disable=SC2086
TAG=deploy docker compose -f docker-compose.yml -f docker-compose.business.yml up -d ${UP_TARGETS}

# ---------- 4) 冒烟 ----------
echo "═══ 冒烟测试 ═══"
fail=0
RC=$(docker ps --format "{{.Status}}" | grep -c "Restarting" || true)
[ "${RC}" -gt 0 ] && { echo "❌ 有容器在重启 (${RC})"; fail=1; }

# 等服务起来（cas/kb 可能需要数十秒）
sleep 20
for i in $(seq 1 15); do
  ok=1
  curl -sf -o /dev/null -m 5 http://localhost/                     || ok=0
  curl -sf -o /dev/null -m 5 http://localhost:8888/api/v1/captcha  || ok=0
  curl -sf -o /dev/null -m 5 http://localhost:8888/api/v1/kb/health || ok=0
  [ "${ok}" = "1" ] && break
  echo "  等待全链路就绪... ${i}"
  sleep 6
done
curl -sf -o /dev/null -m 5 http://localhost/                     || { echo "❌ 前端 200 不通"; fail=1; }
curl -sf -o /dev/null -m 5 http://localhost:8888/api/v1/captcha  || { echo "❌ 网关→CAS 不通"; fail=1; }
curl -sf -o /dev/null -m 5 http://localhost:8888/api/v1/kb/health || { echo "❌ 网关→KB 不通"; fail=1; }
echo "  前端 / 网关→CAS / 网关→KB 均 200 ✓"

for s in gateway cas-service kb-service; do
  n=$(curl -sf -m 6 "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=${s}" 2>/dev/null \
      | python3 -c "import sys,json;print(len(json.load(sys.stdin).get('hosts',[])))" 2>/dev/null || echo 0)
  [ "${n}" -ge 1 ] || { echo "❌ Nacos 缺实例: ${s}=${n}"; fail=1; }
done
echo "  Nacos 实例: gateway/cas-service/kb-service 均 ≥1 ✓"

if [ "${fail}" = "0" ]; then
  echo "✅ 部署成功，记录部署点 ${HEAD}"
  echo "${HEAD}" > "${LAST_FILE}"
else
  echo "❌ 冒烟未通过（服务已尝试拉起，见上）。未记录部署点，可手动重跑。" >&2
  exit 1
fi
