# 运行阶段镜像（需先在宿主机 mvn package 生成 jar，避免 Docker 内 Maven 镜像源问题）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY cas-server/target/cas-server-1.0.0.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 18080
# 补齐探活：此前 cas 镜像完全没有 HEALTHCHECK（P1-10）。注意 context-path 为 /api/v1。
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:18080/api/v1/actuator/health || exit 1
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
