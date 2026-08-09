<template>
  <div class="job-form-page">
    <el-card shadow="never">
      <template #header>
        <div class="form-header">
          <span class="form-header-title">{{ isEdit ? '编辑岗位' : '新增岗位' }}</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        style="max-width: 720px;"
      >
        <el-form-item label="岗位名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入岗位名称" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="所属部门" prop="deptId">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTree"
            node-key="deptId"
            :props="{ label: 'deptName', value: 'deptId', children: 'children' }"
            placeholder="请选择部门"
            check-strictly
            filterable
            style="width: 100%;"
          />
        </el-form-item>

        <el-form-item label="岗位类别" prop="categoryId">
          <el-cascader
            v-model="form.categoryId"
            :options="categoryOptions"
            :props="{ label: 'categoryName', value: 'categoryId', children: 'children', checkStrictly: true, emitPath: false }"
            placeholder="请选择岗位类别"
            clearable
            style="width: 100%;"
          />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工作地点" prop="location">
              <el-select v-model="form.location" placeholder="请选择" style="width: 100%;">
                <el-option label="北京" value="北京" />
                <el-option label="上海" value="上海" />
                <el-option label="深圳" value="深圳" />
                <el-option label="广州" value="广州" />
                <el-option label="杭州" value="杭州" />
                <el-option label="成都" value="成都" />
                <el-option label="西安" value="西安" />
                <el-option label="武汉" value="武汉" />
                <el-option label="南京" value="南京" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学历要求" prop="degreeRequirement">
              <el-select v-model="form.degreeRequirement" placeholder="请选择" style="width: 100%;">
                <el-option label="本科" value="本科" />
                <el-option label="硕士" value="硕士" />
                <el-option label="博士" value="博士" />
                <el-option label="不限" value="不限" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="截止日期" prop="deadline">
          <el-date-picker
            v-model="form.deadline"
            type="date"
            placeholder="请选择截止日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          />
        </el-form-item>

        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔，如：Java, 应届生, 急招" maxlength="200" />
        </el-form-item>

        <el-form-item label="岗位描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="6"
            placeholder="请输入岗位描述"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="任职要求" prop="requirement">
          <el-input
            v-model="form.requirement"
            type="textarea"
            :rows="6"
            placeholder="请输入任职要求"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '立即创建' }}
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
          <el-button v-if="isEdit" type="success" plain :loading="submitting" @click="handleSaveDraft">
            保存草稿
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';

const route = useRoute();
const router = useRouter();
const formRef = ref(null);
const submitting = ref(false);
const deptTree = ref([]);
const categoryOptions = ref([]);

const isEdit = computed(() => !!route.params.id);

const form = reactive({
  title: '',
  deptId: null,
  categoryId: null,
  location: '',
  degreeRequirement: '',
  deadline: '',
  tags: '',
  description: '',
  requirement: '',
});

const rules = {
  title: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择岗位类别', trigger: 'change' }],
  location: [{ required: true, message: '请选择工作地点', trigger: 'change' }],
  degreeRequirement: [{ required: true, message: '请选择学历要求', trigger: 'change' }],
  deadline: [{ required: true, message: '请选择截止日期', trigger: 'change' }],
  description: [{ required: true, message: '请输入岗位描述', trigger: 'blur' }],
  requirement: [{ required: true, message: '请输入任职要求', trigger: 'blur' }],
};

async function fetchDeptTree() {
  try {
    const res = await request.get('/depts/tree');
    deptTree.value = res.data || [];
  } catch { /* ignore */ }
}

async function fetchCategoryOptions() {
  try {
    const res = await request.get('/job-categories/tree');
    categoryOptions.value = res.data || [];
  } catch { /* ignore */ }
}

