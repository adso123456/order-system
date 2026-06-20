# ===== 构建阶段 =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 阿里云 Maven 镜像，加速依赖下载
COPY settings.xml /root/.m2/settings.xml

# 先拷 pom.xml 下依赖（利用 Docker 层缓存，源码没变时不用重新下载）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 再拷源码编译
COPY src ./src
RUN mvn package -DskipTests -B

# ===== 运行阶段 =====
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
