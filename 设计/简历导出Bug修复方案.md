# 简历导出 Bug 修复方案

---

## 1. 问题根因分析

### 1.1 故障链路

```
用户点击「导出Excel」
  → ResumeList.vue:293 POST /resumes/export, responseType: 'blob'
    → axios 将 JSON 响应体当作二进制, response.data 变成 Blob 对象
      → request.js:17 拦截器: blob.code === undefined ≠ 200
        → 走 error 分支, ElMessage.error('请求失败')
          → 前端弹出红色"请求失败"提示
```

### 1.2 根因：`responseType: 'blob'` 与 JSON 响应拦截器的冲突

**后端行为**（`ResumeAdminController.java:423-435`）：

```java
@PostMapping("/export")
public AjaxResult exportResumes(@RequestBody ExportDTO dto, HttpServletResponse response) {
    String filePath = resumeExportService.exportResumeList(...);
    return AjaxResult.success("导出成功", filePath);
    // 返回 JSON: {"code":200, "msg":"导出成功", "data":"E:\\Temp\\recruit-export\\简历导出_xxx.xlsx"}
}
```

后端返回的是**标准 JSON**（`AjaxResult`），不是文件二进制流。文件下载由独立的 `GET /export/download?path=xxx` 端点完成。

**前端错误**（`ResumeList.vue:293`）：

```javascript
const exportRes = await request.post('/resumes/export',
    { applicationIds: allIds },
    { responseType: 'blob' }   // ← 致命参数
);
```

`responseType: 'blob'` 告诉 axios 把响应体当作 `Blob` 对象处理。项目封装的 `request.js` 响应拦截器在此之后执行：

```javascript
// request.js:15-20
response => {
    const { code, msg } = response.data;  // response.data 是 Blob 不是 JSON
    if (code === 200) return response.data;  // blob.code === undefined, 永不进入
    ElMessage.error(msg || '请求失败');      // ← 实际走到这里
    return Promise.reject(new Error(msg));
}
```

**双重失败**：即使拦截器不报错，`handleExport:295` 的 `exportRes.data?.filePath` 也无法从 Blob 中取出文件路径字符串——整个后续下载逻辑都不可达。

### 1.3 与附件预览 Bug 的共性

`ResumeDetail.vue` 的附件预览功能曾经历完全相同的故障（`responseType: 'blob'` + 项目 `request` 拦截器）。该 Bug 已于 `loadPreviewBlob()` 中修复（`ResumeDetail.vue:275-295`），采用的模式是：

- **不用**项目封装的 `request`
- **用原生 `axios`**（import 自 `'axios'`）
- 手动设置 `Authorization` header（从 `localStorage.getItem('admin_token')` 读取 token）
- 需要二进制响应时传 `responseType: 'blob'`，不需要时不传

本修复是对同一类问题、同一套解决模式的第二次应用。

---

## 2. 现有资产

### 2.1 后端已有的两步骤导出 API

| 端点 | 方法 | 功能 | 返回 |
|------|------|------|------|
| `/api/admin/resumes/export` | POST | 接收 `{applicationIds: [...]}`, 生成 Excel 文件 | JSON: `{code:200, msg:"导出成功", data:"文件路径"}` |
| `/api/admin/resumes/export/download` | GET | 接收 `?path=文件路径`, 返回文件流 | 二进制流（含 Content-Type + Content-Disposition） |

**下载端点已实现**：

- 路径安全校验（必须在 `java.io.tmpdir/recruit-export` 目录下，防止路径穿越）
- 正确的 Content-Type（`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`）
- UTF-8 编码的文件名处理（`URLEncoder.encode` + `%20` 替换）

**结论**：下载能力完备，前端只需正确调用两步骤流程，无需重新实现文件下载逻辑。

### 2.2 前端已有的修复先例

`ResumeDetail.vue:275-295` (`loadPreviewBlob`)：

```javascript
const token = localStorage.getItem('admin_token');
const response = await axios.get(
    `/api/admin/resumes/${route.params.id}/attachments/${activeAttachmentId.value}/preview`,
    { responseType: 'blob',
      headers: token ? { Authorization: `Bearer ${token}` } : {} }
);
previewUrl.value = URL.createObjectURL(response.data);
```

核心要素：原生 `axios` + 手动 token + 按需 `responseType`。

---

## 3. 方案对比

### 3.1 方案 A（推荐）：修复前端调用方式，走两步骤流程

**思路**：保持后端不变，修复 `ResumeList.vue` 的 `handleExport()` 函数。

改动：

