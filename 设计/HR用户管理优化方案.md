# HR 用户管理优化方案（定稿 V1.0）

> 日期：2026-08-13
> 类型：设计定稿 + 对抗性审查
> 依据基线：`设计/_briefs/设计简报.md`、`设计/_briefs/00-技术选型裁决基线.md`

---

## 0. 结论摘要

| 项 | 裁决 |
|---|---|
| role_key 改名 | `hr_director → admin`、`hr_recruiter → hr`，后端 5 处硬编码 + 2 处 ErrorCode 文案 + 1 处前端「总监」字符串匹配，共 **8 个改动点**（非任务描述里的 5 处） |
| AT-admin 不可删除 | 前端隐藏删除/禁用按钮 + 后端 `remove`/`changeStatus`/编辑角色 三处拦截（**不止删除一处**） |
| 密码 at123456 | 首选「登录旧密码 → 个人中心修改密码」零工具路径；init-data.sql 用占位哈希并注明需替换 |
| HR 列表「角色」列 | 后端需扩展 `selectUserPage` 返回 `roleIds`/`roleNames`（当前**完全不返回**，且编辑弹窗的角色绑定已坏） |
| 数据清理 | 物理 DELETE（`del_flag` 逻辑删除值是 `'2'` 不是 `'1'`，勿踩坑） |

**整体判断：需按本方案修正后执行（含 4 个 High 风险，均可通过方案内措施规避）。**

---

## 1. 现状核实结论（纠正任务假设）

### 1.1 已核实的文件事实

| 核实项 | 结论 | 证据 |
|---|---|---|
| `SysUserController.list` 是否返回角色 | **否**。`selectUserPage` 返回裸 `SysUser`，无 `roleIds`/`roleNames` | `SysUserController.java:36-41`、`SysUserServiceImpl.java:68-90` |
| `SysUser` 是否有 `del_flag` | **有**，继承 `BaseEntity`，`@TableLogic(value="0", delval="2")` | `BaseEntity.java:37-39` |
| `del_flag` 逻辑删除值 | **`'2'`，不是 `'1'`**（与 RuoYi 一致） | `BaseEntity.java:36` |
| `deleteUserByIds` 是物理还是逻辑删除 | **逻辑删除**（`deleteBatchIds` 经 `@TableLogic` 改写为 `del_flag='2'`） | `SysUserServiceImpl.java:103-105` |
| 角色下拉数据结构 | `SysRole` 有 `roleId/roleName/roleKey/status`，`/role/all` 返回启用角色 | `SysRole.java`、`SysRoleController.java:44-49` |
| `init-schema.sql` 是否含 `sys_user/sys_role/sys_user_role` | **不含**。这些是 RuoYi 框架表，不在自建表脚本中 | 全量 grep `CREATE TABLE sys_` 仅命中 `sys_brand_config/sys_banner/sys_cors_origin/sys_network_config` |

### 1.2 关键结论

1. **角色信息在 HR 列表接口中完全缺失**，且 `SysUserList.vue:305` 的 `handleEdit` 读取 `row.roleIds` 恒为 `undefined` —— 编辑弹窗的角色多选**当前是坏的**（永远空选）。本次「角色」列改造应顺带修复。
2. **角色分配从未落库**：`SysUserController.add` 显式白名单过滤、明确「忽略 roleIds」（`SysUserController.java:59-71`），`edit` 用 `updateById` 也忽略非实体字段。**通过 UI 新建的账号从来没有任何角色**（默认即「无角色 = 非超级管理员 = HR用户」语义）。
3. **role_key 改名是「活库数据操作」**，不走 `init-schema.sql`；同时要同步改 `init-data.sql` 供未来全新部署使用。
4. **前端并非完全不耦合角色**：`NetworkConfig.vue` 用 `e?.message?.includes('总监')` 做权限拒绝的字符串匹配（4 处），改文案会使其静默失效。

---

## 2. 核心裁决

### 裁决 1：权限字符规范化（admin / hr）

