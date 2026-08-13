# 遨天科技 校园招聘简历管理系统

> 遨天科技（北京）有限公司 · 商业航天电推进系统 · 国家级专精特新"小巨人"企业

## 项目简介

面向遨天科技人文发展部（HR部门）的校园招聘全流程管理系统。学生在线注册投递简历，HR后台筛选评审。定位为中小规模校招场景（每年一届、数十岗位、数百到数千份简历），单台台式机部署，非互联网级高并发招聘平台。

### 系统组成

| 端 | 说明 | 访问入口 |
|---|---|---|
| 学生端（前台门户） | 注册登录、个人信息与教育经历、岗位浏览与多条件筛选、一键投递、"我的投递"进度查看、消息通知 | `http://localhost:5173` |
| HR 管理后台 | 工作台、岗位管理（含模板）、简历筛选与附件在线预览、数据报表、学生管理、个人中心 | `http://localhost:5174/admin` |
| 系统管理端 | HR账号/角色/部门/字典/通知模板/品牌配置管理、操作审计、网络管理（仅超级管理员） | 与 HR 后台共入口 |

## 技术栈

| 层 | 选型 | 说明 |
|---|---|---|
| 后端框架 | Spring Boot 3.4 / Java 17 | 单体 JAR 部署 |
| 后台基座 | RuoYi-Vue（MIT协议，免费商用） | RBAC/部门/字典开箱即用 |
| 前端 | Vue 3 + Vite + Element Plus | 学生门户与HR后台两个独立工程 |
| 数据库 | MySQL 8.0 | ngram 全文索引支持关键词搜索 |
| 缓存 | Caffeine（本地缓存） | 无 Redis 依赖 |
| 文件存储 | 本地文件系统 | GB级数据，无对象存储需求 |
| 文档转换 | LibreOffice Headless | Word→PDF 在线预览 |

## 快速开始

### 环境要求

- Windows 10/11 或 Linux
- JDK 17+
- MySQL 8.0（root 空密码）
- Node.js 18+
- LibreOffice（附件预览转换，可选，约 600MB）

### 1. 初始化数据库

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS atmoto_recruit
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 执行建表与种子数据
SOURCE code/recruit-backend/recruit-admin/src/main/resources/sql/init-schema.sql;
SOURCE code/recruit-backend/recruit-admin/src/main/resources/sql/init-data.sql;
```

### 2. 启动后端

项目本地 Maven 路径为 `下载/apache-maven-3.9.16/bin/mvn`（PATH 无全局 mvn 命令）。

```bash
cd code/recruit-backend

# 编译 + 打包（离线模式用 -o）
"下载/apache-maven-3.9.16/bin/mvn" package -DskipTests

# 启动（dev profile，端口 8080）
java -jar recruit-admin/target/recruit-admin.jar --spring.profiles.active=dev
```

### 3. 启动前端

```bash
# HR 管理后台（端口 5174）
cd code/recruit-admin-ui
npm install
npm run dev

# 学生门户（端口 5173）
cd code/recruit-portal-ui
npm install
npm run dev
```

### 4. 登录

| 角色 | 地址 | 账号 | 密码 |
|---|---|---|---|
| 超级管理员 | `http://localhost:5174/admin` | `AT-admin` | `at123456` |
| 学生 | `http://localhost:5173` | `13812341234` | `123456789` |

> 开发环境启用图形验证码，登录时需输入图片上的 4 位数字。
>
> **权限分级**：HR 账号分两级——「超级管理员」（`admin` 角色）拥有全部功能；「HR用户」（`hr` 角色）仅可访问「工作台」和「招聘管理」。`AT-admin` 是唯一内置超级管理员，不可删除。

## 项目结构

```
code/
├── recruit-backend/          # 后端（Maven 多模块）
│   ├── recruit-common/       # 全局工具、错误码、AjaxResult
│   ├── recruit-system/       # 系统管理域（用户/角色/部门/字典/菜单 + CORS白名单规则）
│   ├── recruit-framework/    # 横切层（Security/CORS/Caffeine 缓存）
│   ├── recruit-biz/          # 业务域（岗位/简历/品牌配置）
│   └── recruit-admin/        # 启动入口 + 系统管理 Controller
├── recruit-admin-ui/         # HR 端前端（Vue 3 + Element Plus，端口 5174）
└── recruit-portal-ui/        # 学生端前端（Vue 3 + 深空风品牌，端口 5173）
```

