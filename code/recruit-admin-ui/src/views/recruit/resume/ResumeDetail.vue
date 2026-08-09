<template>
  <div class="resume-detail-page">
    <!-- 顶栏操作 -->
    <div class="detail-toolbar">
      <el-button @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回列表
      </el-button>
      <div class="toolbar-actions">
        <el-button
          v-if="detail.status === 'PENDING_SCREEN'"
          type="success"
          @click="handlePass"
        >
          <el-icon><Select /></el-icon> 通过
        </el-button>
        <el-button
          v-if="detail.status === 'PENDING_SCREEN'"
          type="danger"
          @click="handleEliminate"
        >
          <el-icon><CloseBold /></el-icon> 淘汰
        </el-button>
      </div>
    </div>

    <!-- 主内容：左右分栏 -->
    <el-row :gutter="16" class="detail-main">
      <!-- 左侧：简历信息 -->
      <el-col :xs="24" :md="12" class="detail-left">
        <!-- 基本信息 -->
        <el-card shadow="never" class="info-card">
          <template #header><span class="card-title">基本信息</span></template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="姓名">{{ detail.snapshotName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ detail.snapshotGender || '-' }}</el-descriptions-item>
            <el-descriptions-item label="出生年月">{{ detail.snapshotBirth || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号码">{{ detail.snapshotPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="电子邮箱">{{ detail.snapshotEmail || '-' }}</el-descriptions-item>
            <el-descriptions-item label="现居城市">{{ detail.snapshotCity || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 教育经历 -->
        <el-card shadow="never" class="info-card">
          <template #header><span class="card-title">教育经历</span></template>
          <el-table :data="detail.educations || []" size="small" style="width: 100%;">
            <el-table-column prop="schoolName" label="学校" min-width="140" show-overflow-tooltip />
            <el-table-column prop="major" label="专业" min-width="120" show-overflow-tooltip />
            <el-table-column prop="degree" label="学历" width="80" />
            <el-table-column prop="startDate" label="入学时间" width="110" />
            <el-table-column prop="endDate" label="毕业时间" width="110" />
          </el-table>
          <el-empty v-if="!detail.educations || detail.educations.length === 0" description="暂无教育经历" :image-size="40" />
        </el-card>

        <!-- 投递信息 -->
        <el-card shadow="never" class="info-card">
          <template #header><span class="card-title">投递信息</span></template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="投递岗位">{{ detail.jobTitle || '-' }}</el-descriptions-item>
            <el-descriptions-item label="来源渠道">{{ detail.source || '-' }}</el-descriptions-item>
            <el-descriptions-item label="投递时间">{{ detail.applyTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="筛选状态">
              <el-dropdown
                v-if="availableTransitions.length > 0"
                trigger="click"
                @command="handleStatusChange"
              >
                <el-tag :type="statusTagType(detail.status)" size="small" style="cursor: pointer;">
                  {{ statusLabel(detail.status) }} <el-icon><ArrowDown /></el-icon>
                </el-tag>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="s in availableTransitions"
                      :key="s.code"
                      :command="s.code"
                    >
                      {{ s.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-tag v-else :type="statusTagType(detail.status)" size="small">
                {{ statusLabel(detail.status) }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- HR 内部备注 -->
        <el-card shadow="never" class="info-card">
          <template #header><span class="card-title">HR 内部备注</span></template>
          <div v-if="remarks.length > 0" class="remark-list">
            <div v-for="r in remarks" :key="r.id" class="remark-item">
              <div class="remark-header">
                <span class="remark-user">{{ r.createBy || 'HR' }}</span>
                <span class="remark-time">{{ r.createTime }}</span>
              </div>
              <div class="remark-content">{{ r.content }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无备注" :image-size="40" />
          <el-divider />
          <div class="remark-form">
            <el-input
              v-model="remarkInput"
              type="textarea"
              :rows="3"
              placeholder="添加内部备注..."
              maxlength="500"
              show-word-limit
            />
            <el-button
              type="primary"
              size="small"
              :loading="remarkSubmitting"
              style="margin-top: 8px;"
              @click="handleAddRemark"
            >
              提交备注
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：附件预览 -->
      <el-col :xs="24" :md="12" class="detail-right">
        <el-card shadow="never" class="info-card attachment-card">
          <template #header><span class="card-title">简历附件</span></template>
          <div v-if="attachments.length > 0">
            <div class="attachment-tabs">
              <el-radio-group v-model="activeAttachmentId" size="small" @change="loadPreviewBlob">
                <el-radio-button
                  v-for="att in attachments"
                  :key="att.id"
                  :value="att.id"
                >
                  {{ att.originalName || att.fileName || '附件' }}
                </el-radio-button>
              </el-radio-group>
            </div>
            <div class="preview-frame-wrap">
              <div v-if="previewLoading" style="display:flex;align-items:center;justify-content:center;height:400px;color:#909399;font-size:14px;">
                加载预览中...
              </div>
              <iframe
                v-else-if="previewUrl"
                :src="previewUrl"
                class="preview-frame"
                frameborder="0"
              />
              <el-empty v-else description="请选择附件预览" :image-size="60" />
            </div>
          </div>
          <el-empty v-else description="暂无附件" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown } from '@element-plus/icons-vue';
import request from '@/utils/request';
import axios from 'axios';

const route = useRoute();
const router = useRouter();

const detail = reactive({
  snapshotName: '',
  snapshotGender: '',
  snapshotBirth: '',
  snapshotPhone: '',
  snapshotEmail: '',
  snapshotCity: '',
  jobTitle: '',
  source: '',
  applyTime: '',
  status: '',
  educations: [],
});

const remarks = ref([]);
const remarkInput = ref('');
const remarkSubmitting = ref(false);
const attachments = ref([]);
const activeAttachmentId = ref(null);
const previewUrl = ref('');
const previewLoading = ref(false);
const loading = ref(false);

/** 状态码 -> 中文标签 */
const statusMap = {
  PENDING_SCREEN: '待筛选',
  SCREEN_PASSED: '筛选通过',
  ELIMINATED: '已淘汰',
  PENDING_INTERVIEW: '待面试',
  IN_INTERVIEW: '面试中',
  INTERVIEW_PASSED: '面试通过',
  PENDING_OFFER: '待录用',
  OFFER_SENT: '已发Offer',
  ACCEPTED: '已接受',
  REJECTED: '已拒绝',
  ONBOARDED: '已入职',
};

/** 状态码 -> Element Tag 类型 */
const statusTagMap = {
  PENDING_SCREEN: 'warning',
  SCREEN_PASSED: 'success',
  ELIMINATED: 'danger',
  PENDING_INTERVIEW: 'warning',
  IN_INTERVIEW: '',
  INTERVIEW_PASSED: 'success',
  PENDING_OFFER: 'warning',
  OFFER_SENT: '',
  ACCEPTED: 'success',
  REJECTED: 'danger',
  ONBOARDED: 'info',
};

/** 状态流转规则：当前状态 -> 允许变更为的状态列表（与后端 ResumeAdminController 保持一致） */
const statusFlow = {
  PENDING_SCREEN: ['SCREEN_PASSED', 'ELIMINATED'],
  SCREEN_PASSED: ['PENDING_INTERVIEW', 'ELIMINATED'],
  PENDING_INTERVIEW: ['IN_INTERVIEW', 'ELIMINATED'],
  IN_INTERVIEW: ['INTERVIEW_PASSED', 'ELIMINATED'],
  INTERVIEW_PASSED: ['PENDING_OFFER', 'ELIMINATED'],
  PENDING_OFFER: ['OFFER_SENT', 'ELIMINATED'],
  OFFER_SENT: ['ACCEPTED', 'REJECTED'],
  ACCEPTED: ['ONBOARDED'],
};

/** 当前可用的状态流转选项 */
const availableTransitions = computed(() => {
  const codes = statusFlow[detail.status] || [];
  return codes.map(code => ({ code, label: statusMap[code] || code }));
});

function statusLabel(s) {
  return statusMap[s] || s;
}
function statusTagType(s) {
  return statusTagMap[s] || 'info';
}

async function fetchDetail() {
  loading.value = true;
  try {
    const res = await request.get(`/resumes/${route.params.id}`);
    const d = res.data;
    Object.keys(detail).forEach(key => {
      if (d[key] !== undefined) detail[key] = d[key];
    });
    remarks.value = d.remarks || [];
    attachments.value = d.attachments || [];
    if (attachments.value.length > 0) {
      activeAttachmentId.value = attachments.value[0].id;
      loadPreviewBlob();
    }
  } catch {
    ElMessage.error('获取简历详情失败');
    router.back();
  } finally {
    loading.value = false;
  }
}

/** 通过 axios 带 auth header 获取文件 blob，创建 blob URL 用于 iframe 预览（与学生端一致） */
async function loadPreviewBlob() {
  if (!activeAttachmentId.value) return;
  if (previewUrl.value && previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value);
  }
  previewLoading.value = true;
  previewUrl.value = '';
  try {
    const token = localStorage.getItem('admin_token');
    const response = await axios.get(
      `/api/admin/resumes/${route.params.id}/attachments/${activeAttachmentId.value}/preview`,
      { responseType: 'blob',
        headers: token ? { Authorization: `Bearer ${token}` } : {} }
    );
    previewUrl.value = URL.createObjectURL(response.data);
  } catch {
    // ignore
  } finally {
    previewLoading.value = false;
  }
}

async function handlePass() {
  try {
    await ElMessageBox.confirm('确认通过该简历的筛选吗？', '确认操作', {
      confirmButtonText: '确认通过',
      type: 'success',
    });
    await request.put(`/resumes/${route.params.id}/screen-pass`);
    detail.status = 'SCREEN_PASSED';
    ElMessage.success('已通过');
  } catch { /* ignore */ }
}

async function handleEliminate() {
  try {
    await ElMessageBox.confirm('确认淘汰该简历吗？', '确认操作', {
      confirmButtonText: '确认淘汰',
      type: 'warning',
    });
    await request.put(`/resumes/${route.params.id}/screen-eliminate`);
    detail.status = 'ELIMINATED';
    ElMessage.success('已淘汰');
  } catch { /* ignore */ }
}

/**
 * 手动状态变更（通过状态下拉菜单触发）
 * <p>状态流转规则由后端校验，前端仅发送请求</p>
 */
async function handleStatusChange(newStatus) {
  try {
    const targetLabel = statusMap[newStatus] || newStatus;
    await ElMessageBox.confirm(
      `确认将状态变更为「${targetLabel}」吗？`,
      '状态变更确认',
      {
        confirmButtonText: '确认变更',
        type: 'warning',
      }
    );
    // 调用后端状态变更接口
    await request.put(`/resumes/${route.params.id}/status`, { status: newStatus });
    detail.status = newStatus;
    ElMessage.success(`状态已变更为「${targetLabel}」`);
  } catch {
    // 取消或错误
  }
}

async function handleAddRemark() {
  const content = remarkInput.value.trim();
  if (!content) {
    ElMessage.warning('请输入备注内容');
    return;
  }
  remarkSubmitting.value = true;
  try {
    await request.post(`/resumes/${route.params.id}/remark`, { noteContent: content });
    ElMessage.success('备注已添加');
    remarkInput.value = '';
    // 重新拉取详情以刷新备注列表
    const res = await request.get(`/resumes/${route.params.id}`);
    remarks.value = res.data?.remarks || [];
  } catch { /* ignore */ } finally {
    remarkSubmitting.value = false;
  }
}

onMounted(() => {
  fetchDetail();
});
</script>

<style scoped>
.resume-detail-page {
  max-width: 1400px;
  margin: 0 auto;
}

.detail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 4px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.detail-main {
  align-items: flex-start;
}

.info-card {
  margin-bottom: 16px;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

/* 备注区 */
.remark-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.remark-item {
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.remark-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12px;
}

.remark-user {
  font-weight: 600;
  color: #303133;
}

.remark-time {
  color: #c0c4cc;
}

.remark-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  word-break: break-all;
}

.remark-form {
  display: flex;
  flex-direction: column;
}

/* 附件预览 */
.attachment-card {
  height: calc(100vh - 150px);
  display: flex;
  flex-direction: column;
}

.attachment-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.attachment-tabs {
  margin-bottom: 12px;
}

.preview-frame-wrap {
  flex: 1;
  min-height: 400px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.preview-frame {
  width: 100%;
  height: 100%;
  min-height: 500px;
}

@media (max-width: 768px) {
  .attachment-card {
    height: auto;
  }
  .preview-frame {
    min-height: 350px;
  }
}
</style>