- 权限字符唯一合法值：`admin`（超级管理员）、`hr`（HR用户）。
- **后端强校验**（不能只靠前端下拉）：`SysRoleController.add/edit` 增加 `roleKey ∈ {admin, hr}` 白名单校验，非法值直接 `AjaxResult.error("权限字符只能是 admin 或 hr")`。
- **前端下拉**：`SysRoleList.vue:86-88` 的 `el-input` 改为 `el-select`，选项：
  - `admin` / 超级管理员 —— 拥有系统全部功能
  - `hr` / HR用户 —— 仅工作台 + 招聘管理
- 范围解释文案（下拉旁 `el-tooltip` 或灰色说明文字）：
  > 超级管理员（admin）：可访问全部功能，含系统管理、数据报表、网络管理。
  > HR用户（hr）：仅可访问工作台与招聘管理（岗位/简历/学生/报表查看），无系统管理权限。
- **解释文案与真实前端行为一致性已核实**：`Sidebar.vue` 用 `v-if="isSuperAdmin"` 控制「系统管理」菜单，非超管只看到「工作台」+「招聘管理」，与文案吻合。

### 裁决 2：role_key 改名迁移（8 个改动点）

**后端代码 5 处** `"hr_director"` → `"admin"`：

| # | 文件:行 | 场景 |
|---|---|---|
| 1 | `AdminAuthController.java:163` | `getUserInfo` 计算 `isSuperAdmin` |
| 2 | `ResumeAdminController.java:533` | `hasAllDataScope` |
| 3 | `ReportController.java:127` | `hasAllDataScope` |
| 4 | `SysNetworkConfigController.java:269` | `requireNetworkAdminRole` |
| 5 | `AdminRoleGuard.java:28` | `requireDirector` |

**后端文案 2 处**（用户可见错误消息，含「总监」）：

| # | 文件:行 | 改动 |
|---|---|---|
| 6 | `ErrorCode.java:34` `HR_DIRECTOR_ROLE_REQUIRED` | 消息 `"该操作仅限「人力资源总监」角色执行"` → `"该操作仅限超级管理员执行"` |
| 7 | `ErrorCode.java:70` `NETWORK_ADMIN_ROLE_REQUIRED` | 消息同步去掉「人力资源总监」，改为 `"该操作仅限超级管理员执行，当前账号无修改权限"` |

> 枚举名 `HR_DIRECTOR_ROLE_REQUIRED` 保留不改（改名会级联到 `AdminRoleGuard`、`SysNetworkConfigController` 引用，纯噪声无收益）。

**前端 1 处**（字符串匹配，改文案后必须同步）：

| # | 文件:行 | 改动 |
|---|---|---|
| 8 | `NetworkConfig.vue:363/388/409/448` | `e?.message?.includes('总监')` → `includes('超级管理员')`（或更稳健：改为按响应 `code === 70008` 判断，见 §6-H2） |

**种子 `init-data.sql`**（供未来全新部署）：

```sql
-- 第 124-125 行
(1, '超级管理员', 'admin', 1, '1', '0', 'admin', NOW(), '超级管理员：拥有系统全部功能'),
(2, 'HR用户',     'hr',    2, '2', '0', 'admin', NOW(), 'HR用户：仅工作台+招聘管理');
```

**活库迁移 SQL**（对已运行实例执行，按 `role_key` 匹配，幂等）：

```sql
UPDATE sys_role SET role_key='admin', role_name='超级管理员', remark='超级管理员：拥有系统全部功能'
  WHERE role_key='hr_director';
UPDATE sys_role SET role_key='hr',   role_name='HR用户',     remark='HR用户：仅工作台+招聘管理'
  WHERE role_key='hr_recruiter';
```

> 用 `WHERE role_key=...` 而非 `WHERE role_id=...`，避免历史数据 role_id 漂移导致漏改。

### 裁决 3：AT-admin 初始账号

**目标态**：`user_name='AT-admin'`、`nick_name='超级管理员'`、`user_type='00'`、绑定 `admin` 角色、密码 `at123456`。

