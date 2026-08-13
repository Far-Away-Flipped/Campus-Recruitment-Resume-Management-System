# HR 权限分级与消息通知优化方案

> 版本：V1.0（对抗性审查定稿）
> 日期：2026-08-13
> 基线来源：system-architect / interface-designer / data-architect 三份产出 + 代码库事实核查 + 对抗性红队审查
> 结论：三处设计分歧已裁决（见 §8），两处 Critical 级越权/无效改动风险已识别并纳入修复范围（见 §9）

---

## 1. 需求概述

### 1.1 HR 端
1. 用户权限分两级：**超级管理员**（所有功能）+ **HR 用户**（仅「工作台」+「招聘管理」）
2. 品牌管理从「招聘管理」移到「系统管理」
3. 增加个人中心（管理个人信息 + 修改密码）
4. HR 更新简历状态（通过/淘汰等）后，同步消息给学生端消息中心

### 1.2 学生端
1. 个人中心增加密码修改
2. 消息中心接收简历状态更新提示

### 1.3 角色与前端分组对齐（代码事实，非假设）

前端菜单由 `recruit-admin-ui/src/layout/Sidebar.vue` **硬编码**，与 DB `sys_menu` 表**脱节**（后者仅元数据，无 UI 消费方，且无 SysMenuController）。实际分组如下：

| 前端分组 | 菜单项 | 对应 Controller |
|---|---|---|
| 工作台 | 工作台 | DashboardView（无后端） |
| 招聘管理 `/recruit` | 岗位管理/岗位模板/学生管理/简历管理/数据报表/岗位类别 | JobAdmin/JobTemplate/StudentManage/ResumeAdmin/Report/JobCategory |
| 系统管理 `/system` | HR账号/角色管理/部门管理/字典管理/通知模板/操作审计/网络管理 | SysUser/SysRole/SysDept/SysDictType+SysDictData/NotifyTemplate/AuditLog/SysNetworkConfig |

**结论**：品牌管理移动的**真正落点是 Sidebar.vue**，不是 `sys_menu`（详见 §9 C1）。

---

## 2. 架构方案

### 2.1 权限分级（ADR-1/2/3 采纳）

- **不引入 `@PreAuthorize`**。沿用项目既有模式：Controller 内手写角色判断。理由：项目零处注解；引入需改 JWT claim 跨 framework 层；`data_scope` 是独立维度（岗位负责人归属），与功能级权限正交。
- **新增 `AdminRoleGuard`**，放 `recruit-biz` 模块（依赖链 common→system→framework→biz，biz 同时能引用 `ISysRoleService` 与 `AdminUserHolder`；且 recruit-admin 依赖 recruit-biz，两处 Controller 均可调用）。
- **安全边界在后端**：前端 Sidebar 过滤 + router 守卫仅是体验优化，可绕过；后端每个系统管理 Controller 加 `requireDirector()` 兜底拦截。

```java
// recruit-biz: com.atmoto.recruit.biz.common.security.AdminRoleGuard
@Component
public class AdminRoleGuard {
    private final ISysRoleService sysRoleService;
    public void requireDirector() {
        Long userId = AdminUserHolder.getUserId();
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED);
        if (!sysRoleService.selectRoleKeysByUserId(userId).contains("hr_director"))
            throw new BizException(ErrorCode.HR_DIRECTOR_ROLE_REQUIRED);
    }
}
```

**需加 `requireDirector()` 的 Controller（系统管理侧，9 个，逐一对齐前端 `/system` 分组）：**

| Controller | 位置 | 说明 |
|---|---|---|
| SysUserController | recruit-admin | HR账号 |
| SysRoleController | recruit-admin | 角色管理 |
| SysDeptController | recruit-admin | 部门管理 |
| SysDictTypeController | recruit-admin | 字典管理（类型） |
| SysDictDataController | recruit-admin | 字典管理（数据） |
| SysNetworkConfigController | recruit-admin | 网络管理（**含 4 个只读接口，见 §9 H1**） |
| BrandConfigController | recruit-biz | 品牌管理（移入系统管理） |
| AuditLogController | recruit-biz | 操作审计 |
| NotifyTemplateController | recruit-biz | 通知模板 |

