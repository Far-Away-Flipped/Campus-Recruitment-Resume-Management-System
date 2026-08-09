# HR端Bug根因分析

> 分析日期：2026-08-08
> 分析范围：校园招聘简历管理系统 HR 后台 (http://127.0.0.1:5174/admin/login)
> 方法：逐文件代码级审查，非推测

---

## Bug 4：岗位编辑报500

### 涉及文件
- `E:\Program\校园招聘简历管理系统\code\recruit-backend\recruit-biz\src\main\java\com\atmoto\recruit\biz\admin\controller\JobAdminController.java`（第133-139行）
- `E:\Program\校园招聘简历管理系统\code\recruit-backend\recruit-biz\src\main\java\com\atmoto\recruit\biz\admin\service\impl\JobPositionServiceImpl.java`（第88-90行）
- `E:\Program\校园招聘简历管理系统\code\recruit-backend\recruit-biz\src\main\java\com\atmoto\recruit\biz\common\domain\JobPosition.java`
- `E:\Program\校园招聘简历管理系统\code\recruit-backend\recruit-common\src\main\java\com\atmoto\recruit\common\core\domain\BaseEntity.java`

### 根因

**根因1（主因）：`edit()` 方法与 `add()` 方法的参数解析方式不一致，存在 Jackson 类型转换问题。**

`add()` 方法（第82-128行）使用 `@RequestBody Map<String, Object> body` 手动解析，代码注释明确说明了原因：
```java
// 手动解析请求体，避开 Jackson 类型转换问题
// （el-tree-select返回字符串"2"而非数字2，el-date-picker返回"2026-12-31"而非LocalDateTime）
```

但 `edit()` 方法（第134行）直接使用 `@RequestBody JobPosition jobPosition`，依赖 Jackson 自动反序列化。当前端编辑表单使用与新增相同的组件（el-tree-select、el-date-picker）时：
- `deptId`、`categoryId` 可能以字符串 `"2"` 形式发送，Jackson 对 `Long` 类型的自动转换可能失败
- `deadline` 以 `"2026-12-31"` 格式发送，缺少时间部分，Jackson 无法解析为 `LocalDateTime`，抛出反序列化异常

**根因2（次因）：`AjaxResult.error(String msg)` 的默认错误码是 500。**

`E:\Program\校园招聘简历管理系统\code\recruit-backend\recruit-common\src\main\java\com\atmoto\recruit\common\core\domain\AjaxResult.java` 第53行：
```java
public static AjaxResult error(String msg) {
    return new AjaxResult(500, msg, null);
}
```
`updateById` 因 `@TableLogic` 返回 0 行时，`edit()` 返回 `AjaxResult.error("修改岗位失败")`，HTTP 状态码虽为 200，但业务 code=500，前端 `request.js` 拦截器检测到 `code !== 200` 即 `ElMessage.error`。

**根因3（辅助）：`job_position` 表 NOT NULL 字段无默认值。**

`init-schema.sql` 中 `location`、`description`、`requirement`、`department_id`、`category_id` 均为 `NOT NULL` 但无 `DEFAULT`。虽然 `updateById` 的 NOT_NULL 策略不会 SET null 字段，但如果某次更新意外包含了这些字段的 null 值，MySQL 会拒绝执行。这一点在 `init-schema.sql` 末尾的维护 SQL 中已有修复（第558-563行标注为 "BUG-5"）。

### 修复方向

1. **`edit()` 方法改为与 `add()` 一致的手动解析**：使用 `@RequestBody Map<String, Object> body` 接收参数，复用 `str()`、`lng()` 辅助方法，处理 deadline 字符串转 `LocalDateTime`
2. 或者：在前端发起编辑请求前，确保 `deptId`/`categoryId` 转为数字类型，`deadline` 转为 ISO 格式（如 `2026-12-31T23:59:59`）
3. 短期可用维护 SQL（已在文件中）给 `job_position` 的 NOT NULL 字段添加默认值

---

## Bug 5：岗位模板报500

### 涉及文件
- `E:\Program\校园招聘简历管理系统\code\recruit-backend\recruit-biz\src\main\java\com\atmoto\recruit\biz\admin\controller\JobTemplateController.java`
- `E:\Program\校园招聘简历管理系统\code\recruit-backend\recruit-biz\src\main\java\com\atmoto\recruit\biz\admin\service\impl\JobTemplateServiceImpl.java`
- `E:\Program\校园招聘简历管理系统\code\recruit-backend\recruit-biz\src\main\java\com\atmoto\recruit\biz\common\domain\JobTemplate.java`
- `E:\Program\校园招聘简历管理系统\code\recruit-backend\recruit-common\src\main\java\com\atmoto\recruit\common\core\domain\BaseEntity.java`

### 根因

**代码层面无明显的 NPE 风险**（`selectTemplateList` 中做了 `template != null` 和 `getTemplateName() != null` 检查）。问题在数据库和配置层面。

**根因1：缺少 `MetaObjectHandler` 实现类。**

`BaseEntity` 声明了 `@TableField(fill = FieldFill.INSERT)` 和 `@TableField(fill = FieldFill.INSERT_UPDATE)`，但**项目中没有实现 MyBatis-Plus 的 `MetaObjectHandler`**。这意味着 `createBy`、`createTime`、`updateBy`、`updateTime`、`delFlag` 在 INSERT 时都不会被自动填充。

`job_template` 表的 `del_flag CHAR(1) NOT NULL DEFAULT '0'` 虽然依赖数据库默认值可以插入，但如果 MyBatis-Plus 在 `insert` 时因 `@TableLogic` 机制显式传入了 `delFlag = null`，则可能覆盖数据库默认值，导致后续 `selectList`（自动加 `WHERE del_flag = '0'`）查不到数据。

**根因2：`@TableLogic` 对继承链的影响。**

`JobTemplate` 继承 `BaseEntity`，`BaseEntity` 声明了 `@TableLogic(value = "0", delval = "2")`。MyBatis-Plus 在处理 `jobTemplateMapper.selectList()` 时自动拼接 `WHERE del_flag = '0'`。如果 `job_template` 表结构不完全匹配 `BaseEntity` 定义的审计字段（例如表中没有 `del_flag` 列，或列名不同），SQL 会直接报 500。

**根因3：`insertTemplate` 没有设置 `templateName` 必填校验。**

Controller 第48-51行直接接收 `@RequestBody JobTemplate template` 调用 `insertTemplate`，没有对 `templateName` 做非空校验。如果前端发送空的 templateName，数据库 `NOT NULL` 约束会触发异常（500）。

### 修复方向

1. 实现 `MetaObjectHandler` 自动填充审计字段（`createBy`、`createTime`、`delFlag` 等）
2. 在 Controller 层对 `add()` 和 `edit()` 添加 `templateName` 非空校验
3. 确保 `job_template` 表的 `del_flag` 列存在且默认值为 `'0'`（DDL 已包含，确认实际数据库已执行）

---

## Bug 6 / Bug 10 / Bug 12：分页失效（简历列表 / HR账号列表 / 角色列表）

### 涉及文件
- 简历列表：`ResumeAdminController.java` + 前端 `ResumeList.vue`
- HR账号：`SysUserController.java` + 前端 `SysUserList.vue`
- 角色：`SysRoleController.java` + 前端 `SysRoleList.vue`
- `PageQuery.java`（后端分页参数）

### 根因

**全部三个列表的根本原因是同一个：前后端分页参数名称不匹配。**

#### 后端期望的参数名

`PageQuery.java` 第12-13行：
```java
private Integer pageNum = 1;   // 当前页码
private Integer pageSize = 10; // 每页条数
```

Spring MVC 根据参数名绑定，期望请求参数为 `pageNum` 和 `pageSize`。

#### 前端实际发送的参数名

**ResumeList.vue** 第157-158行、第183行：
```javascript
const query = reactive({ page: 1, size: 10, ... });
// ...
const res = await request.get('/resumes/list', { params });
// 实际发送: ?page=1&size=10&...
```

**SysUserList.vue** 第167-168行、第220行：
```javascript
const query = reactive({ page: 1, size: 10, ... });
// ...
const res = await systemRequest.get('/user/list', { params: { ...query } });
// 实际发送: ?page=1&size=10&...
```

**SysRoleList.vue** 第99-101行、第128行：
```javascript
const query = reactive({ page: 1, size: 10, ... });
// ...
const res = await systemRequest.get('/role/list', { params: { ...query } });
// 实际发送: ?page=1&size=10&...
```

#### 结果

前端发送 `page` / `size`，后端 `PageQuery` 找不到匹配参数，永远使用默认值 `pageNum=1, pageSize=10`。分页控件的翻页操作实际上每次都查询第1页。

#### SysUserController 和 SysRoleController 的额外问题

`SysUserController.java` 第36-44行和 `SysRoleController.java` 第32-38行使用的是**内存伪分页**而非数据库分页：

```java
List<SysUser> list = userService.selectUserList(user);  // 查出全部数据
int total = list.size();
int from = (pageQuery.getPageNum() - 1) * pageQuery.getPageSize();
int to = Math.min(from + pageQuery.getPageSize(), total);
List<SysUser> pageList = from < total ? list.subList(from, to) : List.of();
return AjaxResult.page(TableDataInfo.of(total, pageList));
```

`selectUserList()` 没有分页参数，每次都全量查询。虽然 `selectList` 不需要分页参数也能工作，但数据量大时性能极差。

`ResumeAdminController` 的简历列表**不同**——它使用了 MyBatis-Plus 的 `Page` 对象（`ResumeQueryServiceImpl.java` 第49行），是真正的数据库分页。但同样因为参数名不匹配，`pageNum`/`pageSize` 永远是默认值。

### 修复方向

**统一方案：前端改参数名，对齐后端。**

三个前端列表页面都需要修改：
```javascript
// 修改前
const query = reactive({ page: 1, size: 10, ... });
// 修改后
const query = reactive({ pageNum: 1, pageSize: 10, ... });
```

同时模板中的 `v-model` 绑定也需要改：
```html
<!-- 修改前 -->
v-model:current-page="query.page"
v-model:page-size="query.size"
<!-- 修改后 -->
v-model:current-page="query.pageNum"
v-model:page-size="query.pageSize"
```

对于 `SysUserController` 和 `SysRoleController`，建议进一步改为使用 MyBatis-Plus `Page` 对象做真正的数据库分页，而非内存 `subList`。

---

## Bug 7：简历详情页信息缺失

### 涉及文件
- `ResumeAdminController.java` 第107-114行（`detail()` 方法）
- `ResumeQueryServiceImpl.java` 第78-192行
- `ResumeDetailVO.java`
- `SnapshotVO.java`
- `AppSnapshot.java`
- 前端 `ResumeDetail.vue`

### 根因

**根因1（核心）：前后端字段名不匹配。**

前端 `ResumeDetail.vue` 第170-182行定义的数据结构和第34-39行读取字段如下：
```javascript
const detail = reactive({
  snapshotName: '',    // 期望字段名
  snapshotGender: '',  // 期望字段名
  snapshotBirth: '',   // 期望字段名
  snapshotPhone: '',   // 期望字段名
  snapshotEmail: '',   // 期望字段名
  snapshotCity: '',    // 期望字段名
  ...
});

// 第34-39行模板中读取:
{{ detail.snapshotName }}
{{ detail.snapshotGender }}
{{ detail.snapshotPhone }}
{{ detail.snapshotEmail }}
```

但后端 `ResumeDetailVO.java` 实际返回的字段名为：
```java
private String studentName;    // ← 不是 snapshotName
private String studentPhone;   // ← 不是 snapshotPhone
private String studentEmail;   // ← 不是 snapshotEmail
private String gender;         // ← 不是 snapshotGender
// 且没有 snapshotBirth、snapshotCity 字段
```

`ResumeQueryServiceImpl.java` 第112-121行，数据来自 `StudentProfile`：
```java
vo.setStudentName(profile.getName());          // "studentName"
vo.setStudentPhone(profile.getPhone());        // "studentPhone"
vo.setStudentEmail(profile.getEmail());        // "studentEmail"
vo.setGender(profile.getGender());             // "gender"
// 没有设置 birth（出生日期）、city（现居城市）
```

前端用 `snapshotXxx` 读取，后端返回 `studentXxx`，完全不匹配。

**根因2：快照数据是原始 JSON 字符串，未做结构化解析。**

`AppSnapshot.java` 第27行：
```java
private String snapshotProfile;  // JSON 字符串（约3KB）
```

`SnapshotVO.java` 第20-21行：
```java
private Object snapshotProfile;     // Jackson 序列化为 JSON 字符串
private Object snapshotEducations;  // 同上
```

后端没有将 `snapshotProfile` 的 JSON 字符串反序列化为结构化对象，直接作为 `String` 返回。前端收到的是一个 JSON 字符串（如 `"{\"name\":\"张三\",...}"`），需要用 `JSON.parse()` 才能读取其中的 `name`、`gender`、`phone` 等字段。`AppSnapshot` 中的 `snapshotProfile` 是包含完整个人资料的 JSON 文本，但前端没有解析它。

**根因3：前端 `fetchDetail()` 使用 `Object.keys(detail)` 做字段匹配，但初始值全为空字符串列在 detail 对象中。**

`ResumeDetail.vue` 第253-255行：
```javascript
const d = res.data;
Object.keys(detail).forEach(key => {
  if (d[key] !== undefined) detail[key] = d[key];
});
```

这里 `detail` 的 key 是 `snapshotName`、`snapshotGender`...，而 `d`（后端返回的 VO）的 key 是 `studentName`、`gender`...。因为 key 完全不对应，`d[snapshotName]` 始终为 `undefined`，数据永远写不进去。

### 修复方向

1. **对齐前后端字段名**：将前端 `ResumeDetail.vue` 的 `snapshotName` 改为 `studentName`，`snapshotGender` 改为 `gender`，`snapshotPhone` 改为 `studentPhone`，`snapshotEmail` 改为 `studentEmail`
2. 或者**修改后端 VO 字段名**，在前端没有历史包袱的情况下改前端更简单
3. `SnapshotVO` 中的 `snapshotProfile` 应改为结构化对象（如 `Map<String, Object>`），或在前端用 `JSON.parse()` 解析后读取子字段
4. 需要补充 `birth`（出生日期）、`city`（现居城市）字段的返回（`StudentProfile` 中可能已有 `birthDate` 和 `currentResidence`，但未填入 VO）

---

## Bug 8：附件预览 localhost 拒绝连接

### 涉及文件
- 前端 `ResumeDetail.vue` 第143-151行、第192行
- `CommonFileController.java`
- `ResumeAdminController.java` 第296-329行（ticket 生成）
- `vite.config.js`
- `request.js`

### 根因

**Vite 代理配置正确（`vite.config.js` 第17行 `proxy: { '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true } }`），iframe 的 `src` 也是相对路径 `/api/common/file/preview?ticket=...`（第192行），代理理论应该生效。**

经过全面审查，问题很可能在以下几个方面：

**根因1：ticket 生成与预览端点在不同 Controller 下，代理路径没问题但请求经过了不同的拦截器。**

`previewTicket` 通过 Axios 调用 `/api/admin/resumes/{id}/attachments/{fileId}/ticket` 获取，baseURL 是 `/api/admin`，走 Vite 代理到 8080。

iframe 的 `src="/api/common/file/preview?ticket=xxx"` 也走 Vite 代理到 8080（`/api` 全匹配）。

两者理论上都能代理。但 iframe 请求是**浏览器原生导航请求**（非 XHR/fetch），可能受到额外限制：
- 浏览器的 CSP（Content Security Policy）可能阻止 iframe 加载
- 后端响应中可能缺少 `X-Frame-Options: SAMEORIGIN` 或 CSP `frame-ancestors` 头，但"连接拒绝"通常出现在网络层

**根因2（最可能）：前端端口 5174 和后端端口 8080 不在同一域名下时，Vite 代理可以转发但 iframe 中预览的文件流可能遇到浏览器安全策略问题。**

但"连接拒绝"（Connection Refused）是 TCP 层面的错误，而非 HTTP 层面的。这说明浏览器根本无法建立 TCP 连接到目标。可能的情况：
- 如果 Vite dev server 崩溃或挂起，iframe 请求到 5174 会得到连接拒绝
- 如果后端 8080 没有运行，Vite 代理到 8080 时连接被拒绝，代理返回 502（而不是"连接拒绝"）

**根因3（可能的实际场景）：用户可能直接访问构建后的前端（例如通过 Spring Boot 静态资源），而非通过 Vite dev server。**

如果 nginx 或 Spring Boot 直接提供前端静态文件，前端在某个端口（如 8080），但前端代码中 iframe 使用绝对 URL `http://127.0.0.1:8080/api/...`。此时如果 8080 端口没有运行，就是连接拒绝。

但从代码看 iframe 的 src 是相对路径 `/api/...`，排除了这个情况。

**根因4（代码层面确切问题）：ticket 是一次性的且 60 秒过期。**

`ResumeAdminController.java` 第323行 `previewTicketCache.put(ticket, fileId)`，`CommonFileController.java` 第63行消费 ticket `previewTicketCache.invalidate(ticket)`。

如果在 iframe 加载前有其他请求意外消费了 ticket（比如浏览器预加载、预检请求），ticket 失效，后端返回错误而非连接拒绝。这不直接导致连接拒绝，但会导致预览失败。

### 修复方向

1. **在 `vite.config.js` 中确认代理配置对所有 `/api/*` 路径有效**（已确认有效）
2. **检查后端是否配置了安全响应头**（如 `X-Frame-Options` 或 CSP `frame-ancestors`），如果有则添加 `X-Frame-Options: SAMEORIGIN` 或对管理后台放宽限制
3. **给 ticket 增加容错**：改为两次使用限制而非一次即焚，或给前端增加重试逻辑
4. **优先排查是否是 Vite 代理对 iframe 请求的处理有 bug**：可通过浏览器 DevTools Network 面板确认 iframe 请求是否到达代理

---

## Bug 9：报表数据不准确

### 涉及文件
- `ReportServiceImpl.java`
- `ApplicationMapper.java` 第124-226行（报表查询 SQL）
- `ReportController.java`

### 根因

**SQL 查询逻辑本身无明显错误**，但数据准确性问题来自以下几个代码层面的原因：

**根因1：报表查询依赖 `app_application` 表冗余列，但数据可能未填充。**

`ApplicationMapper.java` 中的报表 SQL（如 `countBySnapshotSchool`、`countBySnapshotDegree`）查询的是 `app_application` 表的 `snapshot_school`、`snapshot_degree` 冗余列：
```sql
SELECT a.snapshot_school AS name, COUNT(*) AS value
FROM app_application a ...
GROUP BY a.snapshot_school
```

但是 `init-schema.sql` 中 `app_application` 表的 DDL（第263-293行）**没有** `snapshot_school`、`snapshot_major`、`snapshot_degree`、`snapshot_name` 这些列！表中只有 `snapshot_profile`、`snapshot_educations` 等 JSON 列。

虽然 `Application.java` 实体声明了这些字段（注释说 "C-06 裁决：筛选字段冗余为普通列（DB中已实际存在）"），但如果实际数据库没有这些列：
- 报表 SQL 会因为 `Unknown column 'a.snapshot_school'` 直接报 500
- 如果列存在但未填充数据（没人写入这些冗余列），查询结果中 `name` 为 NULL，`GROUP BY NULL` 把所有数据聚合到一行

**根因2：数据范围约束 `ownerUserId` 可能过滤掉了所有数据。**

`ReportController.java` 第141-143行：
```java
private Long resolveOwnerUserId() {
    return hasAllDataScope() ? null : getCurrentUserId();
}
```

如果当前登录的 HR 不是 `sys_admin` 类型（`user_type != "sys_admin"`），`resolveOwnerUserId()` 返回该 HR 的 userId 而非 null。此时报表 SQL 的 `LEFT JOIN job_position jp` 加上 `AND jp.owner_user_id = #{ownerUserId}` 条件。如果：
- `job_position` 表没有 `owner_user_id` 列（DDL 第219行只有 `owner_user_id BIGINT DEFAULT NULL`）
- 或者岗位没有设置 `owner_user_id`

则 LEFT JOIN 匹配不上，报表数据为空。

**根因3：测试数据不足。**

报表的准确性天然依赖数据量。`init-data.sql` 只插入了 1 条 admin 账号和基本配置数据，没有插入测试用的投递记录（`app_application`）。如果数据库中没有投递数据，所有报表返回空数组是预期行为，而非 Bug。

### 修复方向

1. **确认 `app_application` 表是否包含 `snapshot_school`、`snapshot_major`、`snapshot_degree`、`snapshot_name` 列**——如果没有，需要执行 ALTER TABLE 添加这些列，并建立数据填充机制（在投递时将 `snapshot_profile` JSON 中的对应字段同步写入冗余列）
2. **确认 `job_position` 表包含 `owner_user_id` 列**——DDL 中有此列，确保实际数据库已存在
3. **插入足够的测试数据**——在 `init-test-data.sql` 中插入至少 20 条不同状态、不同岗位、不同学校/学历的投递记录
4. 报表页面增加**空数据提示**，区分"暂无数据"和"查询出错"

---

## Bug 11：HR新增必填项报错

### 涉及文件
- `SysUserController.java` 第62-81行
- `SysUserServiceImpl.java` 第64-67行
- `SysUser.java`
- `BaseEntity.java`

### 根因

**根因1（核心）：`SysUser` 从 `BaseEntity` 继承的 `delFlag` 字段，在 MyBatis-Plus `insert` 时可能覆盖数据库默认值。**

`SysUserServiceImpl.insertUser()` 第65-67行：
```java
public int insertUser(SysUser user) {
    return userMapper.insert(user);
}
```

`BaseEntity` 有 `@TableLogic(value = "0", delval = "2")` + `@TableField(fill = FieldFill.INSERT)`。由于**项目没有实现 `MetaObjectHandler`**（全局搜索未找到任何 `MetaObjectHandler` 实现类），`delFlag` 不会被自动填充。MyBatis-Plus 的 `insert` 会生成包含所有**非 null 字段**的 INSERT SQL。如果 `delFlag` 为 null（从 `BaseEntity` 继承），它被排除在 SET 之外，数据库默认值 `'0'` 生效——这没问题。

但 `createBy`、`updateBy` 也没有自动填充，它们是 null，被排除在 INSERT 之外。数据库 `sys_user` 表（RuoYi 框架表）很可能对这些列有 DEFAULT 值。

**根因2：前端表单中 `deptId`、`userType` 等字段可能未传入。**

`SysUser.java` 的字段中：
- `userName`、`password` 由前端传入
- `deptId` 由前端传入（`SysUserList.vue` 第116行有部门选择）
- `userType` 未在表单中出现——可能是数据库 NOT NULL 字段
- `sex`、`avatar`、`loginIp`、`loginDate` 未在表单中出现

如果 `sys_user` 表（RuoYi 框架自带）的某些列是 NOT NULL 但前端没有传入且实体为 null，MyBatis-Plus 生成的 INSERT SQL 不包含这些列，数据库 NOT NULL 约束触发异常（500）。

**根因3：RuoYi 的 `sys_user` 表结构未知。**

本项目的初始化 SQL 中没有 `sys_user` 的建表语句（`sys_user` 是 RuoYi 框架自带表）。无法从本项目的 DDL 确认哪些列是 NOT NULL、哪些有默认值。

### 修复方向

1. 在 `add()` 方法中为 `userType`、`sex`、`status` 等字段设置合理的默认值（如 `userType = "01"` 表示普通 HR）
2. 检查 RuoYi `sys_user` 表的实际列约束，确保所有 NOT NULL 且无 DEFAULT 的字段在 insert 前都有值
3. **实现 `MetaObjectHandler`** 自动填充 `createBy`、`createTime`、`delFlag` 等审计字段——这是多个 Bug（4、5、11）的共同修复需求

---

## Bug 13：缺少学生用户管理

### 涉及文件
- `E:\Program\校园招聘简历管理系统\code\recruit-backend\recruit-admin\src\main\resources\sql\init-schema.sql` 第11-36行
- `E:\Program\校园招聘简历管理系统\code\recruit-backend\recruit-system\src\main\java\com\atmoto\recruit\system\domain\SysUser.java`

### 根因

**`stu_user` 表已存在，但完全没有对应的 Java 实体、Mapper、Service、Controller。**

DDL（第11-36行）明确定义了 `stu_user` 表：
```sql
CREATE TABLE `stu_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '学生ID',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号（登录账号，唯一）',
    `password_hash` VARCHAR(128) NOT NULL COMMENT '密码哈希（BCrypt）',
    `real_name` VARCHAR(64) DEFAULT NULL,
    `email` VARCHAR(128) DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ...
    `del_flag` CHAR(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
);
```

`SysUser.java` 第15行注释明确说明：
```java
/**
 * 系统用户（HR 与管理员）
 * 注意事项：学生有独立的 stu_user 表，不使用本表
 */
```

但是项目中：
- **没有 `StuUser.java` 实体类**
- **没有 `StuUserMapper.java`**
- **没有 `StuUserService.java`**
- **没有学生管理的 Controller**
- **没有前端学生管理页面**

现有代码中涉及学生数据的部分（如 `ResumeQueryServiceImpl`）通过 `StudentProfile`（对应 `stu_profile` 表）间接查询学生信息，但 `stu_user` 账号表本身完全没有管理能力。

### 现状确认

| 组件 | 状态 |
|------|------|
| `stu_user` 表 DDL | 已定义（init-schema.sql） |
| `stu_user` 实体类 | **不存在** |
| `stu_user` Mapper | **不存在** |
| 学生管理 Service | **不存在** |
| 学生管理 Controller | **不存在** |
| 前端学生管理页面（`views/student/`） | **不存在** |

### 修复方向（最小改动方案）

1. **新增实体类** `StuUser.java`：
   - 路径：`recruit-biz/src/main/java/com/atmoto/recruit/biz/common/domain/StuUser.java`
   - 不继承 `BaseEntity`（`stu_user` 表已有独立的审计字段，且 `del_flag` 类型为 CHAR(1)，与 BaseEntity 一致）
   - 字段对应 `stu_user` 表：`id`、`phone`、`passwordHash`、`realName`、`email`、`status`、`lastLoginTime`、`lastLoginIp`、`loginFailCount`、`lockUntil`、`dataRetentionDays`、`autoCleanupDate`、`privacyAgreed`、`privacyAgreedTime`
   - 不需要 `@TableLogic`（`del_flag` 直接声明为 `@TableField`）

2. **新增 Mapper** `StuUserMapper.java`：
   - 路径：`recruit-biz/src/main/java/com/atmoto/recruit/biz/common/mapper/StuUserMapper.java`
   - 继承 `BaseMapper<StuUser>`

3. **新增 Service 接口和实现**：
   - `StuUserService.java` + `StuUserServiceImpl.java`
   - 提供学生列表分页查询（使用 MyBatis-Plus `Page`，不要用内存 subList）、按手机号/姓名搜索、状态启禁用

4. **新增 Controller** `StuUserAdminController.java`：
   - 路径：`recruit-biz/src/main/java/com/atmoto/recruit/biz/admin/controller/StuUserAdminController.java`
   - 端点：`/api/admin/students`
   - 提供分页列表、详情查看、状态变更（ACTIVE/DISABLED/LOCKED）

5. **前端页面**：在 `recruit-admin-ui/src/views/` 下新增学生管理列表页，参照 `SysUserList.vue` 结构，注意使用 `request.js`（`baseURL: '/api/admin'`）

---

## 附：跨 Bug 的共同问题

### 1. 缺少 MetaObjectHandler

**影响 Bug**：4、5、11、13

`BaseEntity` 使用 `@TableField(fill = FieldFill.INSERT)` 和 `@TableField(fill = FieldFill.INSERT_UPDATE)` 但没有对应的 `MetaObjectHandler` 实现类。导致：
- `createTime`、`updateTime` 依赖数据库 DEFAULT 值
- `createBy`、`updateBy` 不会被填充，始终为 NULL
- `delFlag` 依赖数据库 DEFAULT 值，但 `@TableLogic` 的行为在与 null 值结合时可能产生意外结果

**修复**：在 `recruit-common` 模块下新增 `MyMetaObjectHandler.java` 实现 `MetaObjectHandler` 接口。

### 2. 前后端分页参数名不一致

**影响 Bug**：6、10、12

所有前端列表页使用 `page`/`size`，后端 `PageQuery` 使用 `pageNum`/`pageSize`。

**修复**：统一改前端参数名，同时 `SysUserController` 和 `SysRoleController` 改用 MyBatis-Plus `Page` 做真正的数据库分页。

### 3. `AjaxResult.error(String)` 默认 error code = 500

**影响 Bug**：4、5、11

`AjaxResult.error(String msg)` 设置的 `code` 为 500。虽然 HTTP 状态码仍是 200，但前端拦截器检查 `code !== 200` 即弹出错误提示，给用户的感觉就是"报错/500"。业务层应使用更细分的错误码（如 400 参数错误、409 冲突等），而非一律 500。
