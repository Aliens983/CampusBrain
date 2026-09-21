# 运行阶段镜像（需先在宿主机 mvn package 生成 jar）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/gateway-1.0.0.jar app.jar
EXPOSE 80
# 补齐探活：此前 gateway 镜像完全没有 HEALTHCHECK（P1-10）。
# 容器内端口为 80（宿主机 8888 映射进来），网关无 context-path。
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:80/actuator/health || exit 1
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