**密码 BCrypt 哈希生成（两种路径）**：

- **方案 A（首选，零工具、哈希必然正确）**：
  1. 活库迁移只改 `user_name`/`role_key`，**暂不改密码**（保留 admin123 的哈希）。
  2. 用 `admin123` 登录 AT-admin。
  3. 走「个人中心 → 修改密码」（`AdminAuthController.updatePassword`，`AdminAuthController.java:218-260`），把密码改为 `at123456`。
  4. 该接口用全局 `BCryptPasswordEncoder` 重新编码，无需手工生成哈希。
  - 约束核查：`at123456` 共 8 位，含字母 `a/t` + 数字，满足 `updatePassword` 的「≥8 位 + 字母 + 数字」强度校验（`AdminAuthController.java:243-245`），可正常通过。
- **方案 B（离线生成，替换 init-data.sql 占位哈希）**：
  ```java
  import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
  public class GenPwd {
      public static void main(String[] a) {
          System.out.println(new BCryptPasswordEncoder().encode("at123456"));
      }
  }
  ```
  用项目 `spring-security-crypto` 依赖版本编译运行（保证与 Spring 校验一致），输出 `$2a$10$...` 哈希写入 `init-data.sql` 第 160 行。**不要用 htpasswd 的 `$2y$` 前缀**，避免与 Spring 默认 `$2a$` 版本差异的边界风险。

> `init-data.sql:160` 现哈希 `$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2` 是 admin123，**只能作占位，上线前必须替换为 at123456 的哈希**（否则全新部署后的初始密码仍是 admin123，违背需求）。

**不可删除 / 不可破坏（前后端双兜底）**：

- **前端 `SysUserList.vue`**：对 `userName === 'AT-admin'` 的行——
  - 隐藏「删除」按钮；
  - 隐藏「禁用/启用」按钮；
  - 隐藏「编辑」按钮（或保留编辑但禁用角色字段，见 §6-H4 取舍），仅保留「重置密码」。
- **后端 `SysUserController`**：定义常量 `private static final String PROTECTED_ADMIN = "AT-admin";`
  - `remove`：按 `userIds` 查出目标用户，若任一 `userName` 等于 `PROTECTED_ADMIN`，直接 `return AjaxResult.error("超级管理员账号不可删除")`，不执行删除。
  - `changeStatus`：若目标用户是 AT-admin 且 `status` 拟置为 `'1'`（禁用），拒绝。
  - `edit`：若目标是 AT-admin，忽略 `deptId/nickName/phonenumber/email` 之外可能破坏超管身份的字段，**尤其禁止移除其 admin 角色**（详见 §6-H4）。
  - 建议同时用「用户名」而非「userId」定位受保护账号，避免重灌数据后 id 漂移。

### 裁决 4：HR 账号列表「角色」列

**后端扩展**（`roleIds` + `roleNames` 一并返回，同时修复编辑弹窗坏绑定）：

1. `SysUser` 增加两个非表字段：
   ```java
   @TableField(exist = false)
   private List<Long> roleIds;
   @TableField(exist = false)
   private List<String> roleNames;
   ```
2. `SysRoleMapper` 增加批量查询：
   ```java
   @Select("<script>SELECT ur.user_id, r.role_id, r.role_name FROM sys_user_role ur "
         + "JOIN sys_role r ON ur.role_id = r.role_id "
         + "WHERE r.status='0' AND r.del_flag='0' AND ur.user_id IN "
         + "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
   List<Map<String,Object>> selectRolesByUserIds(@Param("userIds") List<Long> userIds);
   ```
   > 注意显式加 `r.del_flag='0'`——`selectRoleKeysByUserId` 现 SQL 漏了该过滤（§6-L1）。
3. `SysUserController.list`：拿到分页 `records` 后，收集 `userId` 列表，调用 `selectRolesByUserIds`，按 `user_id` 分组回填 `roleIds`/`roleNames`。用户数极小（≤ 数十），批量查询一次即可，无 N+1 顾虑。

