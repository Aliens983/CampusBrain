# 运行阶段镜像（需先在宿主机 mvn package 生成 jar）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/kb-service-1.0.0.jar app.jar
EXPOSE 8081
# 探活路径必须带 context-path（server.servlet.context-path=/api/v1/kb）。
# 此前写成 /kb/health，每次探活 404 → 容器恒为 unhealthy，
# 编排的健康判断/重启/摘流全部失真（P1-10）。
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:8081/api/v1/kb/health || exit 1
ENTRYPOINT ["java", "-Xms512m", "-Xmx1g", "-jar", "app.jar"]