async function fetchDetail() {
  if (!isEdit.value) return;
  try {
    const res = await request.get(`/jobs/${route.params.id}`);
    const d = res.data;
    Object.keys(form).forEach(key => {
      if (d[key] !== undefined) form[key] = d[key];
    });
    // tags 可能是数组，转逗号分隔
    if (Array.isArray(d.tags)) {
      form.tags = d.tags.join(', ');
    }
    // categoryId 可能是数组，取最后一个
    if (Array.isArray(d.categoryId)) {
      form.categoryId = d.categoryId[d.categoryId.length - 1];
    }
  } catch {
    ElMessage.error('获取岗位详情失败');
    router.back();
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const payload = { ...form };
    // tags 转数组
    if (typeof payload.tags === 'string') {
      payload.tags = payload.tags.split(',').map(t => t.trim()).filter(Boolean);
    }
    // categoryId 可能是级联选择器路径数组，取末级值
    if (Array.isArray(payload.categoryId)) {
      payload.categoryId = payload.categoryId[payload.categoryId.length - 1];
    }
    // deadline 格式后端需要 yyyy-MM-ddTHH:mm:ss，el-date-picker 的
    // value-format="YYYY-MM-DD" 只输出日期，补上时分秒
    if (payload.deadline && /^\d{4}-\d{2}-\d{2}$/.test(payload.deadline)) {
      payload.deadline += 'T23:59:59';
    }
    // 确保数字类型（el-tree-select/el-cascader 可能返回字符串）
    if (payload.deptId != null) payload.deptId = Number(payload.deptId);
    if (payload.categoryId != null) payload.categoryId = Number(payload.categoryId);
    if (isEdit.value) {
      payload.jobId = route.params.id;
      await request.put('/jobs', payload);
      ElMessage.success('更新成功');
    } else {
      await request.post('/jobs', payload);
      ElMessage.success('创建成功');
    }
    router.push('/recruit/jobs');
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '提交失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}

async function handleSaveDraft() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const payload = { ...form, status: 'DRAFT' };
    if (typeof payload.tags === 'string') {
      payload.tags = payload.tags.split(',').map(t => t.trim()).filter(Boolean);
    }
    // categoryId 可能是级联选择器路径数组，取末级值
    if (Array.isArray(payload.categoryId)) {
      payload.categoryId = payload.categoryId[payload.categoryId.length - 1];
    }
    // deadline 格式后端需要 yyyy-MM-ddTHH:mm:ss
    if (payload.deadline && /^\d{4}-\d{2}-\d{2}$/.test(payload.deadline)) {
      payload.deadline += 'T23:59:59';
    }
    if (payload.deptId != null) payload.deptId = Number(payload.deptId);
    if (payload.categoryId != null) payload.categoryId = Number(payload.categoryId);
    if (isEdit.value) {
      payload.jobId = route.params.id;
      await request.put('/jobs', payload);
    } else {
      await request.post('/jobs', payload);
    }
    ElMessage.success('草稿已保存');
    router.push('/recruit/jobs');
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '保存失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  fetchDeptTree();
  fetchCategoryOptions();
  fetchDetail();
  applyTemplate();
});

/** 从岗位模板跳转过来时，用 query 参数预填表单 */
function applyTemplate() {
  const q = route.query;
  if (!q.templateId) return;
  if (q.title) form.title = q.title;
  if (q.deptId) form.deptId = Number(q.deptId);
  if (q.categoryId) form.categoryId = Number(q.categoryId);
  if (q.location) form.location = q.location;
  if (q.degreeRequirement) form.degreeRequirement = q.degreeRequirement;
  if (q.headcount) form.headcount = Number(q.headcount);
  if (q.description) form.description = q.description;
  if (q.requirement) form.requirement = q.requirement;
  if (q.tags) form.tags = q.tags;
  ElMessage.success('模板数据已加载，请补充截止日期后发布');
}
</script>

<style scoped>
.job-form-page {
  max-width: 800px;
  margin: 0 auto;
}

.form-header {
  display: flex;
  align-items: center;
}

.form-header-title {
  font-weight: 600;
  font-size: 16px;
}
</style>