**前端 `SysUserList.vue`**：新增列
```html
<el-table-column label="角色" min-width="120">
  <template #default="{ row }">
    <el-tag size="small" type="info">{{ (row.roleNames || []).join(' / ') || '未分配' }}</el-tag>
  </template>
</el-table-column>
```

**联动修复**：`edit`/`add` 持久化 `roleIds`（§6-H3），否则「角色」列对新账号永远显示「未分配」。

### 裁决 5：数据清理方案（只保留 AT-admin）

**清理对象**：`sys_user` 中除 AT-admin（`user_id=1`）外的全部测试账号，及其 `sys_user_role` 绑定。

**推荐物理删除**（测试账号是垃圾数据，无保留价值；逻辑删除会残留 `del_flag='2'` 行）：

```sql
-- 1) 先清理账号-角色绑定，避免孤儿数据
DELETE FROM sys_user_role WHERE user_id != 1;

-- 2) 再删账号本身
DELETE FROM sys_user WHERE user_id != 1;

-- 3) 重命名初始账号为 AT-admin（密码暂保持 admin123 哈希，走方案A改密）
UPDATE sys_user SET user_name='AT-admin', nick_name='超级管理员' WHERE user_id = 1;

-- 4) 兜底：确保 AT-admin 绑定 admin 角色（按 role_key 定位，兼容 role_id 漂移）
INSERT INTO sys_user_role (user_id, role_id)
SELECT 1, role_id FROM sys_role WHERE role_key='admin'
ON DUPLICATE KEY UPDATE role_id = role_id;
```

- `sys_user_role` 为 RuoYi 标准表，主键 `(user_id, role_id)`，故 `ON DUPLICATE KEY` 有效。
- 若需保留审计痕迹可用逻辑删除替代：`UPDATE sys_user SET del_flag='2' WHERE user_id != 1;`——**注意值是 `'2'` 不是 `'1'`**。
- 执行前建议 `SELECT user_id, user_name FROM sys_user ORDER BY user_id;` 核对 1~47 范围后再删。

---

## 3. 改动清单汇总

| 序号 | 文件 | 改动 |
|---|---|---|
| B1 | `AdminAuthController.java:163` | `"hr_director"` → `"admin"` |
| B2 | `ResumeAdminController.java:533` | `"hr_director"` → `"admin"` |
| B3 | `ReportController.java:127` | `"hr_director"` → `"admin"` |
| B4 | `SysNetworkConfigController.java:269` | `"hr_director"` → `"admin"` |
| B5 | `AdminRoleGuard.java:28` | `"hr_director"` → `"admin"` |
| B6 | `ErrorCode.java:34/70` | 两处消息去「人力资源总监」，改「超级管理员」 |
| B7 | `SysRoleController.java` | add/edit 增加 `roleKey ∈ {admin,hr}` 白名单校验 |
| B8 | `SysUser.java` | 新增 `roleIds`/`roleNames` 非表字段 |
| B9 | `SysRoleMapper.java` | 新增 `selectRolesByUserIds` 批量查询 |
| B10 | `SysUserController.java` | `list` 回填角色；`remove`/`changeStatus`/`edit` 拦截 AT-admin；`add`/`edit` 持久化 `roleIds` |
| B11 | `SysUserServiceImpl.java` | `insertUser`/`updateUser` 支持角色绑定同步（新增方法或扩展） |
| F1 | `SysRoleList.vue:86-88` | 权限字符改 `el-select` 下拉 + 范围解释 |
| F2 | `SysUserList.vue` | 加「角色」列；AT-admin 行隐藏删除/禁用/编辑按钮 |
| F3 | `NetworkConfig.vue:363/388/409/448` | `includes('总监')` → `includes('超级管理员')`（或改 code 判断） |
| S1 | `init-data.sql` | 角色行改名、账号行改 AT-admin + 占位哈希替换、注释同步 |
| S2 | 活库迁移 SQL | role_key 改名 + 账号清理 + AT-admin 重命名 |