**Maven 依赖方向**（严格单向，反向引用编译失败）：
```
recruit-common → recruit-system → recruit-framework → recruit-biz → recruit-admin
```

## 开发注意事项

### 双 JWT 认证体系

后端注册了两条 `SecurityFilterChain`：`portalFilterChain`（Order 1，学生端 `/api/portal/**`）和 `adminFilterChain`（Order 2 兜底，HR端 `/api/admin/**` + 系统管理 `/api/system/**`）。两条链使用不同 JWT 密钥和 `aud` 声明，学生 Token 在 admin 链上验签即失败——防越权从"依赖注解写对"变为"结构上不可能"。

### AjaxResult 响应约定

本项目业务层 HTTP 状态码恒为 200，鉴权/错误通过响应体 `code` 字段区分：`200` 成功、`20001` 未登录。**curl 测试的断言语义必须与此约定一致**，不能按常规 REST API 的 401/403 判断鉴权是否生效。

### CORS 配置

`CorsConfig.java` 的 `CorsFilter` 已用 `FilterRegistrationBean` 设置为 `Ordered.HIGHEST_PRECEDENCE`，确保在 Spring Security 过滤链之前执行（修复了 OPTIONS 预检被鉴权层拦截的问题）。白名单规则从 `sys_cors_origin` 表动态读取，支持 EXACT 精确匹配和 CIDR 网段匹配（仅对 IPv4 字面量 Origin 生效），HR 后台 `系统管理 → 网络管理` 可可视化管理，修改后无需重启即可生效。

### curl 测试 CORS 效果

**必须直连 8080 端口**，不能经 Vite 代理（`vite.config.js` 的 `proxyReq.removeHeader('Origin')` 会剥离 Origin 头，导致 CORS 判定根本不触发、测不出真实效果）。

```bash
curl.exe -s -i "http://127.0.0.1:8080/api/system/network/cors-origins" \
  -H "Origin: http://localhost:5173" \
  -H "Authorization: Bearer <TOKEN>"
```

### Windows 编码

- `.ps1` 脚本必须保存为 UTF-8 with BOM（无 BOM 会被 PowerShell 5.1 按 ANSI 解析导致中文乱码）
- `.bat` 脚本保持纯 ASCII，中文输出交给 `.ps1` 处理
- curl 传中文 JSON body 会被按 GBK 编码，与后端 UTF-8 解析不匹配，测试/调试时用纯 ASCII 英文参数

## 局域网联调（手机访问 HR 后台）

1. 确认开发机与手机连接同一 WiFi
2. 前端 Vite 启动时加 `--host` 参数，后端 `application-dev.yml` 中 `server.address=0.0.0.0`
3. 在 HR 后台 `系统管理 → 网络管理 → CORS白名单` 中添加手机所在网段的 CIDR 规则（如 `192.168.31.0/24`）
4. 手机浏览器访问 `http://<开发机局域网IP>:5174/admin`（本机局域网 IP 可通过 `ipconfig` 查看）

## 设计文档

| 文档 | 路径 |
|---|---|
| 设计简报（唯一事实基线） | `设计/_briefs/设计简报.md` |
| 技术选型裁决基线 | `设计/_briefs/00-技术选型裁决基线.md` |
| 系统架构设计 | `设计/_briefs/03-系统架构设计.md` |
| 数据架构设计 | `设计/_briefs/04-数据架构设计.md` |
| 接口与界面设计 | `设计/_briefs/05-接口与界面设计.md` |
| 技术方案 V1.0 | `设计/校园招聘简历管理系统_技术方案_V1.0.md` |
| 网络管理模块设计方案 | `设计/网络管理模块设计方案_V1.0.md` |
| 网络管理模块测试方案 | `设计/网络管理模块测试方案_V1.0.md` |

## License

内部项目，遨天科技（北京）有限公司版权所有。