1. 在 `ResumeList.vue` 的 `<script>` 中新增 `import axios from 'axios'`
2. 用原生 `axios` 发起 POST `/export`（替代 `request.post`），不传 `responseType`
3. 从 `response.data.data` 取出 `filePath`
4. 调用 `GET /export/download?path=${encodeURIComponent(filePath)}` 触发浏览器下载

**下载触发方式**：使用 `<a>` 标签 + `click()`（比 `window.open` 更可靠，不会被弹窗拦截器阻止，且能正确触发浏览器下载行为）。

```javascript
// 伪代码示意
const token = localStorage.getItem('admin_token');
const exportRes = await axios.post('/api/admin/resumes/export',
    { applicationIds: allIds },
    { headers: token ? { Authorization: `Bearer ${token}` } : {} }
);
const filePath = exportRes.data?.data;
// 触发下载
const a = document.createElement('a');
a.href = `/api/admin/resumes/export/download?path=${encodeURIComponent(filePath)}`;
a.download = '';
document.body.appendChild(a);
a.click();
document.body.removeChild(a);
```

| 维度 | 评估 |
|------|------|
| 改动面 | 仅 `ResumeList.vue` 一个文件，约 20 行 |
| 风险 | 极低——复用已有下载端点，不触碰后端 |
| 回滚 | 简单（还原一个文件的改动） |
| 复用度 | 高——复用 `ResumeDetail.vue` 已验证的 `axios` + token 模式 |
| 副作用 | 无——不影响其他调用方 |

### 3.2 方案 B：后端改为直接返回文件流

**思路**：将 `POST /export` 从"返回文件路径"改为"直接返回文件流"，类似附件预览直出端点的做法。前端用原生 `axios` + `responseType: 'blob'` 接收 + `URL.createObjectURL` + `<a>` 下载。

| 维度 | 评估 |
|------|------|
| 改动面 | Controller 方法签名（返回值从 `AjaxResult` 改为 `void`）、Service 层逻辑（可能需调整返回值类型）、前端 `handleExport` |
| 风险 | 中——修改 Controller 方法签名可能影响 AOP 切面、拦截器对 `AjaxResult` 的依赖 |
| 回滚 | 较复杂 |
| 复用度 | 低——已有的 `GET /export/download` 端点变成冗余代码 |
| 副作用 | 破坏两步骤 API 设计一致性（其他导出功能可能遵循同一模式）；错误响应（如日限额超限、权限不足）无法通过 JSON 传递，前端无法区分"导出成功"和"服务器错误返回的 HTML/JSON 正文被当作文件保存" |

### 3.3 结论：采用方案 A

**理由**：

1. **最小改动原则**：仅修改一个前端文件，后端零改动，回归风险最低
2. **复用现有资产**：`GET /export/download` 端点已包含路径校验、Content-Type、编码文件名等完整下载逻辑，重新在前端实现一遍是重复造轮子
3. **遵循已有模式**：`ResumeDetail.vue` 的 `loadPreviewBlob()` 已验证"原生 axios + 手动 token"模式可行
4. **方案 B 的额外风险**：直接流式返回后，前端的 `ElMessage.error` 提示逻辑（如"日累计上限2000条"）无法工作——错误信息也会被当作 Blob，用户看到的是下载了一个损坏文件而不是明确的错误提示

---

## 4. 具体改动

### 4.1 文件：`code/recruit-admin-ui/src/views/recruit/resume/ResumeList.vue`

#### 改动 1：新增 import（第 148 行之后）

```
当前:
import request from '@/utils/request';

改为:
import request from '@/utils/request';
import axios from 'axios';
```

#### 改动 2：重写 `handleExport` 函数（第 278-308 行）

**当前代码**：

```javascript
async function handleExport() {
  try {
    const params = { ...query };
    if (params.dateRange && params.dateRange.length === 2) {
      params.startDate = params.dateRange[0];
      params.endDate = params.dateRange[1];
    }
    delete params.dateRange;
    // 先按筛选条件拉取全部 id
    const allRes = await request.get('/resumes/list', { params: { ...params, pageNum: 1, pageSize: 10000 } });
    const allIds = (allRes.data?.rows || []).map(r => r.applicationId);
    if (allIds.length === 0) {
      ElMessage.warning('没有可导出的数据');
      return;
    }
    const exportRes = await request.post('/resumes/export', { applicationIds: allIds }, { responseType: 'blob' });
    // 如果后端返回 filePath 则跳转下载
    if (exportRes.data?.filePath) {
      window.open(exportRes.data.filePath, '_blank');
    } else if (exportRes instanceof Blob) {
      // 直接 blob 下载
      const url = window.URL.createObjectURL(exportRes);
      const a = document.createElement('a');
      a.href = url;
      a.download = '简历导出.xlsx';
      a.click();
      window.URL.revokeObjectURL(url);
    }
    ElMessage.success('导出成功');
  } catch { /* ignore */ }
}
```