**明确不加**（招聘管理侧，hr_recruiter 可访问）：`DeptTreeController`（建岗需部门树）、`JobAdminController`、`JobCategoryController`、`JobTemplateController`、`ResumeAdminController`、`ReportController`、`StudentManageController`。

### 2.2 个人中心（ADR-5 增强）

- `GET /api/admin/auth/info` 扩展返回 `{userId, userName, nickName, email, phonenumber, sex, roleKeys, isSuperAdmin}`。
- `PUT /api/admin/auth/profile` 更新本人信息（白名单 nickName/email/phonenumber/sex）。
- `PUT /api/admin/auth/password` 修改密码（校验旧密码 + 强度），成功后**吊销当前 token**（复用已有 `AdminTokenService.revokeToken`，见 §9 H3）。
- 学生端 `POST /api/portal/auth/change-password` 修改密码，**成功后吊销该生所有 refresh token**（复用 `resetPassword` 的吊销逻辑，见 §9 H2）。

### 2.3 消息推送（ADR-4 裁决：同步 + 幂等键兜底）

状态变更后**同步**发送站内信（不 @Async），但用 `dedup_key` 唯一索引防重（见 §8 分歧 1/2）。消息发送点必须在**状态落库 + `app_status_history` 写入之后**，且覆盖**两条**状态变更路径（见 §9 C2）。

---

## 3. 数据库变更（DDL/DML，手动执行脚本）

> 依据 CLAUDE.md：数据库持久化运行，SQL 文件的追加修改不会自动应用，需手动执行。以下脚本可直接在 MySQL 客户端执行。

```sql
-- ============================================================
-- DDL：not_message 增加幂等键 + 索引
-- ============================================================
ALTER TABLE not_message
    ADD COLUMN dedup_key VARCHAR(64) DEFAULT NULL COMMENT '幂等键（APPLICATION_STATUS_CHANGED:{app_status_history.id}），唯一防重'
    AFTER message_type;

-- 唯一索引（MySQL UNIQUE 允许多个 NULL，历史行 dedup_key 为 NULL 不影响 ALTER）
ALTER TABLE not_message ADD UNIQUE KEY uk_dedup_key (dedup_key);
ALTER TABLE not_message ADD KEY idx_message_type_ref (message_type, ref_id);

-- ============================================================
-- DML 1：品牌配置菜单元数据移动（可选，仅文档一致性，不影响 UI）
--   注意：前端 Sidebar.vue 硬编码，真正的 UI 移动在 Sidebar.vue 完成
-- ============================================================
UPDATE sys_menu SET parent_id = 1, order_num = 7 WHERE menu_id = 20;
-- 菜单名对齐（DB 用"业务管理"，需求/前端用"招聘管理"）
UPDATE sys_menu SET menu_name = '招聘管理' WHERE menu_id = 2;

-- ============================================================
-- DML 2：新增 HR 测试账号 recruiter01（绑定 hr_recruiter 角色）
--   password 为 BCrypt 哈希，明文建议 Recruiter@123
--   用 Spring 生成：new BCryptPasswordEncoder().encode("Recruiter@123")
-- ============================================================
INSERT INTO sys_user
    (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, password, status, create_by, create_time, remark)
VALUES
    (2, 2, 'recruiter01', '招聘专员01', '00', 'recruiter01@atmoto.com', '13900000000', '0',
     '<BCrypt("Recruiter@123")>', '0', 'admin', NOW(), 'HR用户测试账号（hr_recruiter角色）');

INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 2);
```

> `sys_user_role` 无实体类。**仅为新增测试账号**时一条 INSERT 足够，不引入 SysUserRoleMapper；仅当「HR账号管理」需要运行时动态分配角色时才新增 Mapper（见 §9 M2）。

---

## 4. 接口设计

