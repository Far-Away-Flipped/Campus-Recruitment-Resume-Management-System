# Docker 部署指南（遨天科技 校园招聘简历管理系统）

> **适用范围**：需要将整套系统（MySQL + 后端 + 两个前端）容器化部署到一台
> Linux 服务器（或 Docker Desktop）的场景。
>
> ⚠️ **与既有基线的说明**：`设计/_briefs/00-技术选型裁决基线.md` 原定
> 「单台式机、禁止 Docker」。当前 Dockerfile/compose 是**额外的可选部署方式**，
> 不替代既有的 Windows 本地部署（`运维脚本/deploy.ps1`）。选用前请确认甲方
> 约束已放宽。若继续使用本地单机部署，可忽略本目录全部文件。

---

## 1. 架构与端口

| 服务    | 镜像                              | 容器内端口 | 宿主端口（可改）   | 说明 |
| ------- | --------------------------------- | ---------- | ------------------ | ---- |
| mysql   | `mysql:8.0`                       | 3306       | 不暴露             | 首次启动自动执行建表+种子数据 |
| backend | `atmoto/recruit-backend`（本地构建） | 8080       | 不暴露（可注释开放调试） | Spring Boot，prod profile |
| portal  | `atmoto/recruit-portal`（本地构建） | 80         | `${PORTAL_PORT}` 默认 80 | 学生门户，nginx 托管+反代 `/api/portal` |
| admin   | `atmoto/recruit-admin`（本地构建）  | 80         | `${ADMIN_PORT}` 默认 8081 | HR 管理后台，nginx 托管+反代 `/api/*` |

- **学生门户**（公网/内网均可访问）：`http://<host>:80` —— 仅代理 `/api/portal`，
  管理接口一律 403，沿用既有 `scripts/nginx-campus.conf` 的安全基线。
- **HR 管理后台**（仅内网）：`http://<host>:8081` —— 切勿直接暴露公网，
  如需公网访问请叠加网关认证/防火墙限制。
- 后端 **不直接暴露** 给宿主，所有 `/api` 请求经容器内 nginx 反代
  （同源，浏览器不触发 CORS）；nginx 显式清空 `Origin` 头，等价于
  Vite dev 代理的 `removeHeader('Origin')`。

## 2. 前置条件

- Docker Engine ≥ 20.10 且支持 Compose v2（`docker compose version` 可验证）。
- **首次构建需要联网**：拉取基础镜像 + Maven/npm 依赖。
  与本地 `mvn -o`（离线）流程不同，离线服务器无法完成镜像构建。

## 3. 快速开始

```bash
# 1) 准备环境变量并修改密钥
cp .env.example .env
#    编辑 .env：
#      - MYSQL_ROOT_PASSWORD   数据库 root 密码
#      - JWT_ADMIN_SECRET      后端管理员 JWT 密钥
#      - JWT_PORTAL_SECRET     学生端 JWT 密钥
#      - PORTAL_PORT / ADMIN_PORT（如宿主端口冲突）

# 2) 构建并启动（首次构建约需几分钟）
docker compose up -d --build

# 3) 查看状态与日志
docker compose ps
docker compose logs -f backend

# 4) 验证后端健康
docker compose exec backend curl -fsS http://127.0.0.1:8080/api/health
```

默认登录凭证与开发环境一致（见 CLAUDE.md「登录凭证」），
但 `AT-admin` 密码建议登录后尽快修改。

## 4. 目录与文件说明

```
docker-compose.yml                     # 编排入口（项目根）
.env.example                           # 环境变量模板 → 复制为 .env
deploy/
  backend/Dockerfile                   # 后端多阶段构建（Maven → JRE）
  portal/Dockerfile                    # 学生端（node → nginx）
  admin/Dockerfile                     # HR 端（node → nginx）
  nginx/portal.conf                    # 学生端 nginx（公开端口安全基线）
  nginx/admin.conf                     # HR 端 nginx（内网反代）
  data/                                # 运行期上传数据（bind mount，已 gitignore）
```

## 5. 关键设计点

### 5.1 后端配置注入（environment 覆盖 yml）

- `SPRING_PROFILES_ACTIVE=prod`：启用生产配置。
- `SERVER_ADDRESS=0.0.0.0`：生产 yml 默认 `127.0.0.1`（直连安全基线），
  容器内必须监听所有网卡供 nginx 反代。**env 优先级高于 yml**，因此覆盖生效。
