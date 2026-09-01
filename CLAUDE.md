# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

遨天科技（北京）校园招聘简历管理系统 — 三端系统：学生门户（recruit-portal-ui）、HR管理后台（recruit-admin-ui）、系统管理端（与HR后台共前端）。单台式机部署、峰值20并发、无专职运维、 **禁用Redis**（Caffeine本地缓存+MySQL替代）。

## 常用命令

### 后端（code/recruit-backend）

本地Maven路径为 `E:\Program\校园招聘简历管理系统\下载\apache-maven-3.9.16\bin\mvn`（PATH没有全局mvn命令，离线用 `-o`）。

```bash
# 编译
"E:\Program\校园招聘简历管理系统\下载\apache-maven-3.9.16\bin\mvn" -o compile

# 打包
"E:\Program\校园招聘简历管理系统\下载\apache-maven-3.9.16\bin\mvn" -o package -DskipTests

# 启动（dev profile，MySQL root空密码，库名atmoto_recruit，端口8080）
java -jar recruit-admin/target/recruit-admin.jar --spring.profiles.active=dev
```

### 前端

```bash
cd code/recruit-admin-ui   # HR端 (5174)
npm run dev

cd code/recruit-portal-ui  # 学生端 (5173)
npm run dev
```

### 数据库

- MySQL 8.0，root空密码，库 `atmoto_recruit`
- 建表脚本：`code/recruit-backend/recruit-admin/src/main/resources/sql/init-schema.sql`
- 种子数据：`code/recruit-backend/recruit-admin/src/main/resources/sql/init-data.sql`
- **注意**：数据库是持久化运行的，对SQL文件的追加修改不会自动应用到已有实例，需要手动执行对应INSERT/ALTER语句
- **增量迁移**：后端启动时自动执行 `recruit-admin/src/main/resources/db/migration/V{n}__*.sql` 中未应用的版本（`schema_version` 表去重，initdb基线=V0）。后续 schema/数据变更请新增 `V{n+1}__描述.sql`，**禁止编辑已应用版本文件**（会触发 checksum 告警）

### 登录凭证（开发环境）

- HR管理后台：`AT-admin` / `at123456`（超级管理员，唯一内置账号，不可删除）
- 学生门户：`13812341234` / `123456789`（可能已被测试脚本修改，备用 `13822222222/Student@123`）
- 登录需要图形验证码：`GET /api/admin/auth/captcha?key=<随机串>`，验证码明文打印在DEBUG日志

### curl测试（Windows）

PowerShell中`curl`是`Invoke-WebRequest`别名，必须用`curl.exe`。验证CORS效果必须直连8080端口，**不能经Vite代理**（Vite会剥离Origin头导致CORS判定完全不触发，看起来一切正常但测不出真实效果）。

```bash
# 带Origin头直连后端（验证CORS的真实路径）
curl.exe -s -i "http://127.0.0.1:8080/api/system/network/cors-origins" \
  -H "Origin: http://localhost:5173" \
  -H "Authorization: Bearer <TOKEN>"
```

## 模块架构

后端Maven五模块，依赖方向是**严格单向链**：

```
recruit-common → recruit-system → recruit-framework → recruit-biz → recruit-admin
```

- **recruit-common**：全局工具、ErrorCode枚举、AjaxResult响应包装
- **recruit-system**：系统管理域 Domain/Mapper/Service（SysDept/SysRole/SysDictType/SysMenu等RuoYi体系类），**也存放网络管理模块的CORS相关Domain/Service**（见下文"网络管理模块"）
- **recruit-framework**：横切关注点——`SecurityConfig`/`CorsConfig`/JWT Token Filter/Caffeine缓存配置。**自定义CORS判定逻辑（DynamicCorsConfigurationSource）在此**
- **recruit-biz**：业务域 Controller/Service（岗位管理、简历管理、品牌配置）
- **recruit-admin**：Spring Boot启动类、`controller/system/`包下系统管理Controller（SysUser/SysDept/SysRole/SysNetworkConfig等）

**关键约束**：反向引用会导致Maven循环依赖编译失败。例如`recruit-system`不能引用`recruit-framework`中的`AdminUserHolder`（Controller所在的`recruit-admin`同时依赖两者，由Controller组装好上下文再传入Service是正确做法）。

### 双过滤链

前端三个端口（5173学生端、5174 HR端、8080后端），通过Vite代理转发`/api`请求。后端双SecurityFilterChain：