### 4.1 HR 端

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | `/api/admin/auth/info` | 扩展返回 userId/userName/nickName/email/phonenumber/sex/roleKeys/isSuperAdmin | 登录 |
| PUT | `/api/admin/auth/profile` | 更新本人信息（白名单 nickName/email/phonenumber/sex） | 登录 |
| PUT | `/api/admin/auth/password` | 改密码：校验旧密码 + 新密码强度，成功后吊销当前 token | 登录 |

### 4.2 学生端

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | `/api/portal/auth/change-password` | 登录后改密码：校验旧密码 + 强度，成功后吊销所有 refresh token | 登录 |
| GET | `/api/portal/messages` | **修正分页契约**：返回 `{rows, total}`（修复当前前端读 `rows/total` 恒空的 bug） | 登录 |
| GET | `/api/portal/messages/unread-count` | 返回未读数 | 登录 |

### 4.3 消息内容与类型

- 消息类型（message_type）：`APPLICATION_STATUS_CHANGED`
- title：「投递进度更新」
- content：「您投递的【{岗位名}】岗位简历状态已更新为：{状态label}」
- recipient：`recipient_type='STUDENT'`，`recipient_id=application.studentId`
- ref_id：`applicationId`（统一为投递记录 ID）
- dedup_key：`APPLICATION_STATUS_CHANGED:{app_status_history.id}`

### 4.4 错误码新增（recruit-common ErrorCode，2xxxx 鉴权段）

```java
OLD_PASSWORD_INCORRECT(20009, "原密码不正确"),
PASSWORD_TOO_WEAK(20010, "新密码强度不足（至少8位）"),
HR_DIRECTOR_ROLE_REQUIRED(20011, "该操作仅限「人力资源总监」角色执行");
```

---

## 5. 前端改动清单（逐文件）

### 5.1 HR 端（recruit-admin-ui）

| 文件 | 改动 |
|---|---|
| `src/stores/auth.js` | `login` 成功后立即调 `getUserInfo()`；`getUserInfo` 保存 `roleKeys/isSuperAdmin/nickName`；新增 getter `isSuperAdmin` |
| `src/router/index.js` | `beforeEach` 增加角色守卫：目标路径以 `/system` 开头且非超管 → `next('/dashboard')`；新增 `/profile` 路由 |
| `src/layout/Sidebar.vue` | ① 系统管理 `<el-sub-menu index="/system">` 加 `v-if="isSuperAdmin"`；② 将 `<el-menu-item index="/recruit/brand">品牌配置</el-menu-item>` 从招聘管理组移到系统管理组 |
| `src/layout/Navbar.vue` | 用户名由硬编码"管理员"改为 `store.userInfo.nickName || userName`；下拉加"个人中心"入口 → `/profile` |
| `src/views/profile/ProfileView.vue`（新增） | 个人中心：显示个人信息 + 修改密码表单（调 PUT `/auth/password`） |
| `src/App.vue` 或 `src/layout/Layout.vue` | 有 token 时 onMounted 调 `getUserInfo()` 以填充 roleKeys（供 Sidebar/守卫使用） |

### 5.2 学生端（recruit-portal-ui）

| 文件 | 改动 |
|---|---|
| `src/views/ProfileView.vue` | 新增「账号安全」Tab：修改密码表单（调 POST `/auth/change-password`） |
| `src/views/MessagesView.vue` | ① 新增未读角标（onMounted 调 `/messages/unread-count`）；② 分页契约已按 `rows/total` 消费，后端修复后自然生效 |
| `src/components/PortalHeader.vue`（可选） | 消息入口显示未读数红点 |

---

## 6. 分阶段实施计划