- `SPRING_DATASOURCE_*`：指向 compose 网络内的 `mysql` 服务。
- `JWT_ADMIN_SECRET / JWT_PORTAL_SECRET`：覆盖 `application.yml` 中的
  开发默认密钥（原文件注释已要求生产覆盖，本次在 Docker 侧补齐）。
- `FILE_UPLOAD_ROOT=/data`：上传根目录，bind mount 到 `deploy/data`。
- `LIBREOFFICE_PATH=/usr/bin/soffice`：Word→PDF 简历预览。

### 5.2 数据库初始化与增量迁移

- `mysql` 服务挂载 `init-schema.sql` / `init-data.sql` 到
  `/docker-entrypoint-initdb.d/`，**仅在数据卷为空（首次建库）** 时执行。
- **增量迁移由后端启动时自动执行**：后端首次启动会创建 `schema_version`
  版本表，读取 classpath `db/migration/V{n}__*.sql` 中尚未应用的迁移文件
  并逐条执行（已应用版本跳过）。因此 `git pull` 后直接 `docker compose up -d --build`
  即可自动完成数据库升级，无需手动执行 DDL/DML。
- 约定：**初始建库 = 基线 V0**，迁移从 V1 起；**禁止编辑已应用的版本文件**，
  后续 schema/数据变更一律新增 `V{n+1}__描述.sql` 到
  `code/recruit-backend/recruit-admin/src/main/resources/db/migration/`。

### 5.3 健康检查

- 后端容器检查 `GET /api/health`（permitAll）。
  **不要改用 `/actuator/health`** —— SecurityConfig 中 `/actuator/**` 需要鉴权。

### 5.4 文件上传

- nginx `client_max_body_size 11m`，对齐后端 `file.max-file-size: 10MB`，
  避免大简历上传 413。

## 6. 备份与恢复

```bash
# 停止前做一致性备份：
docker compose exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" atmoto_recruit' > atmoto_recruit.sql
# 上传附件/简历在宿主机 deploy/data/ 下，直接打包即可。

# 恢复（新建数据卷场景）：
docker compose down -v            # 注意：-v 会清空数据卷！
docker compose up -d mysql        # 自动重新初始化
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" atmoto_recruit' < atmoto_recruit.sql
```

## 7. 更新版本

```bash
git pull
docker compose up -d --build      # 重建镜像并滚动重启
# 数据库增量迁移由后端启动时自动执行（见 5.2），无需手动操作
```

## 8. 安全注意事项

1. **密钥**：`.env` 中的数据库密码、两个 JWT 密钥必须在生产替换；
   `.env` 已被 `.gitignore` 排除，不会提交。
2. **HR 后台端口**：`ADMIN_PORT`（默认 8081）默认宿主开放，请用防火墙/
   安全组限制来源；公网暴露请叠加反代认证。
3. **LibreOffice**：默认随后端镜像安装（约 +500MB）。输入文件来自不可信
   外部投递，建议按 `code/recruit-backend/recruit-admin/src/main/resources/scripts/libreoffice-hardening.md`
   在容器内施加同等加固（低权限账号、出站限制、超时终止）。
   若要精简镜像：`WITH_LIBREOFFICE=false`（文档预览功能降级不可用）。
4. **宿主 80 端口**：若已被占用，改 `.env` 中 `PORTAL_PORT`。

## 9. 常见问题

- **`docker compose up` 报 `command not found` / compose 语法错误**：
  需 Compose v2（`docker compose`，新版 Docker 自带）；旧版 `docker-compose` 可能不支持 `name:` 字段。
- **首次构建很慢**：正常，Maven/npm 依赖需在线下载；国内可设
  `NPM_REGISTRY=https://registry.npmmirror.com`（.env 默认已设）。
  Maven 若太慢，可在 `deploy/backend/Dockerfile` 的 `RUN mvn ...` 前加阿里云镜像
  `settings.xml`。
- **登录页验证码不显示 / 中文乱码**：后端镜像已装 CJK 字体，属罕见情况，
  检查 `docker compose logs backend` 中 `DocumentConversionService` 初始化日志。
- **上传 413**：确认 nginx 的 `client_max_body_size 11m` 未被覆盖。
- **改了 init-*.sql 不生效**：见 5.2，仅首次数据卷初始化时执行。
