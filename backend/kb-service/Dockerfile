# 运行阶段镜像（需先在宿主机 mvn package 生成 jar）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/kb-service-1.0.0.jar app.jar
EXPOSE 8081
# 探活走独立管理端口 9102 的 liveness 探针（不含 ES/Qdrant/Redis 等依赖状态；
# 独立端口无 context-path，actuator 不在业务 8081 暴露）
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:9102/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-Xms512m", "-Xmx1g", "-jar", "app.jar"]
