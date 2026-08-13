<template>
  <div class="network-page">
    <div class="page-header">
      <h2>网络管理</h2>
      <p class="page-desc">管理 CORS 跨域白名单与局域网访问策略，修改后无需重启即可生效</p>
    </div>

    <el-tabs v-model="activeTab" class="network-tabs">
      <!-- ──────── Tab 1：CORS 白名单 ──────── -->
      <el-tab-pane label="CORS白名单" name="cors">
        <div class="toolbar">
          <el-button type="primary" :disabled="!isDirector" @click="handleAddRule">
            <el-icon><Plus /></el-icon> 新增白名单
          </el-button>
          <span v-if="!isDirector" class="perm-tip">当前账号仅可查看，修改需「超级管理员」角色</span>
        </div>

        <el-card shadow="never">
          <el-table :data="corsList" v-loading="corsLoading" stripe border style="width: 100%;">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="ruleType" label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="row.ruleType === 'CIDR' ? 'warning' : ''" size="small">{{ row.ruleType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ruleValue" label="值" min-width="220" show-overflow-tooltip />
            <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.isActive === '1' ? 'success' : 'info'" size="small">
                  {{ row.isActive === '1' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="内置" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.isBuiltin === '1'" type="danger" size="small" effect="plain">内置</el-tag>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" :disabled="!isDirector" @click="handleEditRule(row)">编辑</el-button>
                <el-button type="warning" link size="small" :disabled="!isDirector" @click="handleToggleStatus(row)">
                  {{ row.isActive === '1' ? '停用' : '启用' }}
                </el-button>
                <el-button
                  type="danger" link size="small"
                  :disabled="!isDirector || row.isBuiltin === '1'"
                  @click="handleDeleteRule(row)"
                >删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="ruleDialogVisible" :title="ruleDialogTitle" width="560px" :close-on-click-modal="false">
          <el-form ref="ruleFormRef" :model="ruleForm" :rules="ruleFormRules" label-width="100px" size="default">
            <el-form-item label="规则类型" prop="ruleType">
              <el-select v-model="ruleForm.ruleType" style="width: 100%;" :disabled="isEditRule">
                <el-option label="EXACT（精确Origin匹配）" value="EXACT" />
                <el-option label="CIDR（局域网网段匹配）" value="CIDR" />
              </el-select>
            </el-form-item>
            <el-form-item
              :label="ruleForm.ruleType === 'CIDR' ? '网段' : 'Origin'"
              prop="ruleValue"
            >
              <el-input
                v-model="ruleForm.ruleValue"
                :placeholder="ruleForm.ruleType === 'CIDR' ? '如 192.168.31.0/24' : '如 http://192.168.31.56:5174'"
                maxlength="128"
              />
              <div v-if="ruleForm.ruleType === 'CIDR' && cidrPrefixWarning" class="cidr-warning">
                <el-icon><WarningFilled /></el-icon>
                前缀短于 /16，覆盖的IP数量级较大（后端将强制拒绝短于/16或非RFC1918私有地址段的提交，最终以后端校验为准）
              </div>
            </el-form-item>
            <el-form-item label="说明" prop="description">
              <el-input
                v-model="ruleForm.description"
                type="textarea"
                :rows="2"
                maxlength="256"
                show-word-limit
                placeholder="如：HR办公室WiFi网段"
              />
            </el-form-item>
            <el-form-item label="启用状态" prop="isActive">
              <el-switch v-model="ruleForm.isActive" active-value="1" inactive-value="0" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="ruleDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="ruleSubmitLoading" @click="handleSubmitRule">确定</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- ──────── Tab 2：局域网访问 ──────── -->
      <el-tab-pane label="局域网访问" name="lan">
        <el-card shadow="never" class="lan-card" v-loading="lanLoading">
          <template #header><span class="card-header-title">局域网 / 内网访问开关</span></template>
          <div class="lan-switch-row">
            <div class="lan-switch-desc">
              <p class="lan-switch-title">是否允许局域网/内网IP直接访问HR后台</p>
              <p class="lan-switch-sub">
                该开关是纵深防御配置，不影响 CORS 白名单机制本身；关闭前请确认已了解影响范围。
              </p>
            </div>
            <el-switch
              v-model="lanForm.lanAccessEnabled"
              :disabled="!isDirector"
              active-text="开启"
              inactive-text="关闭"
            />
          </div>
          <div class="lan-save-row">
            <el-button type="primary" :disabled="!isDirector" :loading="lanSaveLoading" @click="handleSaveLanAccess">
              保存
            </el-button>
            <span v-if="!isDirector" class="perm-tip">当前账号仅可查看，修改需「超级管理员」角色</span>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ──────── Tab 3：网络诊断 ──────── -->
      <el-tab-pane label="网络诊断" name="diagnostics">
        <div class="toolbar">
          <el-button :loading="diagLoading" @click="fetchDiagnostics">
            <el-icon><Refresh /></el-icon> 刷新诊断
          </el-button>
          <span class="perm-tip diag-tip">诊断信息为被动读取当前请求上下文，不做任何主动网络探测；请手动点击刷新</span>
        </div>

        <el-card shadow="never" v-loading="diagLoading">
          <template #header><span class="card-header-title">服务器与白名单配置</span></template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="服务器监听地址">{{ diagnostics.serverAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="服务器监听端口">{{ diagnostics.serverPort ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="局域网访问开关">
              <el-tag :type="diagnostics.lanAccessEnabled ? 'success' : 'info'" size="small">
                {{ diagnostics.lanAccessEnabled ? '已开启' : '已关闭' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="当前生效CORS白名单条数">{{ (diagnostics.corsAllowedOrigins || []).length }}</el-descriptions-item>
            <el-descriptions-item label="当前生效CORS白名单" :span="2">
              <div v-if="(diagnostics.corsAllowedOrigins || []).length === 0">-</div>
              <el-tag
                v-for="origin in diagnostics.corsAllowedOrigins"
                :key="origin"
                size="small"
                style="margin: 2px 4px 2px 0"
              >{{ origin }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" style="margin-top: 16px" v-loading="diagLoading">
          <template #header><span class="card-header-title">本次请求上下文（被动推断，非主动探测）</span></template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="来源客户端IP">{{ diagnostics.requestClientIp || '-' }}</el-descriptions-item>
            <el-descriptions-item label="请求Host头">{{ diagnostics.requestHost || '-' }}</el-descriptions-item>
            <el-descriptions-item label="X-Forwarded-For">{{ diagnostics.forwardedForHeader || '（无该请求头）' }}</el-descriptions-item>
            <el-descriptions-item label="X-Forwarded-Proto">{{ diagnostics.forwardedProtoHeader || '（无该请求头）' }}</el-descriptions-item>
            <el-descriptions-item label="是否检测到Nginx反代痕迹" :span="2">
              <el-tag :type="diagnostics.nginxProxyDetected ? 'success' : 'info'" size="small">
                {{ diagnostics.nginxProxyDetected ? '是（存在Forwarded系列请求头）' : '否（未见Forwarded系列请求头，可能是直连）' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-tab-pane>

      <!-- ──────── Tab 4：变更历史 ──────── -->
      <el-tab-pane label="变更历史" name="audit">
        <el-card class="filter-card" shadow="never">
          <el-form :inline="true" :model="auditQuery" size="default">
            <el-form-item label="配置来源">
              <el-select v-model="auditQuery.configType" placeholder="全部" clearable style="width:160px">
                <el-option label="CORS白名单" value="CORS_ORIGIN" />
                <el-option label="局域网开关" value="NETWORK_CONFIG" />
              </el-select>
            </el-form-item>
            <el-form-item label="操作类型">
              <el-select v-model="auditQuery.operationType" placeholder="全部" clearable style="width:160px">
                <el-option v-for="t in auditOpTypes" :key="t" :label="formatAuditOpType(t)" :value="t" />
              </el-select>
            </el-form-item>
            <el-form-item label="操作人">
              <el-input v-model="auditQuery.operatorName" placeholder="搜索操作人" clearable style="width:160px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchAuditList">查询</el-button>
              <el-button @click="resetAuditQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" style="margin-top: 12px">
          <el-table :data="auditList" v-loading="auditLoading" stripe border style="width: 100%;">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="createTime" label="操作时间" width="170" />
            <el-table-column prop="operatorName" label="操作人" width="110" />
            <el-table-column label="配置来源" width="110">
              <template #default="{ row }">
                {{ row.configTable === 'CORS_ORIGIN' ? 'CORS白名单' : row.configTable === 'NETWORK_CONFIG' ? '局域网开关' : (row.configTable || '-') }}
              </template>
            </el-table-column>
            <el-table-column label="操作类型" width="110">
              <template #default="{ row }">
                <el-tag :type="auditTagType(row.operationType)" size="small">{{ formatAuditOpType(row.operationType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operationDetail" label="操作详情" min-width="200" show-overflow-tooltip />
            <el-table-column prop="oldValue" label="变更前" min-width="160" show-overflow-tooltip />
            <el-table-column prop="newValue" label="变更后" min-width="160" show-overflow-tooltip />
            <el-table-column prop="ipAddress" label="来源IP" width="140" />
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="auditQuery.pageNum"
              v-model:page-size="auditQuery.pageSize"
              :total="auditTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @change="fetchAuditList"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import systemRequest from '@/utils/systemRequest';

const activeTab = ref('cors');

// 当前账号是否为「超级管理员」——通过尝试解析登录态角色获取
// 项目现有前端架构无角色下发机制（router.beforeEach仅检查token是否存在，见router/index.js），
// 此处保守默认true（展示层按钮不禁用），真正的权限边界由后端 requireNetworkAdminRole() 承担；
// 一旦写操作被拒绝（NETWORK_ADMIN_ROLE_REQUIRED），立即降级为false并提示，避免用户反复点击写操作被拒
const isDirector = ref(true);

// ──────── CORS 白名单 ────────
const corsList = ref([]);
const corsLoading = ref(false);

async function fetchCorsList() {
  corsLoading.value = true;
  try {
    const res = await systemRequest.get('/network/cors-origins');
    corsList.value = res.data || [];
  } catch {
    // 错误已在拦截器统一提示
  } finally {
    corsLoading.value = false;
  }
}

const ruleDialogVisible = ref(false);
const ruleDialogTitle = ref('新增白名单');
const ruleSubmitLoading = ref(false);
const ruleFormRef = ref(null);
const isEditRule = ref(false);

const ruleForm = reactive({
  id: null,
  ruleType: 'EXACT',
  ruleValue: '',
  description: '',
  isActive: '1',
});

const ruleFormRules = {
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  ruleValue: [{ required: true, message: '请输入规则值', trigger: 'blur' }],
};

const cidrPrefixWarning = computed(() => {
  if (ruleForm.ruleType !== 'CIDR') return false;
  const m = /\/(\d{1,2})$/.exec(ruleForm.ruleValue || '');
  if (!m) return false;
  const prefix = parseInt(m[1], 10);
  return !Number.isNaN(prefix) && prefix < 16;
});

function resetRuleForm() {
  ruleForm.id = null;
  ruleForm.ruleType = 'EXACT';
  ruleForm.ruleValue = '';
  ruleForm.description = '';
  ruleForm.isActive = '1';
  isEditRule.value = false;
  ruleFormRef.value?.resetFields();
}

function handleAddRule() {
  resetRuleForm();
  ruleDialogTitle.value = '新增白名单';
  ruleDialogVisible.value = true;
}

function handleEditRule(row) {
  resetRuleForm();
  isEditRule.value = true;
  ruleForm.id = row.id;
  ruleForm.ruleType = row.ruleType;
  ruleForm.ruleValue = row.ruleValue;
  ruleForm.description = row.description || '';
  ruleForm.isActive = row.isActive;
  ruleDialogTitle.value = '编辑白名单';
  ruleDialogVisible.value = true;
}

async function handleSubmitRule() {
  const valid = await ruleFormRef.value.validate().catch(() => false);
  if (!valid) return;

  const confirmMsg = isEditRule.value
    ? '确认保存对该白名单规则的修改吗？'
    : '该来源发起的跨域请求将被允许携带登录凭证访问所有后台接口，请确认该地址可信。';
  try {
    await ElMessageBox.confirm(confirmMsg, isEditRule.value ? '保存确认' : '新增确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return; // 用户取消
  }

  ruleSubmitLoading.value = true;
  try {
    if (isEditRule.value) {
      await systemRequest.put(`/network/cors-origins/${ruleForm.id}`, {
        ruleType: ruleForm.ruleType,
        ruleValue: ruleForm.ruleValue,
        description: ruleForm.description,
        isActive: ruleForm.isActive,
      });
      ElMessage.success('修改成功');
    } else {
      await systemRequest.post('/network/cors-origins', {
        ruleType: ruleForm.ruleType,
        ruleValue: ruleForm.ruleValue,
        description: ruleForm.description,
        isActive: ruleForm.isActive,
      });
      ElMessage.success('新增成功');
    }
    ruleDialogVisible.value = false;
    fetchCorsList();
  } catch (e) {
    // NETWORK_ADMIN_ROLE_REQUIRED 等权限错误：降级按钮禁用状态，避免用户反复点击被拒
    if (e?.message?.includes('超级管理员')) {
      isDirector.value = false;
    }
  } finally {
    ruleSubmitLoading.value = false;
  }
}

async function handleToggleStatus(row) {
  const target = row.isActive === '1' ? '0' : '1';
  const actionText = target === '1' ? '启用' : '停用';
  try {
    await ElMessageBox.confirm(`确认${actionText}该白名单规则吗？`, `${actionText}确认`, {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await systemRequest.put(`/network/cors-origins/${row.id}/status`, { isActive: target });
    ElMessage.success(`${actionText}成功`);
    fetchCorsList();
  } catch (e) {
    if (e?.message?.includes('超级管理员')) {
      isDirector.value = false;
    }
  }
}

async function handleDeleteRule(row) {
  try {
    await ElMessageBox.confirm(
      `删除后来源「${row.ruleValue}」将立即失去访问权限，确认删除吗？`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    );
  } catch {
    return;
  }
  try {
    await systemRequest.delete(`/network/cors-origins/${row.id}`);
    ElMessage.success('删除成功');
    fetchCorsList();
  } catch (e) {
    if (e?.message?.includes('超级管理员')) {
      isDirector.value = false;
    }
  }
}

// ──────── 局域网访问开关 ────────
const lanLoading = ref(false);
const lanSaveLoading = ref(false);
const lanForm = reactive({ lanAccessEnabled: true });

async function fetchLanAccess() {
  lanLoading.value = true;
  try {
    const res = await systemRequest.get('/network/lan-access');
    lanForm.lanAccessEnabled = !!res.data?.lanAccessEnabled;
  } catch {
    // ignore
  } finally {
    lanLoading.value = false;
  }
}

async function handleSaveLanAccess() {
  const actionText = lanForm.lanAccessEnabled ? '开启' : '关闭';
  try {
    await ElMessageBox.confirm(
      `确认${actionText}局域网/内网访问吗？${lanForm.lanAccessEnabled ? '开启后局域网设备可直接访问后台。' : '关闭后将收紧访问范围，请确认不影响正在使用的终端。'}`,
      `${actionText}确认`,
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    );
  } catch {
    return;
  }
  lanSaveLoading.value = true;
  try {
    await systemRequest.put('/network/lan-access', { lanAccessEnabled: lanForm.lanAccessEnabled });
    ElMessage.success('保存成功');
  } catch (e) {
    if (e?.message?.includes('超级管理员')) {
      isDirector.value = false;
    }
  } finally {
    lanSaveLoading.value = false;
  }
}

onMounted(() => {
  fetchCorsList();
  fetchLanAccess();
});

// ──────── Tab 3：网络诊断 ────────
const diagLoading = ref(false);
const diagnostics = reactive({
  serverAddress: '',
  serverPort: null,
  corsAllowedOrigins: [],
  lanAccessEnabled: false,
  requestClientIp: '',
  requestHost: '',
  forwardedForHeader: '',
  forwardedProtoHeader: '',
  nginxProxyDetected: false,
});

async function fetchDiagnostics() {
  diagLoading.value = true;
  try {
    const res = await systemRequest.get('/network/diagnostics');
    Object.assign(diagnostics, res.data || {});
  } catch {
    // 错误已在拦截器统一提示
  } finally {
    diagLoading.value = false;
  }
}

// ──────── Tab 4：变更历史 ────────
const auditList = ref([]);
const auditTotal = ref(0);
const auditLoading = ref(false);
const auditOpTypes = ref(['ADD', 'UPDATE', 'DELETE', 'ENABLE', 'DISABLE', 'TOGGLE']);

const auditQuery = reactive({
  pageNum: 1,
  pageSize: 20,
  configType: '',
  operationType: '',
  operatorName: '',
});

const auditOpTypeLabels = {
  ADD: '新增',
  UPDATE: '修改',
  DELETE: '删除',
  ENABLE: '启用',
  DISABLE: '停用',
  TOGGLE: '切换',
};

function formatAuditOpType(type) {
  return auditOpTypeLabels[type] || type || '—';
}

function auditTagType(type) {
  const map = {
    ADD: 'success', UPDATE: '', DELETE: 'danger',
    ENABLE: 'success', DISABLE: 'info', TOGGLE: 'warning',
  };
  return map[type] || 'info';
}

async function fetchAuditList() {
  auditLoading.value = true;
  try {
    const params = {
      pageNum: auditQuery.pageNum,
      pageSize: auditQuery.pageSize,
    };
    if (auditQuery.configType) params.configType = auditQuery.configType;
    if (auditQuery.operationType) params.operationType = auditQuery.operationType;
    if (auditQuery.operatorName) params.operatorName = auditQuery.operatorName;

    const res = await systemRequest.get('/network/audit/list', { params });
    if (res.data) {
      auditList.value = res.data.rows || [];
      auditTotal.value = res.data.total || 0;
    }
  } catch {
    auditList.value = [];
  } finally {
    auditLoading.value = false;
  }
}

function resetAuditQuery() {
  auditQuery.configType = '';
  auditQuery.operationType = '';
  auditQuery.operatorName = '';
  auditQuery.pageNum = 1;
  fetchAuditList();
}

// Tab 切换到诊断/变更历史时，若尚无数据则自动首拉一次（诊断页仍保留手动"刷新诊断"按钮，
// 这里只解决"点进Tab却是空白"的问题，不做自动轮询）
watch(activeTab, (tab) => {
  if (tab === 'diagnostics' && !diagnostics.requestHost) {
    fetchDiagnostics();
  } else if (tab === 'audit' && auditList.value.length === 0) {
    fetchAuditList();
  }
});
</script>

<style scoped>
.network-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0 0 4px;
  font-size: 20px;
}

.page-desc {
  color: #909399;
  font-size: 13px;
  margin: 0;
}

.network-tabs {
  background: #fff;
}

.toolbar {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.perm-tip {
  color: #E6A23C;
  font-size: 12px;
}

.diag-tip {
  color: #909399;
}

.filter-card {
  margin-bottom: 0;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.card-header-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.lan-card {
  max-width: 640px;
}

.lan-switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0 16px;
}

.lan-switch-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 4px;
}

.lan-switch-sub {
  font-size: 12px;
  color: #909399;
  margin: 0;
}

.lan-save-row {
  display: flex;
  align-items: center;
  gap: 12px;
  border-top: 1px solid #ebeef5;
  padding-top: 16px;
}

.cidr-warning {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #E6A23C;
  font-size: 12px;
  margin-top: 4px;
}
</style>
