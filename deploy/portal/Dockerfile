# syntax=docker/dockerfile:1
# ============================================================
# 学生门户前端镜像：Node 构建 → nginx 托管 + 反向代理 /api/portal
# 构建上下文：仓库根目录（配合根目录 .dockerignore 控制上下文体积）
#
# 构建参数：
#   NPM_REGISTRY   npm 镜像源（国内可传 https://registry.npmmirror.com）
# ============================================================
ARG NPM_REGISTRY=https://registry.npmjs.org

# ---------- Stage 1: 构建前端 ----------
FROM node:20-alpine AS build
ARG NPM_REGISTRY
WORKDIR /app

# 先安装依赖（利用层缓存；npm ci 需 package-lock.json）
COPY code/recruit-portal-ui/package.json code/recruit-portal-ui/package-lock.json ./
RUN echo "registry=${NPM_REGISTRY}" > /root/.npmrc \
    && npm ci --no-audit --no-fund

# 拷贝源码并构建
COPY code/recruit-portal-ui/ ./
RUN npm run build

# ---------- Stage 2: nginx 运行 ----------
FROM nginx:1.27-alpine
COPY deploy/nginx/portal.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
