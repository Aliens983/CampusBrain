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
#   - 每次先加载 backend/.env，再自动 mvn clean install -Dspotbugs.skip（-DskipTests），最后 java -jar
#     clean 防 IDE 坏 class 残留；install 同步共享模块到 ~/.m2；spotbugs 等静态检查留给 CI
#   - 全局构建锁：cas/kb 共用 common-auth 等上游模块，禁止两个终端并发构建
#   - 打包前会自动停止同名旧实例（优雅 kill，15s 不退则强杀），避免覆盖运行中的 jar
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

# 全局构建锁：cas / kb / gateway 的 -am 构建都会带上 common-auth 等共享模块，
# 并发执行 clean install 会互相删对方正在编译的 target/classes、抢装同一个 .m2 jar，
# 表现为间歇“程序包不存在/找不到符号”，失败模块每次漂移。锁只罩构建阶段，不罩运行。
LOCK_FILE="/tmp/campusbrain-mvn.lock"
exec 9>"${LOCK_FILE}"
if ! flock -w 600 9; then
  echo "✗ 另一个终端正在构建（锁 ${LOCK_FILE}），等待 10 分钟仍未结束"
  exit 1
fi
echo "▶ 已获取构建锁，其他终端的同类构建将排队等待 ..."

# 停止同名旧实例：必须在 mvn package 之前做。
# 原因：fat jar 被原地覆盖时，旧 JVM 的 LaunchedURLClassLoader 懒加载会读到被截断的
#       jar，进而随机抛出 Rabbit/Tomcat/Netty/gRPC 等 ClassNotFoundException（运行中覆盖 jar 必崩）。
JAR_BASE="${JAR##*/}"
EXISTING="$(pgrep -f "[j]ava -jar .*${JAR_BASE}" 2>/dev/null || true)"
if [ -n "${EXISTING}" ]; then
  echo "▶ 发现 ${SERVICE} 旧实例（PID: ${EXISTING//$'\n'/ }），先停止再打包 ..."
  # shellcheck disable=SC2086
  kill ${EXISTING} 2>/dev/null || true
  for _ in $(seq 1 15); do
    sleep 1
    pgrep -f "[j]ava -jar .*${JAR_BASE}" >/dev/null 2>&1 || { EXISTING=""; break; }
  done
  if [ -n "${EXISTING}" ]; then
    echo "▶ 等待 15s 未退出，强制 kill -9 ..."
    # shellcheck disable=SC2086
    kill -9 ${EXISTING} 2>/dev/null || true
    sleep 1
  fi
  echo "✓ 旧实例已停止"
fi

# 打包（除非 --fast 且 jar 已存在）
if [ "${FAST}" = "yes" ] && [ -f "${JAR}" ]; then
  echo "▶ 跳过打包（--fast），直接用: ${JAR}"
else
  echo "▶ 打包 ${MODULE_PATH} ..."
  # clean install（install 生命周期已包含 package，无需再写 package）：
  #   - clean：清空 target，防止 IDE（redhat.java / ecj）写入的增量编译坏 class
  #     被 javac 沿用并打进 fat jar（曾导致运行时 Mappers.getMapper() 抛
  #     ExceptionInInitializerError、接口 500）
  #   - install：产物同时装入本地 ~/.m2，保证 common-auth 等共享模块引用的是最新版
  # 不用 -U：com.laoliu 内部模块都在同一反应堆里源码构建，-U 只增加远程检查且无收益。
  # 不用 -q：编译错误必须直接可见，避免“看似打包成功”的误判。
  # spotbugs.skip：kb-service 把 spotbugs:check 绑在 verify（install 会触发，package 不会），
  #   存量 Low 告警会阻断本地启动；静态检查留给 CI，本地只跑服务。
  if ! mvn -B -DskipTests -Dspotbugs.skip=true -pl "${MODULE_PATH}" -am clean install; then
    echo "✗ 构建失败（若是“程序包不存在/找不到符号”且时有时无，"
    echo "  请确认 IDE 已关闭 java.autobuild.enabled，详见项目说明）"
    exit 1
  fi
fi

[ -f "${JAR}" ] || { echo "✗ 找不到 ${JAR}，请去掉 --fast 重新打包"; exit 1; }

# 释放构建锁：之后各服务自由启动，互不阻塞（锁只串行化构建阶段）
flock -u 9

echo "▶ 启动 ${SERVICE}（端口见上，Ctrl+C 停止）..."
exec java -jar "${JAR}"