### Phase 1 — 权限分级（后端安全边界优先）
1. `ErrorCode` 加 `HR_DIRECTOR_ROLE_REQUIRED(20011)`
2. 新建 `AdminRoleGuard`（recruit-biz）
3. 9 个系统管理 Controller 加 `requireDirector()`（含 SysNetworkConfigController 4 个只读接口）
4. 修复 `hasAllDataScope()` bug（`ResumeAdminController` + `ReportController`，改为 role_key 判断）——**这是"超级管理员所有功能"能看全量数据的必要条件**
5. 前端 Sidebar 过滤 + router 守卫 + Navbar 动态用户名
6. `GET /auth/info` 扩展 roleKeys/isSuperAdmin

### Phase 2 — 个人中心
1. `PUT /api/admin/auth/profile`、`PUT /api/admin/auth/password`
2. `POST /api/portal/auth/change-password`（含 refresh token 吊销）
3. HR 端 ProfileView.vue；学生端 ProfileView 加「账号安全」Tab

### Phase 3 — 消息通知
1. DDL：`not_message` 加 `dedup_key` + 唯一索引 + 组合索引；`NotMessage.java` 加 `dedupKey` 字段
2. `NotifyService` 新增同步方法 `sendStatusChangeNotice(...)`
3. `ResumeActionServiceImpl.doScreen()` 与 `ResumeAdminController.changeStatus()` 在 history 写入后调用通知；**同步修复 changeStatus 的 IDOR（§9 C3）**
4. `PortalMessageController` 修复分页契约 + 新增 unread-count
5. 学生端 MessagesView 未读角标

### Phase 4 — 数据 + 验证
1. 执行 §3 DDL/DML；新增 recruiter01 测试账号
2. 跑 §7 验证清单

---

## 7. 验证清单

| # | 验证项 | 预期 |
|---|---|---|
| 1 | recruiter01 登录，Sidebar 只显示「工作台」「招聘管理」，无「系统管理」 | 系统管理组不可见 |
| 2 | recruiter01 直连 `curl /api/system/user/list`（带 token） | 返回 code=20011 拒绝 |
| 3 | recruiter01 直连 `GET /api/system/network/cors-origins` | 返回 code=20011（网络模块只读接口也拦截） |
| 4 | recruiter01 直连 `GET /api/admin/brand/config` | 返回 code=20011（品牌已移入系统管理） |
| 5 | admin 登录 `/auth/info` | 返回 isSuperAdmin=true、roleKeys 含 hr_director |
| 6 | admin 在简历列表能看到所有岗位简历（hasAllDataScope 修复后） | 不再只显示自己名下 |
| 7 | admin 改密码 → 旧 token 立即失效 → 前端跳登录 | 旧 token 调接口返回 20001 |
| 8 | 学生改密码 → 旧 refresh token 失效 → 强制重登 | refresh 返回 token 失效 |
| 9 | admin 对某投递「筛选通过」→ 学生消息中心出现「投递进度更新」 | 消息 content 含岗位名 + "筛选通过" |
| 10 | 对同一投递重复触发状态变更 → 不产生重复消息 | dedup_key 唯一拦截 |
| 11 | 学生消息列表分页显示（第 2 页） | 返回 rows/total，非空 |
| 12 | 学生消息未读角标数字正确 | unread-count 与实际一致 |
| 13 | recruiter01 用任意 applicationId 调 `PUT /resumes/{id}/status` 改不属于自己岗位的简历 | 被 owner 校验拒绝（IDOR 修复后） |

---

## 8. 三个分歧裁决（明确结论）

### 分歧 1：消息发送方式 —— **同步**
采纳 system-architect。理由（基于代码事实）：
- 现有 `NotifyServiceImpl.sendInAppMessage/sendHrNotification` 已标 `@Async`，但那是"投递成功/新申请"通知，调用点在非事务上下文。而状态变更消息的调用点在 `doScreen()`（`@Transactional`，见 `ResumeActionServiceImpl.java:41-45`）内，`@Async` 在事务内会立即在独立线程执行，可能在主事务提交前读到未提交状态（Spring 默认 executor 不等待事务提交），这是经典事务边界 bug。
- 单机 20 并发，插入一条 `not_message` <1ms，异步收益为 0。
- **实现要点**：消息发送用 try-catch 包裹，失败仅 `log.error` 不抛异常——消息是"尽力而为"，不能让消息失败回滚主业务（简历状态变更）。

