<template>
  <div class="template-page">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon> 新增模板
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        size="small"
        style="width: 100%;"
      >
        <el-table-column prop="templateName" label="模板名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="title" label="岗位名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="location" label="工作地点" width="120" show-overflow-tooltip />
        <el-table-column prop="degreeRequirement" label="学历要求" width="100" />
        <el-table-column prop="headcount" label="招聘人数" width="90" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleUse(row)">
              使用创建岗位
            </el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" size="default">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="岗位名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入岗位名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="form.deptId" placeholder="请选择部门" clearable style="width: 100%;">
            <el-option
              v-for="dept in deptList"
              :key="dept.deptId"
              :label="dept.deptName"
              :value="dept.deptId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位类别">
          <el-select v-model="form.categoryId" placeholder="请选择类别" clearable style="width: 100%;">
            <el-option
              v-for="cat in categoryList"
              :key="cat.categoryId"
              :label="cat.categoryName"
              :value="cat.categoryId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="工作地点">
          <el-input v-model="form.location" placeholder="如：北京、上海" maxlength="50" />
        </el-form-item>
        <el-form-item label="学历要求">
          <el-select v-model="form.degreeRequirement" placeholder="请选择学历要求" clearable style="width: 100%;">
            <el-option label="大专" value="大专" />
            <el-option label="本科" value="本科" />
            <el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" />
            <el-option label="不限" value="不限" />
          </el-select>
        </el-form-item>
        <el-form-item label="招聘人数">
          <el-input-number v-model="form.headcount" :min="1" :max="999" style="width: 140px;" />
        </el-form-item>
        <el-form-item label="岗位职责">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入岗位职责描述"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="任职要求">
          <el-input
            v-model="form.requirement"
            type="textarea"
            :rows="4"
            placeholder="请输入任职要求"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="如：急招/实习/校招（多个标签用逗号分隔）" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import systemRequest from '@/utils/systemRequest';

const router = useRouter();

const loading = ref(false);
const tableData = ref([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增模板');
const submitLoading = ref(false);
const formRef = ref(null);
const isEdit = ref(false);
const editId = ref(null);

/** 部门列表（用于下拉选择） */
const deptList = ref([]);
/** 岗位类别列表（用于下拉选择） */
const categoryList = ref([]);

const form = reactive({
  templateName: '',
  title: '',
  deptId: null,
  categoryId: null,
  location: '',
  degreeRequirement: '',
  headcount: 1,
  description: '',
  requirement: '',
  tags: '',
});

const rules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
};

async function fetchList() {
  loading.value = true;
  try {
    const res = await request.get('/job-templates/list');
    tableData.value = res.data?.rows || [];
  } catch {
    // ignore
  } finally {
    loading.value = false;
  }
}

async function fetchDeptList() {
  try {
    const res = await systemRequest.get('/dept/list');
    deptList.value = res.data || [];
  } catch {
    // ignore
  }
}

async function fetchCategoryList() {
  try {
    const res = await request.get('/job-categories/list');
    categoryList.value = res.data || [];
  } catch {
    // ignore
  }
}

function resetForm() {
  form.templateName = '';
  form.title = '';
  form.deptId = null;
  form.categoryId = null;
  form.location = '';
  form.degreeRequirement = '';
  form.headcount = 1;
  form.description = '';
  form.requirement = '';
  form.tags = '';
  isEdit.value = false;
  editId.value = null;
  formRef.value?.resetFields();
}

function handleAdd() {
  resetForm();
  dialogTitle.value = '新增模板';
  dialogVisible.value = true;
}

function handleEdit(row) {
  resetForm();
  isEdit.value = true;
  editId.value = row.id;
  form.templateName = row.templateName || '';
  form.title = row.title || '';
  form.deptId = row.deptId || null;
  form.categoryId = row.categoryId || null;
  form.location = row.location || '';
  form.degreeRequirement = row.degreeRequirement || '';
  form.headcount = row.headcount ?? 1;
  form.description = row.description || '';
  form.requirement = row.requirement || '';
  form.tags = row.tags || '';
  dialogTitle.value = '编辑模板';
  dialogVisible.value = true;
}

function handleUse(row) {
  // 跳转到岗位创建页面，只传 templateId，由 JobForm 通过 API 回查模板详情
  router.push({
    path: '/recruit/jobs/create',
    query: {
      templateId: row.id,
    },
  });
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  submitLoading.value = true;
  try {
    if (isEdit.value) {
      await request.put('/job-templates', { id: editId.value, ...form });
      ElMessage.success('编辑成功');
    } else {
      await request.post('/job-templates', { ...form });
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    fetchList();
  } catch {
    // ignore
  } finally {
    submitLoading.value = false;
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除模板「${row.templateName}」吗？删除后不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    await request.delete(`/job-templates/${row.id}`);
    ElMessage.success('删除成功');
    fetchList();
  } catch {
    // 取消或错误
  }
}

onMounted(() => {
  fetchList();
  fetchDeptList();
  fetchCategoryList();
});
</script>

<style scoped>
.template-page {
  max-width: 1200px;
  margin: 0 auto;
}

.toolbar {
  margin-bottom: 12px;
}
</style>
