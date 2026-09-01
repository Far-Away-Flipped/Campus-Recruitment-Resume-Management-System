<template>
  <div class="dict-page">
    <el-row :gutter="16">
      <!-- 左侧：字典类型 -->
      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="dict-type-card">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">字典类型</span>
              <el-button type="primary" size="small" @click="handleAddType">
                <el-icon><Plus /></el-icon> 新增
              </el-button>
            </div>
          </template>
          <div v-loading="typeLoading" class="type-list">
            <div
              v-for="item in typeList"
              :key="item.dictId"
              class="type-item"
              :class="{ active: selectedTypeId === item.dictId }"
              @click="selectType(item)"
            >
              <div class="type-item-info">
                <span class="type-item-name">{{ item.dictName }}</span>
                <span class="type-item-type">{{ item.dictType }}</span>
              </div>
              <div class="type-item-actions" @click.stop>
                <el-button type="primary" link size="small" @click="handleEditType(item)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button type="danger" link size="small" @click="handleDeleteType(item)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
            <el-empty v-if="!typeLoading && typeList.length === 0" description="暂无字典类型" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：字典数据 -->
      <el-col :xs="24" :md="16">
        <el-card shadow="never" class="dict-data-card">
          <template #header>
            <div class="card-header-row">
              <span class="card-header-title">
                字典数据
                <span v-if="selectedType" class="selected-type-hint">
                  - {{ selectedType.dictName }}
                </span>
              </span>
              <el-button
                v-if="selectedTypeId"
                type="primary"
                size="small"
                @click="handleAddData"
              >
                <el-icon><Plus /></el-icon> 新增
              </el-button>
            </div>
          </template>

          <div v-if="!selectedTypeId" class="no-selection">
            <el-empty description="请选择左侧字典类型" :image-size="80" />
          </div>

          <div v-else>
            <el-table
              v-loading="dataLoading"
              :data="dataList"
              stripe
              size="small"
              style="width: 100%;"
            >
              <el-table-column prop="dictLabel" label="字典标签" min-width="120" />
              <el-table-column prop="dictValue" label="字典键值" min-width="100" />
              <el-table-column prop="dictSort" label="排序" width="80" align="center" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
                    {{ row.status === '0' ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="handleEditData(row)">编辑</el-button>
                  <el-button type="danger" link size="small" @click="handleDeleteData(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrap">
              <el-pagination
                v-model:current-page="dataQuery.pageNum"
                v-model:page-size="dataQuery.pageSize"
                :page-sizes="[10, 20, 50]"
                :total="dataTotal"
                layout="total, sizes, prev, pager, next, jumper"
                background
                @size-change="fetchDataList"
                @current-change="fetchDataList"
              />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 字典类型弹窗 -->
    <el-dialog
      v-model="typeDialogVisible"
      :title="typeDialogTitle"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="100px" size="default">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="如：性别" maxlength="30" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="typeForm.dictType" placeholder="如：sys_user_sex" :disabled="isTypeEdit" maxlength="50" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="typeForm.status" :active-value="'0'" :inactive-value="'1'" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" placeholder="可选" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeSubmitLoading" @click="handleTypeSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据弹窗 -->
    <el-dialog
      v-model="dataDialogVisible"
      :title="dataDialogTitle"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="100px" size="default">
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="如：男" maxlength="30" />
        </el-form-item>
        <el-form-item label="字典键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="如：0" maxlength="30" />
        </el-form-item>
        <el-form-item label="排序" prop="dictSort">
          <el-input-number v-model="dataForm.dictSort" :min="0" :max="999" style="width: 140px;" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="dataForm.status" :active-value="'0'" :inactive-value="'1'" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="dataForm.remark" type="textarea" :rows="2" placeholder="可选" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dataSubmitLoading" @click="handleDataSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import systemRequest from '@/utils/systemRequest';

// ---------- 字典类型 ----------
const typeLoading = ref(false);
const typeList = ref([]);
const selectedTypeId = ref(null);
const selectedType = ref(null);

// 类型弹窗
const typeDialogVisible = ref(false);
const typeDialogTitle = ref('新增字典类型');
const typeSubmitLoading = ref(false);
const typeFormRef = ref(null);
const isTypeEdit = ref(false);
const typeEditId = ref(null);

const typeForm = reactive({
  dictName: '',
  dictType: '',
  status: '0',
  remark: '',
});

const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }],
};

// ---------- 字典数据 ----------
const dataLoading = ref(false);
const dataList = ref([]);
const dataTotal = ref(0);
const dataQuery = reactive({ pageNum: 1, pageSize: 10 });

// 数据弹窗
const dataDialogVisible = ref(false);
const dataDialogTitle = ref('新增字典数据');
const dataSubmitLoading = ref(false);
const dataFormRef = ref(null);
const isDataEdit = ref(false);
const dataEditId = ref(null);

const dataForm = reactive({
  dictLabel: '',
  dictValue: '',
  dictSort: 0,
  status: '0',
  remark: '',
});

const dataRules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }],
};

// ========== 类型操作 ==========
async function fetchTypeList() {
  typeLoading.value = true;
  try {
    const res = await systemRequest.get('/dict/type/list');
    typeList.value = res.data || [];
    // 如果之前有选中，刷新后恢复选中状态
    if (selectedTypeId.value) {
      const found = typeList.value.find(t => t.dictId === selectedTypeId.value);
      if (found) {
        selectedType.value = found;
      } else {
        selectedTypeId.value = null;
        selectedType.value = null;
        dataList.value = [];
      }
    }
  } catch {
    // ignore
  } finally {
    typeLoading.value = false;
  }
}