---

## 4. 分阶段实施计划

### 阶段 0：数据备份（必做）
```sql
CREATE TABLE sys_user_bak_20260813 AS SELECT * FROM sys_user;
CREATE TABLE sys_user_role_bak_20260813 AS SELECT * FROM sys_user_role;
CREATE TABLE sys_role_bak_20260813 AS SELECT * FROM sys_role;
```

### 阶段 1：后端代码改动（B1~B11）
1. 先做 role_key 改名（B1~B6）。**本阶段代码用过渡兼容判断**，规避部署时序锁死（§6-H1）：
   ```java
   // 过渡期：兼容新旧 role_key，稳定后删掉旧分支
   return roleKeys.contains("admin") || roleKeys.contains("hr_director");
   ```
2. 再做角色白名单（B7）、列表角色列（B8~B10）、AT-admin 保护（B10）、角色持久化（B10/B11）。
3. 编译：`"E:\Program\校园招聘简历管理系统\下载\apache-maven-3.9.16\bin\mvn" -o package -DskipTests`。

### 阶段 2：活库迁移（S2）+ 种子同步（S1）
1. 执行 §2 裁决 5 的清理 SQL + 裁决 2 的 role_key 改名 SQL。
2. 同步 `init-data.sql`（供未来全新部署）。

### 阶段 3：前端改动（F1~F3）
1. `SysRoleList.vue` 下拉 + 解释文案。
2. `SysUserList.vue` 角色列 + AT-admin 保护。
3. `NetworkConfig.vue` 「总监」匹配改为「超级管理员」。

### 阶段 4：密码替换为 at123456
- 方案 A：登录 AT-admin（admin123）→ 个人中心修改密码为 at123456。
- 方案 B：离线生成哈希替换 `init-data.sql` 占位 + 活库 `UPDATE sys_user SET password='<at123456哈希>' WHERE user_id=1`。

### 阶段 5：收尾
1. 删除过渡期 `contains("hr_director")` 兼容分支（B1~B6 清理）。
2. 重启后端，按 §5 验证清单回归。

---

## 5. 验证清单

| # | 验证项 | 预期 |
|---|---|---|
| 1 | 登录 AT-admin / admin123（阶段4前） | 成功，`getInfo` 返回 `isSuperAdmin=true`，`roleKeys` 含 `admin` |
| 2 | 登录后「系统管理」菜单可见 | 是 |
| 3 | `GET /api/system/user/list` | `rows[0].roleNames` 含「超级管理员」，`roleIds` 含 1 |
| 4 | 角色管理页权限字符下拉 | 仅 `admin`/`hr` 两项，带范围解释 |
| 5 | 直接 `curl.exe POST /api/system/role` 提交 `roleKey="hacker"` | 后端拒绝，返回「权限字符只能是 admin 或 hr」 |
| 6 | `DELETE /api/system/user/1` | 后端拒绝「超级管理员账号不可删除」，AT-admin 仍在 |
| 7 | 前端 HR 列表 AT-admin 行 | 删除/禁用/编辑按钮不可见，仅「重置密码」可见 |
| 8 | 新增 HR 账号并选 `hr` 角色 | 保存后列表「角色」列显示「HR用户」，重登后 `isSuperAdmin=false`，无系统管理菜单 |
| 9 | `NetworkConfig.vue` 用 HR 账号触发写操作 | 前端按钮降级禁用（`isDirector=false`） |
| 10 | 阶段4后登录 AT-admin / at123456 | 成功（密码已替换） |
| 11 | 全新库跑 `init-data.sql` + `init-schema.sql` | 只出现 AT-admin 一个账号，密码为 at123456 哈希 |

---

## 6. 对抗性审查发现（分级）