**替换为**：

```javascript
async function handleExport() {
  try {
    // 1. 组装筛选参数
    const params = { ...query };
    if (params.dateRange && params.dateRange.length === 2) {
      params.startDate = params.dateRange[0];
      params.endDate = params.dateRange[1];
    }
    delete params.dateRange;

    // 2. 先按筛选条件拉取全量 ID（复用项目 request，不走 blob 所以安全）
    const allRes = await request.get('/resumes/list', { params: { ...params, pageNum: 1, pageSize: 10000 } });
    const allIds = (allRes.data?.rows || []).map(r => r.applicationId);
    if (allIds.length === 0) {
      ElMessage.warning('没有可导出的数据');
      return;
    }

    // 3. 用原生 axios 发起 POST /export（避开 request.js 响应拦截器）
    const token = localStorage.getItem('admin_token');
    const exportRes = await axios.post('/api/admin/resumes/export',
      { applicationIds: allIds },
      { headers: token ? { Authorization: `Bearer ${token}` } : {} }
    );
    // exportRes.data 是已解析的 JSON: { code: 200, msg: "导出成功", data: "E:\\Temp\\..." }

    if (exportRes.data?.code !== 200) {
      ElMessage.error(exportRes.data?.msg || '导出失败');
      return;
    }

    const filePath = exportRes.data?.data;
    if (!filePath) {
      ElMessage.error('导出文件路径为空');
      return;
    }

    // 4. 通过已有的下载端点触发浏览器下载
    const downloadUrl = `/api/admin/resumes/export/download?path=${encodeURIComponent(filePath)}`;
    const a = document.createElement('a');
    a.href = downloadUrl;
    a.download = '';  // 让服务器 Content-Disposition 决定文件名
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);

    ElMessage.success('导出成功');
  } catch {
    ElMessage.error('导出失败，请重试');
  }
}
```

### 4.2 关键设计决策说明

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 触发下载方式 | `<a>` + `click()` | 比 `window.open` 更可靠，不被弹窗拦截器阻止；下载端点返回 `Content-Disposition: attachment` 确保浏览器下载而非打开 |
| 是否检查 `code !== 200` | 是 | 后端可能返回业务错误（如日限额超限），前端需要展示给用户 |
| catch 中的错误提示 | `'导出失败，请重试'` | 给用户明确的反馈，不静默失败 |
| 文件名 | 由服务器 `Content-Disposition` 决定 | 下载端点已处理中文文件名编码，前端不重复处理 |
| 保留 `request.get('/resumes/list')` | 是 | 该调用不传 `responseType: 'blob'`，返回 JSON，项目 `request` 正常工作 |

---

## 5. 验证方法

### 5.1 功能验证

#### 步骤 1：登录获取 token

```bash
curl.exe -s -X POST "http://127.0.0.1:8080/api/admin/auth/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

从响应中提取 `data.token`，记为 `<TOKEN>`。

#### 步骤 2：拉取简历列表获取 applicationId

```bash
curl.exe -s "http://127.0.0.1:8080/api/admin/resumes/list?pageNum=1&pageSize=5" ^
  -H "Authorization: Bearer <TOKEN>"
```

从响应的 `data.rows` 中取 `applicationId`，记为 `<ID1>,<ID2>`。

#### 步骤 3：调用导出接口（验证 JSON 返回）

```bash
curl.exe -s -X POST "http://127.0.0.1:8080/api/admin/resumes/export" ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer <TOKEN>" ^
  -d "{\"applicationIds\":[<ID1>,<ID2>]}"
```

**预期响应**：

```json
{"code":200,"msg":"导出成功","data":"C:\\Users\\...\\Temp\\recruit-export\\简历导出_xxx.xlsx"}
```

#### 步骤 4：调用下载端点（验证文件流返回）

从步骤 3 响应中取 `data` 字段值，记为 `<FILE_PATH>`。

```bash
curl.exe -s -o "download_test.xlsx" ^
  "http://127.0.0.1:8080/api/admin/resumes/export/download?path=<URL_ENCODED_FILE_PATH>" ^
  -H "Authorization: Bearer <TOKEN>"