function selectType(item) {
  selectedTypeId.value = item.dictId;
  selectedType.value = item;
  dataQuery.pageNum = 1;
  fetchDataList();
}

function resetTypeForm() {
  typeForm.dictName = '';
  typeForm.dictType = '';
  typeForm.status = '0';
  typeForm.remark = '';
  isTypeEdit.value = false;
  typeEditId.value = null;
  typeFormRef.value?.resetFields();
}

function handleAddType() {
  resetTypeForm();
  typeDialogTitle.value = '新增字典类型';
  typeDialogVisible.value = true;
}

function handleEditType(row) {
  resetTypeForm();
  isTypeEdit.value = true;
  typeEditId.value = row.dictId;
  typeForm.dictName = row.dictName;
  typeForm.dictType = row.dictType;
  typeForm.status = row.status ?? '0';
  typeForm.remark = row.remark || '';
  typeDialogTitle.value = '编辑字典类型';
  typeDialogVisible.value = true;
}

async function handleTypeSubmit() {
  const valid = await typeFormRef.value.validate().catch(() => false);
  if (!valid) return;

  typeSubmitLoading.value = true;
  try {
    if (isTypeEdit.value) {
      await systemRequest.put('/dict/type', { dictId: typeEditId.value, ...typeForm });
      ElMessage.success('编辑成功');
    } else {
      await systemRequest.post('/dict/type', { ...typeForm });
      ElMessage.success('新增成功');
    }
    typeDialogVisible.value = false;
    fetchTypeList();
  } catch {
    // ignore
  } finally {
    typeSubmitLoading.value = false;
  }
}

async function handleDeleteType(row) {
  try {
    await ElMessageBox.confirm(`确认删除字典类型「${row.dictName}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await systemRequest.delete(`/dict/type/${row.dictId}`);
    ElMessage.success('删除成功');
    if (selectedTypeId.value === row.dictId) {
      selectedTypeId.value = null;
      selectedType.value = null;
      dataList.value = [];
    }
    fetchTypeList();
  } catch {
    // 取消或错误
  }
}

// ========== 数据操作 ==========
async function fetchDataList() {
  if (!selectedTypeId.value) return;
  dataLoading.value = true;
  try {
    const res = await systemRequest.get('/dict/data/list', {
      params: { ...dataQuery, dictType: selectedType.value?.dictType },
    });
    dataList.value = res.data?.rows || [];
    dataTotal.value = res.data?.total || 0;
  } catch {
    // ignore
  } finally {
    dataLoading.value = false;
  }
}

function resetDataForm() {
  dataForm.dictLabel = '';
  dataForm.dictValue = '';
  dataForm.dictSort = 0;
  dataForm.status = '0';
  dataForm.remark = '';
  isDataEdit.value = false;
  dataEditId.value = null;
  dataFormRef.value?.resetFields();
}

function handleAddData() {
  resetDataForm();
  dataDialogTitle.value = '新增字典数据';
  dataDialogVisible.value = true;
}

function handleEditData(row) {
  resetDataForm();
  isDataEdit.value = true;
  dataEditId.value = row.dictCode;
  dataForm.dictLabel = row.dictLabel;
  dataForm.dictValue = row.dictValue;
  dataForm.dictSort = row.dictSort ?? 0;
  dataForm.status = row.status ?? '0';
  dataForm.remark = row.remark || '';
  dataDialogTitle.value = '编辑字典数据';
  dataDialogVisible.value = true;
}

async function handleDataSubmit() {
  const valid = await dataFormRef.value.validate().catch(() => false);
  if (!valid) return;

  dataSubmitLoading.value = true;
  try {
    const payload = { ...dataForm, dictType: selectedType.value?.dictType };
    if (isDataEdit.value) {
      await systemRequest.put('/dict/data', { dictCode: dataEditId.value, ...payload });
      ElMessage.success('编辑成功');
    } else {
      await systemRequest.post('/dict/data', payload);
      ElMessage.success('新增成功');
    }
    dataDialogVisible.value = false;
    fetchDataList();
  } catch {
    // ignore
  } finally {
    dataSubmitLoading.value = false;
  }
}

async function handleDeleteData(row) {
  try {
    await ElMessageBox.confirm(`确认删除字典数据「${row.dictLabel}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await systemRequest.delete(`/dict/data/${row.dictCode}`);
    ElMessage.success('删除成功');
    fetchDataList();
  } catch {
    // 取消或错误
  }
}

onMounted(() => {
  fetchTypeList();
});
</script>

<style scoped>
.dict-page {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.selected-type-hint {
  font-weight: 400;
  font-size: 13px;
  color: #909399;
  margin-left: 4px;
}

/* 类型列表 */
.dict-type-card {
  margin-bottom: 16px;
  min-height: 400px;
}

.type-list {
  max-height: 600px;
  overflow-y: auto;
}

.type-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 4px;
}

.type-item:hover {
  background: #f5f7fa;
}

.type-item.active {
  background: rgba(95, 184, 214, 0.1);
  border-left: 3px solid #5FB8D6;
}

.type-item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.type-item-name {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.type-item-type {
  font-size: 12px;
  color: #909399;
}

.type-item-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}

/* 数据区 */
.dict-data-card {
  margin-bottom: 16px;
  min-height: 400px;
}

.no-selection {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
