# 运行阶段镜像（需先在宿主机 mvn package 生成 jar，避免 Docker 内 Maven 镜像源问题）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY cas-server/target/cas-server-1.0.0.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 18080
# 探活走独立管理端口 9101 的 liveness 探针（不含 DB/Rabbit/邮件等依赖状态，
# actuator 不在业务端口暴露，见 application.yml management.server.port）
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:9101/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