### 分歧 2：幂等方案 —— **需要 dedup_key + 唯一索引**
采纳 data-architect 的"加 dedup_key"，但**纠正命名**。理由：
- 同步方案下，乐观锁保证每次状态变更只成功一次、消息只发一次，理论上不需防重。
- 但 `batchScreen`（`ResumeActionServiceImpl.java:55`）**无 `@Transactional`**，逐条 `doScreen` 在 autocommit 下执行，若某条失败重试，存在重复插入风险；且未来若切异步/队列，`app_status_history.id` 是天然稳定幂等键。
- 成本极低（一列 + 一唯一索引），收益是防重 + 未来兼容。**幂等键 = `APPLICATION_STATUS_CHANGED:{app_status_history.id}`**（不用 data-architect 的 `RESUME_STATUS_CHANGED` 前缀）。

### 分歧 3：消息类型命名 —— **`APPLICATION_STATUS_CHANGED`**
理由：
- 领域术语：状态属于**投递记录**（`app_application.status` / `applicationId`），不属于**简历**（resume 是文件/内容）。`RESUME_STATUS_CHANGED` 语义错误。
- 现有 `message_type` 已有 `NEW_APPLICATION`、`APPLY_SUCCESS`，用 `APPLICATION` 术语一致。
- `CHANGED`（完成态）比 `CHANGE`（动词原型）更符合现有枚举风格。

---

## 9. 对抗性审查发现（关键风险）

### [Critical] C1：data-architect 的 `sys_menu` DML 对 UI 无效，品牌管理移动的落点错了
- **触发场景**：执行 `UPDATE sys_menu SET parent_id=1 WHERE menu_id=20` 后，前端"品牌配置"仍出现在"招聘管理"分组下。因为 `Sidebar.vue:31` 硬编码 `<el-menu-item index="/recruit/brand">品牌配置</el-menu-item>`，且 `sys_menu` 表无任何 UI 消费方（无 SysMenuController，`router/index.js` 也是硬编码）。
- **影响**：需求 #2（品牌移到系统管理）不生效；数据层改动是无效劳动。
- **修复**：已在 §5.1 把品牌移动落到 Sidebar.vue；`sys_menu` DML 降级为"可选元数据一致性维护"，并标注不影响 UI。

### [Critical] C2：消息发送插入点有两处，interface-designer 只覆盖"状态流转"语义，遗漏核心筛选路径
- **触发场景**：HR 用 `PUT /{applicationId}/screen-pass`（`screenPass`）或 `/batch-screen` 操作简历，走的是 `ResumeActionServiceImpl.doScreen()`，**不经过** `ResumeAdminController.changeStatus()`。若只在 `changeStatus()` 加消息发送，则"筛选通过/淘汰"（最常用操作）不发消息。`doScreen()` 第 163-164 行已有 TODO 标记"异步发站内信通知学生"未实现。
- **影响**：需求 #4 只对"手动状态流转"生效，对核心筛选动作失效。
- **修复**：已在 §2.3/§6 Phase3 明确两条路径都加通知，抽 `notifyStatusChange(application, toStatusLabel)` 复用。

### [Critical] C3：`changeStatus()` 存在 IDOR 越权（计算 hasAllDataScope 但从未使用）
- **触发场景**：`ResumeAdminController.changeStatus()` 第 234-235 行计算了 `currentUserId`/`hasAllDataScope`，但随后直接 `applicationMapper.selectById(applicationId)` 更新，无 owner 校验（对比 `batchScreen` 用 `selectByIdWithScope`）。任意 HR 可用任意 applicationId 修改任意投递状态（只要状态流转合法）。
- **影响**：hr_recruiter 可操作不属于自己岗位的简历；且本任务要在 changeStatus 加消息发送，会把"越权改状态"扩散为"越权给学生发假通知"。
- **修复**：changeStatus 复用 `selectByIdWithScope(applicationId, ownerUserId)` 补齐 owner 校验（既有 bug，本任务触碰该代码顺手修复）。

