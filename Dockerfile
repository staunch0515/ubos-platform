# --- 第一阶段：构建 (Builder) ---
# 使用官方 Gradle 镜像进行编译，确保环境一致
FROM gradle:8.5-jdk17 AS builder

# 将代码复制到镜像中
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

# 执行构建 (只打包 server 模块，依赖会自动处理)
# --no-daemon: CI/Docker 环境推荐配置
RUN ./gradlew :ubos-server:bootJar --no-daemon

# --- 第二阶段：运行 (Runtime) ---
# 使用轻量级的 JRE 镜像运行
FROM openjdk:17-slim

# 从构建阶段把 jar 包拷过来
COPY --from=builder /home/gradle/src/ubos-server/build/libs/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "/app.jar"]