### [High] H1 — role_key 改名与 5 处代码改动的部署时序会导致超级管理员锁死
- **触发场景**：先执行活库 SQL（`role_key` 已改为 `admin`），但后端 jar 尚未重编译部署（代码仍 `contains("hr_director")`）。此时 `selectRoleKeysByUserId` 返回 `["admin"]`，所有 `contains("hr_director")` 恒 false → `isSuperAdmin=false`、`requireDirector` 抛错 → AT-admin **看不到系统管理菜单、无法访问 `/api/system/**`**，唯一超管账号被自己锁死在门外。反向顺序（先部署代码后改 SQL）同理。
- **影响**：系统管理功能完全瘫痪，且无自愈路径（需再改回 SQL 或再发代码）。
- **修复方向**：阶段 1 用过渡兼容判断 `contains("admin") || contains("hr_director")`，待阶段 5 稳定后删除旧分支；或严格保证 SQL 与新 jar 在同一发布窗口原子落地。

### [High] H2 — NetworkConfig.vue 的 `includes('总监')` 字符串匹配在改文案后静默失效
- **触发场景**：按 B6 把 `NETWORK_ADMIN_ROLE_REQUIRED` 消息改为「该操作仅限超级管理员执行」（去掉了「总监」两字），但 `NetworkConfig.vue:363/388/409/448` 仍 `e?.message?.includes('总监')`。HR 用户触发写操作被拒后，前端判断不到，`isDirector` 保持初始 `true`，写按钮永不降级禁用。
- **影响**：纯 UX 降级（安全边界仍由后端 `requireNetworkAdminRole` 承担），但 HR 用户会反复点击被拒，体验崩坏，且与「范围解释」宣称的权限模型观感冲突。
- **修复方向**：同步改 4 处 `includes('总监')` 为 `includes('超级管理员')`；**更稳健**：改为按响应体 `code` 字段判断（`code===70008`），彻底摆脱消息文本耦合。

### [High] H3 — 角色分配（roleIds）在新增/编辑时从未持久化，「角色」列将显示错误
- **触发场景**：`SysUserController.add` 白名单显式忽略 `roleIds`（`SysUserController.java:59-71`），`edit` 的 `updateById` 也忽略非实体字段（当前 `SysUser` 无 roleIds 字段）。只加「角色」列、不加持久化，则所有通过 UI 新建的账号永远「未分配」角色；编辑弹窗多选保存后也不生效。
- **影响**：「角色」列对真实数据失真；两分类权限模型不闭合（新账号游离于 admin/hr 之外）。
- **修复方向**：`add` 白名单加入 `roleIds`（无则默认绑定 `hr` 角色），写 `sys_user_role`；`edit` 增加角色同步（先删旧绑定再插新绑定）。`SysUserServiceImpl` 增 `insertUserRole(userId, roleIds)` / `deleteUserRoles(userId)`。

### [High] H4 — AT-admin「不可删除」只挡了删除，漏了「禁用」与「移除 admin 角色」两条等价破坏路径
- **触发场景**：仅按字面要求拦截 `remove`。管理员（或自动化脚本）仍可：(a) `PUT /user` 把 AT-admin `status` 置 `'1'`（禁用）→ 唯一超管无法登录；(b) 编辑 AT-admin 时把角色多选清空 → 失去 `admin` 角色 → `isSuperAdmin=false`，系统管理再次锁死。
- **影响**：与删除等效的「软锁死」，且更难察觉。
- **修复方向**：前端 AT-admin 行同时隐藏「禁用/启用」与「编辑」按钮；后端 `changeStatus` 对 AT-admin 禁用操作拦截、`edit` 对 AT-admin 禁止移除 `admin` 角色（或直接禁止修改其角色字段）。

### [Medium] M1 — at123456 是弱密码（超管账号 + 简历个保法数据）
- **触发场景**：`at123456` 全小写、含连续数字串 `123456`、无大写/特殊字符，可被常见字典秒破。若 HR 后台暴露在局域网外（`sys_network_config.lan_access_enabled` 默认 `true`），且未额外限流，存在被撞库风险。
- **影响**：超管失守即全量简历 PII（个保法合规底线）泄露。
- **修复方向**：明确该密码仅限内网/开发态使用；上线前强制首登改密（或要求走阶段 4 换成强密码）；文档中标注「示例密码，生产必须更换」。属需求方显式指定，此处降级接受但必须书面提示。

