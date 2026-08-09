# 校园招聘简历管理系统 — HR端问题修复与优化方案

> **文档版本**：V1.0 | **编制日期**：2026-08-08
> **编制方式**：多Agent并行排查（general-purpose 根因分析 + researcher 最佳实践调研）
> **审核状态**：⏳ 待甲方审核

---

## 一、问题总览与根因矩阵

> **行业调研结论**（详见 [`HR系统最佳实践调研.md`](HR系统最佳实践调研.md)）：以下修复方案与 Moka/北森/谷露/Gartner HRM 基准高度一致——KPI卡片需可点击导航、表头排序是事实标准(89%偏好)、pdf.js+sandbox iframe是附件预览行业标准、学生/HR分表管理符合合规最佳实践、ECharts 图表体系是招聘报表主流选择。7项明确不推荐的方案（3D图表、无限分页、纯前端大数据分页、服务端PDF渲染为图片、学生/HR共表、仪表盘图表、无鉴权iframe）已规避。

| 编号 | 问题 | 严重度 | 根因 | 修复人日 |
|------|------|--------|------|---------|
| **B1** | 工作台统计卡片无跳转 | P2 | 卡片仅有 CSS `cursor:pointer`，缺少 `@click` 路由跳转 | 0.3 |
| **B2** | 工作台动态无法展示/跳转 | P2 | ① 字段映射错误：前端读 `r.id` 但后端返 `applicationId` ② 动态条目缺 `@click` | 0.5 |
| **B3** | 岗位列表缺排序功能 | P2 | `el-table-column` 未设 `sortable` 属性，后端 `selectJobList` 排序硬编码 | 1.0 |
| **B4** | 编辑岗位报500 | **P0** | `edit()` 用 `@RequestBody JobPosition` 依赖 Jackson 自动反序列化，但 `add()` 明确注释"避开 Jackson 类型转换"使用手动 Map 解析。el-date-picker 返回 `"2026-12-31"` 无时间部分，Jackson 无法解析为 `LocalDateTime` 抛反序列化异常 | 0.5 |
| **B5** | 岗位模板报500 | **P0** | Controller `list()` 不接收 PageQuery，`JobTemplate` 参数解析触发 `@TableField(fill=...)` 但 **`MetaObjectHandler` 未实现**——`delFlag` 的 `@TableLogic` 行为与 null 实体值结合导致不可预期的 SQL | 0.3 |
| **B6** | 简历列表分页点击无效 | **P0** | 前端 `query.page`/`query.size` 传给后端，后端 `PageQuery` 接收 `pageNum`/`pageSize`，**参数名不匹配** | 0.5 |
| **B7** | 简历详情页信息缺失 + 备注无法保存 | **P0** | ① 后端返回 `studentName`/`gender`，前端读 `snapshotName`/`snapshotGender`——**字段名完全不匹配** ② `snapshotProfile` 是原始 JSON 字符串未解析 ③ 备注发送 `content` vs 后端 `RemarkDTO.noteContent` | 1.0 |
| **B8** | 附件预览 iframe 拒绝连接 | **P0** | ① iframe 作为浏览器原生请求可能被 CSP 响应头阻止 ② ticket 一次性消费后 iframe 二次加载时已过期 ③ `server.address=127.0.0.1` 仅绑 IPv4 环回 | 1.0 |
| **B9** | 数据报表不准确 | P1 | ① 测试数据不足 ② `app_application` DDL 中**不存在** `snapshot_school`/`snapshot_degree` 冗余列——ApplicationMapper SQL 引用不存在的列 ③ 前端 ReportView 图表 option 为静态示例数据 | 1.5 |
| **B10** | HR账号列表分页失效 | **P0** | 同 B6：前端 `page`/`size` vs 后端 `pageNum`/`pageSize` 参数名不匹配，且 `SysUserController.list()` 用**手动 subList 内存截断**（全表查再截断，O(N)） | 0.5 |
| **B11** | HR新增账号非必填项报错 | **P0** | 前端发送 `roleIds`(数组) 非实体字段+`status`=数字，`SysUser` 无这些属性。且**无 `MetaObjectHandler` 实现**——`delFlag`/`userType`/`sex` 等 NOT NULL 列全为 null | 0.5 |
| **B12** | 角色列表分页失效 | **P0** | 同 B6/B10：`SysRoleController.list()` 使用手动 subList + `page`/`size` vs `pageNum`/`pageSize` 不匹配 | 0.3 |
| **B13** | 缺少学生用户管理 | P1 | `stu_user` 表及 Mapper 已存在，缺前端页面和后端管理 Controller | 1.5 |
| | | | **合计** | **9.4 人日** |

---

## 二、分项修复方案

### B1：工作台统计卡片跳转