### [High] H1：hr_recruiter 权限拦截清单需含网络模块 4 个只读接口（推翻原"两角色可读"设计）
- **触发场景**：`SysNetworkConfigController` 的 `listCorsOrigins`/`diagnostics`/`auditList`/`getLanAccess` 目前"两角色可读"（无 `requireNetworkAdminRole`）。新需求下 recruiter 看不到系统管理，但能直连 `/api/system/network/cors-origins` 读白名单。
- **影响**：前端隐藏菜单无法阻止 API 直连；网络模块 Phase2 的"两角色可读"被新需求推翻。
- **修复**：网络管理全部接口（含只读）加 `requireDirector()`。

### [High] H2：学生端改密码未吊销 refresh token，与 resetPassword 安全语义不一致
- **触发场景**：学生改密码后，已签发的 refresh token（7 天，落库 `stu_refresh_token`）仍 ACTIVE，可续命换新 access token。
- **影响**：改密码"强制登出"目标未达成。
- **修复**：`change-password` 复用 `resetPassword`（`PortalAuthServiceImpl.java:302-310`）的吊销逻辑。

### [High] H3：HR 改密码"不吊销 JWT"——已有 revokeToken 能力却不用
- **触发场景**：HR 改密码后，30 分钟内旧 access token 仍有效（`AdminTokenService.isTokenValid` 只查 tokenCache，与密码无关）。
- **影响**：token 泄露场景下改密码后仍可访问至过期。
- **修复**：`PUT /api/admin/auth/password` 从 Authorization 头提取当前 token 调 `adminTokenService.revokeToken(token)`（`AdminTokenService.java:73-75` 能力已存在），前端跳登录页。属 ADR-5 增强，非推翻。

### [Medium] M2：新增 SysUserRoleMapper 对"仅新增测试账号"场景属过度设计
- **触发场景**：仅需插入 recruiter01，一条 `INSERT INTO sys_user_role` 即可；除非 HR账号管理需要运行时分配角色（需求未提）。
- **修复**：默认 seed INSERT；仅在确需动态分配角色时引入 Mapper。

### [Medium] M4：`getMessages` 无分页 + 前端已按 rows/total 消费 → 学生消息列表当前恒空
- **触发场景**：`PortalMessageController.getMessages()` 返回裸 `List`，而 `MessagesView.vue:71-74` 读 `res.data.rows/total`（`axios.js` 拦截器已把 `res` 设为响应体，`res.data` 即 `data` 字段）→ 恒为 `[]`/`0`，学生端消息中心当前显示"暂无消息"。
- **影响**：本次"修正分页契约"顺带修复了一个已存在的显示 bug；验证清单 #11 是回归项。

### [Low] M3：菜单命名"招聘管理 vs 业务管理"不一致
- DB `sys_menu` 用"业务管理"，前端/需求用"招聘管理"。sys_menu 无 UI 影响，已加 `UPDATE sys_menu SET menu_name='招聘管理'` 对齐，属文档一致性。

---

## 10. 风险与缓解

| 风险 | 等级 | 缓解 |
|---|---|---|
| 权限守卫漏覆盖某系统管理接口 | High | §9 H1 已列全 9 Controller；验证清单 #2-#4 用 curl 直连兜底验证 |
| 前端菜单过滤被绕过 | Medium | 后端 `requireDirector()` 为唯一安全边界，前端仅体验层 |
| 消息发送失败回滚主业务 | Medium | 消息 try-catch 吞异常，不影响状态变更 |
| 批筛重复消息 | Low | dedup_key 唯一索引兜底 |
| hasAllDataScope 修复后数据范围语义变化 | Medium | 修复后 hr_director 看全量、hr_recruiter 看自己名下，符合两级权限意图；需回归验证 #6 |
| 改密码后旧 token 未吊销 | Medium | H2/H3 已补吊销逻辑 |
