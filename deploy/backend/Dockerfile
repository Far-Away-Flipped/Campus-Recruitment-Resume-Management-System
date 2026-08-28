# syntax=docker/dockerfile:1
# ============================================================
# 遨天科技 校园招聘简历管理系统 - 后端镜像
# 多阶段构建：Stage 1 Maven 编译打包 → Stage 2 JRE 运行
# 构建上下文：仓库根目录（配合根目录 .dockerignore 控制上下文体积）
#
# 运行配置（数据源/JWT密钥/上传路径等）由 docker-compose.yml
# 通过环境变量注入；生产环境不得使用默认密钥。
# ============================================================

# ---------- Stage 1: Maven 构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先只拷贝 pom 文件预取依赖，利用 Docker 层缓存加速二次构建
# （首次构建需要联网下载依赖，与本地 -o 离线构建不同）
COPY code/recruit-backend/pom.xml ./pom.xml
COPY code/recruit-backend/recruit-common/pom.xml ./recruit-common/pom.xml
COPY code/recruit-backend/recruit-system/pom.xml ./recruit-system/pom.xml
COPY code/recruit-backend/recruit-framework/pom.xml ./recruit-framework/pom.xml
COPY code/recruit-backend/recruit-biz/pom.xml ./recruit-biz/pom.xml
COPY code/recruit-backend/recruit-admin/pom.xml ./recruit-admin/pom.xml
# 预取依赖失败不阻塞构建，后续 package 阶段仍会补齐缺失依赖
RUN mvn -B -q -DskipTests dependency:go-offline || true

# 拷贝全部源码并打包
COPY code/recruit-backend/ ./
RUN mvn -B -q -DskipTests package

# ---------- Stage 2: JRE 运行 ----------
FROM eclipse-temurin:17-jre AS runtime

# 是否安装 LibreOffice（Word/DOC/DOCX → PDF 简历预览）。
# 置 false 可显著减小镜像体积，代价是文档预览不可用
# （后端会自动降级，不影响投递/查看等其他功能）。
ARG WITH_LIBREOFFICE=true

ENV TZ=Asia/Shanghai \
    FILE_UPLOAD_ROOT=/data \
    SPRING_PROFILES_ACTIVE=prod

# 基础运行环境：
#   - 字体（图形验证码、LibreOffice/PDF 渲染中文简历必须 CJK 字体）
#   - curl（容器健康检查）、ca-certificates、时区
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        fontconfig \
        fonts-liberation \
        fonts-noto-cjk \
        tzdata \
    && ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && if [ "${WITH_LIBREOFFICE}" = "true" ]; then \
         apt-get install -y --no-install-recommends libreoffice-writer; \
       fi \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/recruit-admin/target/recruit-admin.jar /app/recruit-admin.jar
RUN mkdir -p /data && chmod 775 /data

EXPOSE 8080

# /actuator/** 需鉴权，健康检查使用 permitAll 的 /api/health
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/api/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/recruit-admin.jar"]