**根因**：`DashboardView.vue` 中四个 `<el-card>` 统计卡片仅设了 CSS`cursor:pointer` 和 hover 动画，缺少点击事件。

**修复**：每个卡片增加 `@click` 导航：
```html
<el-card @click="$router.push('/recruit/jobs')">...</el-card>       <!-- 在招岗位 → 岗位管理 -->
<el-card @click="$router.push('/recruit/resumes')">...</el-card>    <!-- 今日投递 → 简历管理 -->
<el-card @click="$router.push('/recruit/resumes?status=PENDING')">...</el-card> <!-- 待筛选 → 简历管理+筛选 -->
<el-card @click="$router.push('/recruit/resumes?dateRange=today')">...</el-card> <!-- 本周新增 → 简历管理+日期筛选 -->
```

**行业最佳实践参考**：主流 ATS（北森、Moka）工作台卡片均为可点击，点击后带筛选参数跳转，减少HR操作步骤。

---

### B2：工作台"最近投递动态"展示与跳转

**根因**：
1. 前端 `fetchStats()` 第170行映射 `r.id`，但后端 `ResumeListVO` 的 ID 字段是 `applicationId`
2. 动态条目无点击跳转

**修复**：
1. 字段映射改为 `id: r.applicationId`
2. 路由跳转参数改为 `applicationId`
3. 为 `recent-item` 增加 `@click` 跳转到 `/recruit/resumes/{applicationId}`
4. 生成5-10条真实投递测试数据，确保动态区域有内容

---

### B3：岗位管理列表排序

**现状**：`JobList.vue` 表格列未设 `sortable`，后端 `JobPositionServiceImpl.selectJobList()` 排序固定为 `createTime DESC`。

**修复**：

**前端**：
```html
<el-table-column prop="categoryId" label="岗位分类" sortable="custom" />
<el-table-column prop="title" label="岗位名称" sortable="custom" />
<el-table-column prop="deptId" label="部门" sortable="custom" />
<el-table-column prop="location" label="工作地点" sortable="custom" />
<el-table-column prop="status" label="状态" sortable="custom" />
<el-table-column prop="deadline" label="截止时间" sortable="custom" />
```
监听 `@sort-change` 事件，将 `{prop, order}` 传参给后端。

**后端**：`JobPositionService.selectJobList()` 增加 `sortField`/`sortOrder` 参数，根据前端传值动态 `wrapper.orderBy()`：
```java
if (sortField != null) {
    wrapper.orderBy(true, "ascending".equals(sortOrder), 
        switch(sortField) {
            case "title" -> JobPosition::getTitle;
            case "status" -> JobPosition::getStatus;
            case "location" -> JobPosition::getLocation;
            case "deadline" -> JobPosition::getDeadline;
            case "deptId" -> JobPosition::getDeptId;
            case "categoryId" -> JobPosition::getCategoryId;
            default -> JobPosition::getCreateTime;
        });
}
```

---

### B4：编辑岗位报500

**根因**：`edit()` 用 `@RequestBody JobPosition` 依赖 Jackson 自动反序列化，但 `add()` 方法明确注释了原因——"el-tree-select返回字符串'2'而非数字2，el-date-picker返回'2026-12-31'而非LocalDateTime"。`edit()` 未做同样处理，Jackson 无法将 `"2026-12-31"` 转为 `LocalDateTime`，抛反序列化异常 → 全局异常处理返回 traceId 的 500 响应。

**修复**：`edit()` 方法也改为 `@RequestBody Map<String, Object>` 手动解析（与 `add()` 保持一致）：

---

### B5：岗位模板报500

**根因**：`JobTemplateController.list()` 返回 `AjaxResult.success(list)`（纯数组），但前端 `JobTemplate.vue` 期望 `res.data.rows` + `res.data.total` 分页结构。

**修复**：
```java
@GetMapping("/list")
public AjaxResult list(JobTemplate template, PageQuery pageQuery) {
    // 增加分页支持
    IPage<JobTemplate> page = jobTemplateService.selectTemplatePage(template, pageQuery);
    return AjaxResult.page(TableDataInfo.of((int)page.getTotal(), page.getRecords()));
}
```

---

### B6/B10/B12：分页失效（简历/HR账号/角色列表）

**根因**：**前端与后端参数名不匹配**——前端发送 `page`/`size`，后端 `PageQuery` 类接收 `pageNum`/`pageSize`。

| 对比 | 前端发送 | 后端 `PageQuery` |
|------|---------|-----------------|
| 页码 | `page` | `pageNum` |
| 每页条数 | `size` | `pageSize` |

Spring 参数绑定时找不到 `pageNum`，始终使用默认值 pageNum=1，所以第 2、3 页实际拿到的是第 1 页数据。

