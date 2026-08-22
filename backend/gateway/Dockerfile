# 运行阶段镜像（需先在宿主机 mvn package 生成 jar）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/gateway-1.0.0.jar app.jar
EXPOSE 80
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
