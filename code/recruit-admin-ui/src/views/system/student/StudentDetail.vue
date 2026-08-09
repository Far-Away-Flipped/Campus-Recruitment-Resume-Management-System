<template>
  <div class="student-detail-page" v-loading="loading">
    <!-- 顶栏 -->
    <div class="detail-toolbar">
      <el-button @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回列表
      </el-button>
      <span class="toolbar-title">{{ detail.realName || '-' }}</span>
      <div>
        <el-button
          v-if="detail.status === 'ACTIVE'"
          type="warning"
          @click="handleToggleStatus"
        >
          禁用账号
        </el-button>
        <el-button
          v-else
          type="success"
          @click="handleToggleStatus"
        >
          启用账号
        </el-button>
      </div>
    </div>

    <!-- 基本信息 -->
    <el-card shadow="never" class="info-card">
      <template #header><span class="card-title">基本信息</span></template>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="姓名">{{ detail.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ genderLabel(detail.gender) }}</el-descriptions-item>
        <el-descriptions-item label="手机号码">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="电子邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ detail.birthDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所在城市">{{ detail.currentCity || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="detail.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
            {{ detail.status === 'ACTIVE' ? '正常' : '已禁用' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 教育经历 -->
    <el-card shadow="never" class="info-card">
      <template #header><span class="card-title">教育经历</span></template>
      <el-table :data="detail.educations || []" size="small" style="width: 100%;">
        <el-table-column prop="schoolName" label="学校" min-width="160" show-overflow-tooltip />
        <el-table-column prop="major" label="专业" min-width="140" show-overflow-tooltip />
        <el-table-column prop="degree" label="学历" width="80" />
        <el-table-column prop="startDate" label="入学时间" width="110" />
        <el-table-column prop="endDate" label="毕业时间" width="110" />
      </el-table>
      <el-empty v-if="!detail.educations || detail.educations.length === 0" description="暂无教育经历" :image-size="40" />
    </el-card>

    <!-- 实习/项目经历 -->
    <el-card v-if="detail.internships && detail.internships.length > 0" shadow="never" class="info-card">
      <template #header><span class="card-title">实习/项目经历</span></template>
      <el-table :data="detail.internships" size="small" style="width: 100%;">
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            {{ row.recordType === 'I' ? '实习经历' : '项目经历' }}
          </template>
        </el-table-column>
        <el-table-column prop="orgName" label="公司/项目" min-width="140" show-overflow-tooltip />
        <el-table-column prop="position" label="岗位/角色" min-width="120" show-overflow-tooltip />
        <el-table-column label="起止时间" width="200">
          <template #default="{ row }">
            {{ row.startDate || '-' }} ~ {{ row.endDate || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-card>

    <!-- 简历附件 -->
    <el-card v-if="detail.resumeFiles && detail.resumeFiles.length > 0" shadow="never" class="info-card">
      <template #header><span class="card-title">简历附件</span></template>
      <div class="file-list">
        <div v-for="f in detail.resumeFiles" :key="f.id" class="file-item">
          <span class="file-name">{{ f.originalName }}</span>
          <span class="file-meta">上传于 {{ f.uploadTime }} · {{ formatFileSize(f.fileSize) }}</span>
        </div>
      </div>
    </el-card>

    <!-- 投递历史 -->
    <el-card shadow="never" class="info-card">
      <template #header><span class="card-title">投递历史（最近5条）</span></template>
      <el-table :data="detail.applications || []" size="small" style="width: 100%;">
        <el-table-column prop="jobTitle" label="投递岗位" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="job-link" @click="goResumeDetail(row)">{{ row.jobTitle }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="投递时间" width="170" />
        <el-table-column label="当前状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ row.statusLabel || row.status }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!detail.applications || detail.applications.length === 0" description="暂无投递记录" :image-size="40" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import request from '@/utils/request';

const route = useRoute();
const router = useRouter();
const loading = ref(false);

const detail = reactive({
  studentId: null, phone: '', realName: '', email: '', gender: '',
  birthDate: '', currentCity: '', avatarUrl: '', status: '', createTime: '',
  educations: [], internships: [], resumeFiles: [], applications: [],
});

function genderLabel(g) {
  const map = { M: '男', F: '女', O: '其他' };
  return map[g] || '-';
}

function formatFileSize(bytes) {
  if (!bytes) return '-';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1048576).toFixed(1) + ' MB';
}

const statusTagMap = {
  PENDING_SCREEN: 'warning', SCREEN_PASSED: 'success', ELIMINATED: 'danger',
  INTERVIEW_PASSED: 'success', ACCEPTED: 'success', REJECTED: 'danger', ONBOARDED: 'info',
};
function statusTagType(s) { return statusTagMap[s] || ''; }

/** 跳转简历详情 */
function goResumeDetail(row) {
  router.push({ name: 'resume-detail', params: { id: row.applicationId } });
}

async function fetchDetail() {
  loading.value = true;
  try {
    const res = await request.get(`/students/${route.params.id}`);
    const d = res.data;
    Object.keys(detail).forEach(key => {
      if (d[key] !== undefined) detail[key] = d[key];
    });
  } catch {
    ElMessage.error('获取学生详情失败');
    router.back();
  } finally {
    loading.value = false;
  }
}

async function handleToggleStatus() {
  const newStatus = detail.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const action = newStatus === 'ACTIVE' ? '启用' : '禁用';
  try {
    await ElMessageBox.confirm(`确认${action}该学生账号吗？`, '确认操作', {
      confirmButtonText: `确认${action}`, type: 'warning',
    });
    await request.put(`/students/${route.params.id}/status`, null, { params: { status: newStatus } });
    detail.status = newStatus;
    ElMessage.success(`${action}成功`);
  } catch { /* ignore */ }
}

onMounted(() => { fetchDetail(); });
</script>

<style scoped>
.student-detail-page { max-width: 1200px; margin: 0 auto; }
.detail-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px; padding: 12px 16px; background: #fff; border-radius: 4px;
}
.toolbar-title { font-size: 16px; font-weight: 600; color: #303133; }
.info-card { margin-bottom: 16px; }
.card-title { font-weight: 600; font-size: 15px; color: #303133; }
.file-list { display: flex; flex-direction: column; gap: 8px; }
.file-item {
  display: flex; justify-content: space-between; padding: 8px 12px;
  background: #f5f7fa; border-radius: 6px; font-size: 13px;
}
.file-name { color: #303133; font-weight: 500; }
.file-meta { color: #909399; font-size: 12px; }
.job-link { color: #409eff; cursor: pointer; }
.job-link:hover { text-decoration: underline; }
</style>