**附带问题**：`SysUserController.list()` 和 `SysRoleController.list()` 使用**手动内存截断**（`list.subList(from, to)`），先查全表再截断，数据量大时 OOM 风险。

**修复（推荐：统一方案）**：

**方案一（最小改动）**：前端统一字段名

修改 `ResumeList.vue`、`SysUserList.vue`、`SysRoleList.vue` 的 query 对象：
```javascript
const query = reactive({
  pageNum: 1,    // 改名
  pageSize: 10,  // 改名
  // ...
})
```

以及 API 调用中的 params：
```javascript
params: { pageNum: query.pageNum, pageSize: query.pageSize, ... }
```

**方案二（更彻底）**：修改 `PageQuery` 增加别名支持
```java
@Data
public class PageQuery {
    private int pageNum = 1;
    private int pageSize = 10;
    
    // 兼容前端传 page/size
    public void setPage(int page) { this.pageNum = page; }
    public void setSize(int size) { this.pageSize = size; }
}
```

**推荐方案一**：改动小，不引入隐藏行为。同时修改 `SysUserController.list()` 和 `SysRoleController.list()`，将手动 subList 改为 MyBatis-Plus 分页查询。

---

### B7：简历详情页信息缺失 + 备注无法保存

**根因1（信息缺失）**：前端 `ResumeDetail.vue` 读取 `detail.snapshotName`、`detail.snapshotGender` 等字段，但后端 `ResumeDetailVO` 可能使用不同命名（如 `snapshot_name`→`snapshotName` 需要 Jackson 的 PropertyNamingStrategy 正确配置）。

**根因2（备注保存）**：前端第335行 `handleAddRemark` 发送 `{ content }`，但后端 `RemarkDTO` 的字段名是 `noteContent`。同时 `AppHrNoteServiceImpl` 中 `addRemark()` 的字段映射与前端不一致。

**修复**：
1. 核实 `ResumeDetailVO` 所有字段名与前端 `detail` reactive 对象对齐
2. `snapshotProfile` JSON 列应在后端解析为 `Map<String,Object>` 填充到 VO
3. 备注提交：统一字段名为 `content`（前后端均改为 `content`），或前端改为发送 `{ noteContent: content }`

---

### B8：附件预览 iframe 拒绝连接

**根因**：多种可能叠加：
1. `server.address=127.0.0.1` 仅绑定 IPv4 环回，浏览器可能尝试 IPv6 `::1`
2. iframe 通过 Vite proxy 访问 backend，CORS 白名单已修复但仍可能被浏览器 CSP 策略阻止
3. `CommonFileController.preview()` 的 ticket 一次性消费后立即失效，iframe 二次加载时 ticket 已过期