- **portalFilterChain**（Order 1）：学生端 `/api/portal/**`，`permitAll`仅包含auth/login/register/refresh/captcha/sms-code/reset-password、jobs/**、brand/**
- **adminFilterChain**（Order 2，兜底）：HR端 `/api/admin/**` + 系统管理 `/api/system/**`，`permitAll`仅含admin auth/login/captcha和health/actuator

## 关键约定与模式

### AjaxResult响应与断言

本项目业务层HTTP状态码恒为200，鉴权/错误通过**响应体`code`字段**区分：`200`成功、`20001`未登录、其余分段错误码（3xxxx学生、5xxxx投递、7xxxx网络管理）。**curl测试的断言必须是`code`字段值，不能按常规REST API的HTTP 401/403语义判断鉴权是否生效**。

### 权限模型

项目当前**零处使用`@PreAuthorize`**，`sys_menu.perms`字段（如`system:user:list`）只是元数据，未被任何Controller消费。`Sidebar.vue`和`router/index.js`是纯静态硬编码（Sidebar 用 `isSuperAdmin` 布尔控制「系统管理」菜单显隐）。

**两级角色**（`sys_role.role_key` 固定两个取值）：
- `admin`（超级管理员）：拥有全部功能，含系统管理、数据报表、网络管理
- `hr`（HR用户）：仅「工作台」+「招聘管理」，无系统管理权限

**鉴权组件**：`recruit-biz/common/security/AdminRoleGuard.java` 的 `requireDirector()` 统一校验当前用户是否 `admin` 角色（`sysRoleService.selectRoleKeysByUserId(userId).contains("admin")`）。系统管理侧 9 个 Controller（SysUser/SysRole/SysDept/SysDictType/SysDictData/SysNetworkConfig/BrandConfig/AuditLog/NotifyTemplate）均调用它兜底拦截。

**数据范围**：`hasAllDataScope()` 判断是否 `admin` 角色（全部数据）还是 `hr`（仅本人负责岗位）。**历史 bug 已修复**：原 `"sys_admin".equals(sysUser.getUserType())` 恒为 false 的判断已改为 `role_key` 判断，严禁照抄旧模式。

**受保护账号**：`AT-admin` 是唯一内置超级管理员，不可删除/禁用/移除 admin 角色（前端隐藏按钮 + 后端 remove/changeStatus/edit 三处拦截）。

### ErrorCode枚举

通用1xxxx / 认证鉴权2xxxx / 学生3xxxx / 岗位4xxxx / 投递5xxxx / 文件6xxxx / 网络管理7xxxx。

### CORS配置（本次已改造）

- **CorsConfig.java**：CorsFilter用`FilterRegistrationBean`包装并设为`Ordered.HIGHEST_PRECEDENCE`，确保在Spring Security过滤链之前执行（修复了OPTIONS预检被鉴权层拦截的遗留问题）
- **DynamicCorsConfigurationSource**：每次请求从`recruit-system`的`ICorsWhitelistService`读取白名单规则（Caffeine缓存+DB降级），EXACT精确匹配Origin、CIDR仅对IPv4字面量Origin做网段判断
- **ResourcesConfig.java**：已删除遗留的`addCorsMappings("*")+allowCredentials(true)`高危通配符配置（一票否决级安全清理）
- **sys_cors_origin表**：5条种子数据（localhost:5173/5174, 127.0.0.1:5173/5174, campus.atmoto.cn，`is_builtin=1`禁止物理删除）

### BrandConfig模式

`sys_brand_config`表是"一行一个配置项"的KV模式（config_key/config_value/config_type/config_group），Controller通过`BrandConfigMapper`直接读写。新增同质Simple配置可参照此模式，但**不可复用这张表本身**——`/api/portal/brand`在permitAll列表中且全表无过滤返回给匿名访问者。

### `.gitignore`与敏感文件

以下文件/目录已被`.gitignore`排除：`node_modules/`、`target/`、`dist/`、`logs/`、`.vscode/`、`.gstack/`、测试目录、顶层散落的诊断/攻击测试脚本、`登录信息.txt`（含真实凭证）、`fake_test.pdf`/`captcha_test.png`/`*adfile.exe`（安全测试样本残留）、本地工具安装包（`下载/`）、agent协作中间进度日志（`设计/执行进度/`）。

### Windows中文环境编码坑

- `.ps1`：UTF-8 with BOM，不能无BOM（PowerShell 5.1按ANSI读无BOM文件中文变乱码）
- `.bat`：纯ASCII，不要写中文，不要加`chcp 65001`
- `curl.exe`传中文JSON body到后端会被按GBK编码，与UTF-8不匹配导致`JSON parse error`——测试/调试时用纯ASCII英文参数

## 网络管理模块（本次新增）

设计方案详见 `设计/网络管理模块设计方案_V1.0.md`，测试方案详见 `设计/网络管理模块测试方案_V1.0.md`。核心改动：

- **Phase 1**：`sys_cors_origin`/`sys_network_config`/`audit_network_config`三表+种子数据、`DynamicCorsConfigurationSource`、删除`ResourcesConfig`遗留通配符CORS、`application-prod.yml`新建
- **Phase 2**：`SysNetworkConfigController`（`/api/system/network/**`，7接口含权限隔离）、`NetworkConfig.vue`前端页面（4个Tab）、`init-data.sql`中menu/role_menu补充及KBV配置Domain/Mapper
- **Phase 3**：诊断接口、审计埋点（`audit_network_config`）、变更历史查询、`IpUtils`工具类下沉
- **Fix**（测试执行中发现）：`CorsFilter`用`FilterRegistrationBean`设置执行顺序到最高优先级

## 现有设计文档索引

- `设计/_briefs/设计简报.md`：唯一事实基线（甲方背景、需求优先级P0/P1/P2、硬性技术约束、合规安全基线）
- `设计/_briefs/00-技术选型裁决基线.md`：技术栈裁决（Spring Boot 3.4+Java 17/RuoYi-Vue/Vue3+Element Plus/MySQL/Caffeine替代Redis/禁止Docker）
- `设计/_briefs/03-系统架构设计.md`：总体架构/模块划分/部署拓扑
- `设计/_briefs/04-数据架构设计.md`：表结构/数据字典
- `设计/网络管理模块设计方案_V1.0.md`：本次网络管理模块的完整设计
- `设计/网络管理模块测试方案_V1.0.md`：73条测试用例及执行记录汇总
