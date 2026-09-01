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
              <el-select v-model="form.location" multiple placeholder="请选择（可多选）" style="width: 100%;">
                <el-option
                  v-for="loc in locationOptions"
                  :key="loc.value"
                  :label="loc.label"
                  :value="loc.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学历要求" prop="degreeRequirement">
              <el-select v-model="form.degreeRequirement" placeholder="请选择" style="width: 100%;">
                <el-option
                  v-for="deg in degreeOptions"
                  :key="deg.value"
                  :label="deg.label"
                  :value="deg.value"
                />
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
          <el-input-tag v-model="form.tags" placeholder="输入标签后回车添加，如：急聘、Java" max="10" />
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
import { parseLoc, parseTags } from '@/utils/location';

const route = useRoute();
const router = useRouter();
const formRef = ref(null);
const submitting = ref(false);
const deptTree = ref([]);
const categoryOptions = ref([]);
const locationOptions = ref([]);
const degreeOptions = ref([]);

const isEdit = computed(() => !!route.params.id);

const form = reactive({
  title: '',
  deptId: null,
  categoryId: null,
  location: [],
  degreeRequirement: '',
  deadline: '',
  tags: [],
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

async function fetchLocationOptions() {
  try {
    const res = await request.get('/dict/data/work_location');
    locationOptions.value = res.data || [];
  } catch { /* ignore */ }
}

async function fetchDegreeOptions() {
  try {
    const res = await request.get('/dict/data/education_degree');
    degreeOptions.value = res.data || [];
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
    // tags 存 JSON 数组文本（或旧坏数据），解析成数组给 el-input-tag
    if (d.tags) {
      form.tags = parseTags(d.tags);
    } else {
      form.tags = [];
    }
    // location 存 JSON 数组文本，转码值数组给多选
    if (d.location) {
      form.location = parseLoc(d.location);
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
    // tags 已是数组（el-input-tag），归一化去空；兜底兼容旧字符串
    payload.tags = parseTags(payload.tags);
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
    // tags 已是数组（el-input-tag），归一化去空；兜底兼容旧字符串
    payload.tags = parseTags(payload.tags);
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
  fetchLocationOptions();
  fetchDegreeOptions();
  fetchDetail();
  applyTemplate();
});

/** 从岗位模板跳转过来时，通过 templateId 查询模板详情并预填表单 */
async function applyTemplate() {
  const q = route.query;
  if (!q.templateId) return;
  try {
    const res = await request.get(`/job-templates/${q.templateId}`);
    if (res.code === 200 && res.data) {
      const t = res.data;
      if (t.title) form.title = t.title;
      if (t.deptId) form.deptId = Number(t.deptId);
      if (t.categoryId) form.categoryId = Number(t.categoryId);
      if (t.location) form.location = parseLoc(t.location);
      if (t.degreeRequirement) form.degreeRequirement = t.degreeRequirement;
      if (t.headcount) form.headcount = Number(t.headcount);
      if (t.description) form.description = t.description;
      if (t.requirement) form.requirement = t.requirement;
      if (t.tags) form.tags = parseTags(t.tags);
      ElMessage.success('模板数据已加载，请补充截止日期后发布');
    } else {
      ElMessage.warning('模板数据加载失败，请手动填写');
    }
  } catch {
    ElMessage.warning('模板数据加载失败，请手动填写');
  }
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