**修复方案**：
```html
<!-- 方案1：改为 pdf.js 渲染（推荐） -->
<vue-pdf-embed :src="pdfSource" />

<!-- 方案2：iframe 直连 backend（需要 CORS + 明确端口） -->
<iframe :src="`http://127.0.0.1:8080/api/common/file/preview?ticket=${ticket}`" />
```

**推荐方案1**：引入 `vue-pdf-embed`（轻量、零配置），避免跨端口 iframe 安全策略问题。PDF 能浏览器端渲染，Word/DOCX 经 LibreOffice 转 PDF 后再渲染。如果 ticket 已过期，自动重新获取。

---

### B9：数据报表设计与数据

**现状问题**：
1. `ReportView.vue` 中 ECharts 图表使用**硬编码静态数据**，未从后端获取
2. 测试数据只有 24 条投递记录，分布不均
3. `ApplicationMapper` 的 XML SQL 需要验证 `snapshotSchool`/`snapshotDegree` 等冗余列是否正确写入

**修复**：
1. **生成充足测试数据**（50条+ 投递记录），覆盖：
   - 5所不同学校（清华、北大、北航、哈工大、西电）
   - 4种学历（本科/硕士/博士/其他）
   - 3种渠道（官网/校招现场/内部推荐）
   - 3个月时间跨度
2. **后端 SQL 核查**：确认 `ApplicationMapper` 的 `countByApplyDate`/`countBySnapshotSchool`/`countBySnapshotDegree` 等方法的 MyBatis XML SQL 正确
3. **前端图表数据绑定**：修改 `ReportView.vue` 的 `fetchAll()` → 从后端 API 获取真实数据 → 动态更新 ECharts option

**图表映射规范**（行业最佳实践）：

| 报表 | 图表类型 | 数据来源 |
|------|---------|---------|
| 投递趋势 | 折线图 | `GET /api/admin/reports/apply-trend?startDate=&endDate=` |
| 岗位排行 | 横向条形图 | `GET /api/admin/reports/job-ranking?topN=10` |
| 学校分布 | 柱状图 | `GET /api/admin/reports/school-distribution` |
| 学历分布 | 饼图 | `GET /api/admin/reports/degree-distribution` |
| 渠道来源 | 饼图 | `GET /api/admin/reports/source-distribution` |

---

### B11：HR新增账号非必填项报错

**根因**：
1. 前端发送 `roleIds: []`（数组），但 `SysUser` 实体无此字段 → Jackson 反序列化失败
2. `sys_user` 表可能有 NOT NULL 列（`user_type`、`create_by` 等）——需要 `MyMetaObjectHandler` 自动填充

**修复**：
1. **前端**：发送前删除非实体字段
```javascript
async function handleSubmit() {
  const payload = {
    userName: form.userName,
    password: form.password,
    nickName: form.nickName,
    deptId: form.deptId,
    phonenumber: form.phonenumber || undefined,
    email: form.email || undefined,
    status: String(form.status),  // 转 String 匹配后端
    userType: '00',  // 系统用户
  };
  if (isEdit.value) delete payload.password;
  const res = await systemRequest.post('/user', payload);
}
```

2. **后端**：`SysUserController.add()` 补充默认值
```java
if (user.getUserType() == null) user.setUserType("00");
if (user.getStatus() == null) user.setStatus("0");
if (user.getSex() == null) user.setSex("0");
```

---

### B13：学生用户管理模块

**设计原则**：学生用户与HR用户**独立管理**（已有 `stu_user` 独立表），不混入 `sys_user` 体系。学生账号管理在系统管理菜单中单独入口。

**后端**（最小改动）：
- 新增 `StudentManageController`（`/api/admin/students`）
  - `GET /list` — 分页查询（手机号/姓名搜索、状态筛选）
  - `GET /{id}` — 查看详情（含基本资料+教育经历+投递数）
  - `PUT /{id}/status` — 启用/禁用
  - `DELETE /{id}` — 软删除（触发去标识化）

**前端**：
- 新建 `views/system/student/StudentList.vue` — 表格页（手机号/姓名/邮箱/注册时间/状态/投递数）
- 路由：`/system/students`
- Sidebar 菜单项：「学生用户」（位于系统管理子菜单）

**数据库**：无需改动（`stu_user` + `stu_profile` + `stu_education` 已存在）

---

## 三、实施优先级与批次

### 第一批：阻塞修复（1.8 人日）
修复 P0 级阻断项：**B4/B5/B6/B7/B8/B10/B11/B12**

| 任务 | 人日 | 依赖 |
|------|------|------|
| B4 编辑岗位 | 0.5 | — |
| B5 岗位模板 | 0.3 | — |
| B6/B10/B12 分页统一修复 | 0.5 | 涉及 3 个前端页面 + 2 个后端 Controller |
| B7 简历详情信息 | 0.3 | 需读取 ResumeDetailVO 字段 |
| B7 备注保存 | 0.2 | — |
| B8 附件预览 | 0.5 | 引入 vue-pdf-embed |
| B11 HR新增账号 | 0.5 | — |

### 第二批：体验优化（2.1 人日）
| 任务 | 人日 |
|------|------|
| B1 统计卡片跳转 | 0.3 |
| B2 最近投递动态 | 0.5 |
| B3 岗位列表排序 | 0.5 |
| B8 附件预览 (剩余) | 0.5 |

### 第三批：数据与模块（3.0 人日）
| 任务 | 人日 |
|------|------|
| B9 报表数据+测试数据 | 1.5 |
| B13 学生用户管理 | 1.5 |

---

## 四、测试数据设计

为验证修复效果，需生成以下测试数据：

| 实体 | 数量 | 分布 |
|------|------|------|
| 学生 | 30（新增） | 含完整资料、缺资料、不同学校/学历 |
| 投递 | 50（新增） | 覆盖 PENDING_SCREEN(15)、SCREEN_PASSED(10)、ELIMINATED(10)、其余状态各3-5 |
| 岗位 | 8（现有4+新增4） | 覆盖 3 个部门×2个类别 |
| 附件 | 10 | PDF×7 + DOCX×3 |
| 备注 | 15 | 分散到不同投递记录 |

---

## 五、Agent 分工方案

| 阶段 | Agent | 职责 |
|------|-------|------|
| **审核** | 甲方 | 审核本方案 |
| **执行-后端** | `implementer` × 3 | 每批 2-3 个 Bug 并行修复 |
| **执行-前端** | `implementer` × 2 | Vue 页面修改 + 新页面创建 |
| **验证** | `code-validator` | 逐项核对修复后的代码 vs 本方案要求 |
| **测试** | `integration-verifier` | 浏览器实际操作审核，验证每个修复项 |
| **终审** | `adversarial-reviewer` | 红队审查修复后无新漏洞引入 |

---

> **文档结束** | 待甲方审核批准后启动执行