```

**预期**：下载的 `download_test.xlsx` 文件可正常打开，内容为步骤 2 中指定简历的 Excel 数据。

#### 步骤 5：浏览器端验证（手动）

1. 打开 HR 管理后台 → 简历管理页面
2. 输入筛选条件（可按岗位/状态筛选）
3. 勾选若干简历，点击「导出Excel」
4. **预期**：浏览器弹出文件下载，文件名包含"简历导出"和日期

### 5.2 边界验证

| 场景 | 操作 | 预期 |
|------|------|------|
| 无数据导出 | 筛选条件下无简历，点导出 | 弹出"没有可导出的数据" |
| 未登录 | 清除 localStorage token 后点导出 | 跳转登录页或提示未登录 |
| 日限额超限 | 当日已导出超过 2000 条后再次导出 | 弹出后端返回的具体错误信息 |
| 筛选结果超 10000 条 | 筛选条件下简历超过 10000 条 | 仅导出前 10000 条（已知限制，见第 6 章） |

---

## 6. 已知限制与后续建议（P1，非本次修复范围）

### 6.1 pageSize=10000 硬编码

`handleExport` 中 `pageSize: 10000` 硬编码限制。当筛选条件下的简历总数超过 10000 时，仅导出前 10000 条，用户无感知。

**建议**（后续单独处理）：

- 在调用 `/resumes/list` 前先查 `total` 数
- 若 `total > 10000`，弹出确认框："筛选结果共 {total} 条，单次最多导出 10000 条，是否继续？"
- 或改为分页循环拉取全量 ID（需评估 10000+ 条时的内存和导出耗时）

### 6.2 导出文件服务端残留

导出生成的 Excel 文件存储在 `java.io.tmpdir/recruit-export/` 目录下，没有定期清理机制。长期运行后磁盘占用会增长。

**建议**：添加定时清理任务（如每日清理 24 小时前的导出文件），或者改为流式生成不落盘。

---

## 7. 改动清单（执行编排器可直接消费）

| 改动ID | 目标文件 | 改动类型 | 内容摘要 | 依赖 | 关键路径 | 验收标准 |
|--------|----------|----------|----------|------|----------|----------|
| EXP-01 | `code/recruit-admin-ui/src/views/recruit/resume/ResumeList.vue:148` | 新增 import | 新增 `import axios from 'axios'` | 无 | 是 | axios 导入无编译错误 |
| EXP-02 | `code/recruit-admin-ui/src/views/recruit/resume/ResumeList.vue:278-308` | 重写函数 | 替换 `handleExport` 函数体（见 4.1 改动 2） | EXP-01 | 是 | 导出功能恢复正常；curl 验证四步均通过；浏览器端手动验证通过 |

---

## 8. 分布式要求登记表

| 要求编号 | 要求描述 | 来源 | 影响范围 | 涉及模块/文件 |
|----------|----------|------|----------|---------------|
| REQ-EXP-01 | POST /export 必须返回标准 JSON（AjaxResult） | 现有后端行为 | 后端不可改为流式返回 | ResumeAdminController.java |
| REQ-EXP-02 | 前端调用 POST /export 不得使用 `responseType: 'blob'` | 本方案 | 前端 | ResumeList.vue |
| REQ-EXP-03 | 前端调用 POST /export 必须使用原生 axios 而非项目封装的 request | 本方案（复用 ResumeDetail.vue 先例） | 前端 | ResumeList.vue |
| REQ-EXP-04 | 文件下载必须通过已有的 GET /export/download 端点 | 本方案（复用已有资产） | 前端 | ResumeList.vue |
| REQ-EXP-05 | 导出失败时必须向用户展示明确的错误信息 | 本方案（用户体验） | 前端 | ResumeList.vue |

---

## 9. 总结

- **根因**：前端使用 `responseType: 'blob'` 接收后端 JSON 响应，导致项目 `request.js` 响应拦截器无法解析 `code` 字段而报错
- **修复**：改用原生 `axios` 发起 POST `/export`（不传 `responseType`），获取 JSON 中的文件路径后，通过已有的 `GET /export/download` 端点触发下载
- **改动面**：仅 `ResumeList.vue` 一个文件，约 25 行改动
- **风险**：极低——复用项目中 `ResumeDetail.vue` 已验证的修复模式，复用后端已有的下载端点
- **回滚**：还原 `ResumeList.vue` 中 `handleExport` 函数的原始版本即可

---

*方案制定日期：2026-08-09*
*关联问题：HR 简历管理页面导出 Excel 功能不可用*
