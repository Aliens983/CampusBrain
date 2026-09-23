# 运行阶段镜像（需先在宿主机 mvn package 生成 jar）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/gateway-1.0.0.jar app.jar
EXPOSE 80
# 探活走独立管理端口 9103 的 liveness 探针（actuator 不在业务 80 端口暴露）
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:9103/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
