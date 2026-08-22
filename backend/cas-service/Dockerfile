# 运行阶段镜像（需先在宿主机 mvn package 生成 jar，避免 Docker 内 Maven 镜像源问题）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY cas-server/target/cas-server-1.0.0.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 18080
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