### [Medium] M2 — 角色管理后端缺 roleKey 白名单（仅前端下拉不够）
- **触发场景**：绕过前端，`curl.exe POST /api/system/role` 提交 `{"roleKey":"hr_director","roleName":"x"}`。当前 `SysRoleController.add` 无白名单，会成功写入。若后续有人误用旧 key 造角色，`selectRoleKeysByUserId` 返回的旧 key 在过渡期 `contains("hr_director")` 兼容分支下仍会被当超管。
- **影响**：两分类约束形同虚设，且与过渡兼容分支叠加放大越权面。
- **修复方向**：B7 白名单校验（本方案已纳入）。

### [Low] L1 — `selectRoleKeysByUserId` 未过滤 `r.del_flag='0'`
- **触发场景**：某角色被逻辑删除（`del_flag='2'`）后，`selectRoleKeysByUserId`（`SysRoleMapper.java:14-16` 原生 `@Select`，不走 `@TableLogic`）仍会返回其 `role_key`，导致被删角色的权限残留。
- **影响**：逻辑删除的角色仍参与鉴权判定。与本次改名正交，但新增 `selectRolesByUserIds` 时应一并纠正。
- **修复方向**：两处 SQL 都加 `AND r.del_flag='0'`。

### [Low] L2 — 术语残留（「人力资源总监」散见注释与 Javadoc）
- **触发场景**：`ResumeAdminController.java:522`、`ReportController.java:118`、`AdminRoleGuard.java:13` 等 Javadoc 仍写「人力资源总监 (hr_director)」。
- **影响**：无功能影响，但与新命名不一致，后续维护易误解。
- **修复方向**：B1~B6 顺手改注释；可作非阻塞项。

### [Low] L3 — sys_role_menu 种子与「HR用户=工作台+招聘管理」描述不符（无实际影响）
- **触发场景**：`init-data.sql:137-138` 给 role_id=2（hr）配的是 menu 2/21/22（业务管理+岗位类别+Banner），与「工作台+招聘管理」文案不一致。
- **影响**：**无**。前端 `Sidebar.vue` 纯静态、仅按 `isSuperAdmin` 控制菜单，`sys_role_menu` 不被任何 Controller 消费（CLAUDE.md 已注明「`sys_menu.perms` 只是元数据」）。属死数据。
- **修复方向**：可顺手把 role_id=2 的 sys_role_menu 改为与文案一致，或留注释说明「未消费」。非阻塞。

### [Low] L4 — 用户名大小写不敏感
- **触发场景**：MySQL 默认 `utf8mb4_0900_ai_ci` 大小写不敏感，`AT-admin` 与 `at-admin` 均可匹配登录。
- **影响**：无安全影响，仅提示 `AT-admin` 的「大写 AT」是展示约定而非唯一性约束。
- **修复方向**：无需处理，文档备注即可。

---

## 7. 风险总表与回滚

| 风险 | 等级 | 缓解 | 回滚 |
|---|---|---|---|
| 部署时序锁死超管 | High | 过渡兼容判断（H1） | 回滚 SQL：`UPDATE sys_role SET role_key='hr_director' WHERE role_key='admin'` |
| 前端「总监」匹配失效 | High | 同步改文案/改 code 判断（H2） | 前端回滚 NetworkConfig.vue |
| 角色持久化缺失 | High | 阶段 1 一并补（H3） | — |
| AT-admin 软锁死 | High | 前端隐藏 + 后端三处拦截（H4） | 回滚 SQL：恢复 `sys_user_role`、`status='0'` |
| 弱密码 | Medium | 内网限定 + 首登改密（M1） | — |
| 无白名单 | Medium | B7 校验（M2） | — |

**回滚主路径**：阶段 0 已备份 `sys_user/sys_user_role/sys_role`，任何数据层故障用 `_bak_20260813` 三表恢复；代码层故障用 git revert。
