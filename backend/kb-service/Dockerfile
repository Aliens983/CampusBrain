# 运行阶段镜像（需先在宿主机 mvn package 生成 jar）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/kb-service-1.0.0.jar app.jar
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:8081/kb/health || exit 1
ENTRYPOINT ["java", "-Xms512m", "-Xmx1g", "-jar", "app.jar"]
