<template>
  <div class="profile-list-page" v-motion-fade="{ y: 24 }">
    <h1 class="page-title">{{ pageTitle }}</h1>

    <LoadingSpinner :visible="loading" :text="'加载中...'" />

    <div class="form-toast form-toast--success" v-if="success">{{ success }}</div>
    <div class="form-toast form-toast--error" v-if="error">{{ error }}</div>

    <!-- 添加按钮 -->
    <button class="btn-add" @click="openAdd" v-if="!loading">
      + 添加{{ itemLabel }}
    </button>

    <!-- 列表 -->
    <div class="list" v-if="!loading && items.length > 0">
      <div v-for="item in items" :key="item.id" class="list-item">
        <div class="list-item__info">
          <p class="list-item__title">{{ formatTitle(item) }}</p>
          <p class="list-item__sub">{{ formatSub(item) }}</p>
          <p class="list-item__desc" v-if="item.description">{{ item.description }}</p>
        </div>
        <div class="list-item__actions">
          <button class="btn-sm btn-sm--edit" @click="openEdit(item)">编辑</button>
          <button class="btn-sm btn-sm--delete" @click="handleDelete(item.id)">删除</button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty" v-if="!loading && items.length === 0">
      <p>暂无{{ itemLabel }}记录</p>
      <p class="empty-hint">点击上方按钮添加</p>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div class="modal-overlay" v-if="showModal" @click.self="closeModal">
      <div class="modal">
        <h3 class="modal-title">{{ isEdit ? '编辑' : '新增' }}{{ itemLabel }}</h3>
        <form @submit.prevent="handleSubmit">
          <template v-if="type === 'internship'">
            <div class="form-group">
              <label>类型</label>
              <select v-model="form.recordType">
                <option value="I">实习经历</option>
                <option value="P">项目经历</option>
              </select>
            </div>
            <div class="form-group">
              <label>公司/项目名称 *</label>
              <input v-model="form.orgName" type="text" required placeholder="请输入公司或项目名称" />
            </div>
            <div class="form-group">
              <label>岗位/角色</label>
              <input v-model="form.position" type="text" placeholder="例如：软件开发实习生" />
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>开始时间</label>
                <input v-model="form.startDate" type="date" />
              </div>
              <div class="form-group">
                <label>结束时间</label>
                <input v-model="form.endDate" type="date" />
              </div>
            </div>
            <div class="form-group">
              <label>工作描述</label>
              <textarea v-model="form.description" rows="3" placeholder="请描述您的工作内容和成果"></textarea>
            </div>
          </template>

          <template v-if="type === 'certificate'">
            <div class="form-group">
              <label>类型</label>
              <select v-model="form.certType">
                <option value="CERT">证书</option>
                <option value="SKILL">技能</option>
                <option value="LANGUAGE">语言能力</option>
              </select>
            </div>
            <div class="form-group">
              <label>名称 *</label>
              <input v-model="form.certName" type="text" required placeholder="例如：CET-6、Python、普通话" />
            </div>
            <div class="form-group">
              <label>等级/分数</label>
              <input v-model="form.certLevel" type="text" placeholder="例如：580分、熟练" />
            </div>
            <div class="form-group">
              <label>补充说明</label>
              <input v-model="form.description" type="text" placeholder="补充说明（选填）" />
            </div>
          </template>

          <template v-if="type === 'activity'">
            <div class="form-group">
              <label>社团/组织名称 *</label>
              <input v-model="form.orgName" type="text" required placeholder="例如：学生会、志愿者协会" />
            </div>
            <div class="form-group">
              <label>担任职务</label>
              <input v-model="form.position" type="text" placeholder="例如：外联部部长" />
            </div>
            <div class="form-group">
              <label>主要职责及业绩</label>
              <textarea v-model="form.description" rows="3" placeholder="请描述您在社团中的主要工作和成绩"></textarea>
            </div>
          </template>

          <div class="modal-actions">
            <button type="button" class="btn-cancel" @click="closeModal">取消</button>
            <button type="submit" class="btn-save" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import api from '../utils/axios.js';
import LoadingSpinner from '../components/LoadingSpinner.vue';

const props = defineProps({
  type: { type: String, required: true } // 'internship' | 'certificate' | 'activity'
});

const route = useRoute();
const loading = ref(true);
const saving = ref(false);
const error = ref('');
const success = ref('');
const showModal = ref(false);
const isEdit = ref(false);
const editId = ref(null);

const items = ref([]);

const form = reactive({
  // internship
  recordType: 'I',
  orgName: '',
  position: '',
  startDate: '',
  endDate: '',
  // certificate
  certType: 'CERT',
  certName: '',
  certLevel: '',
  // activity
  // orgName, position reused
  description: ''
});

const API_PATH = computed(() => `/profile/${props.type}`);
const pageTitle = computed(() => {
  const map = { internship: '实习/项目经历', certificate: '技能/证书', activity: '社团经历' };
  return map[props.type] || '经历管理';
});
const itemLabel = computed(() => {
  const map = { internship: '经历', certificate: '证书/技能', activity: '社团经历' };
  return map[props.type] || '记录';
});

function formatTitle(item) {
  if (props.type === 'internship') return item.orgName || '未命名';
  if (props.type === 'certificate') return item.certName || '未命名';
  if (props.type === 'activity') return item.orgName || '未命名';
  return '—';
}

function formatSub(item) {
  if (props.type === 'internship') {
    const type = item.recordType === 'P' ? '项目经历' : '实习经历';
    return `${type}${item.position ? ' · ' + item.position : ''}${item.startDate ? ' · ' + item.startDate : ''}`;
  }
  if (props.type === 'certificate') {
    const typeMap = { CERT: '证书', SKILL: '技能', LANGUAGE: '语言能力' };
    const type = typeMap[item.certType] || item.certType;
    return `${type}${item.certLevel ? ' · ' + item.certLevel : ''}`;
  }
  if (props.type === 'activity') {
    return item.position || '';
  }
  return '';
}

async function loadItems() {
  loading.value = true;
  try {
    const res = await api.get(API_PATH.value);
    items.value = Array.isArray(res.data) ? res.data : (res.data?.rows || []);
  } catch (e) {
    error.value = '加载失败';
  } finally {
    loading.value = false;
  }
}

function openAdd() {
  resetForm();
  isEdit.value = false;
  editId.value = null;
  showModal.value = true;
}

function openEdit(item) {
  isEdit.value = true;
  editId.value = item.id;
  Object.keys(form).forEach(k => { form[k] = item[k] || ''; });
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
  resetForm();
}

function resetForm() {
  form.recordType = 'I';
  form.orgName = '';
  form.position = '';
  form.startDate = '';
  form.endDate = '';
  form.certType = 'CERT';
  form.certName = '';
  form.certLevel = '';
  form.description = '';
}

async function handleSubmit() {
  saving.value = true;
  error.value = '';
  try {
    const payload = {};
    if (props.type === 'internship') {
      Object.assign(payload, {
        recordType: form.recordType,
        orgName: form.orgName,
        position: form.position,
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        description: form.description
      });
    } else if (props.type === 'certificate') {
      Object.assign(payload, {
        certType: form.certType,
        certName: form.certName,
        certLevel: form.certLevel,
        description: form.description
      });
    } else if (props.type === 'activity') {
      Object.assign(payload, {
        orgName: form.orgName,
        position: form.position,
        description: form.description
      });
    }

    if (isEdit.value) {
      await api.put(`${API_PATH.value}/${editId.value}`, payload);
    } else {
      await api.post(API_PATH.value, payload);
    }

    success.value = isEdit.value ? '修改成功' : '添加成功';
    setTimeout(() => success.value = '', 2000);
    closeModal();
    await loadItems();
  } catch (e) {
    error.value = e.response?.data?.msg || '操作失败';
    setTimeout(() => error.value = '', 3000);
  } finally {
    saving.value = false;
  }
}

async function handleDelete(id) {
  if (!confirm('确认删除？')) return;
  try {
    await api.delete(`${API_PATH.value}/${id}`);
    success.value = '删除成功';
    setTimeout(() => success.value = '', 2000);
    await loadItems();
  } catch (e) {
    error.value = '删除失败';
    setTimeout(() => error.value = '', 3000);
  }
}

onMounted(loadItems);
</script>

<style scoped>
.profile-list-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 24px;
}
.page-title { font-size: 28px; margin-bottom: 24px; color: var(--color-text); }
.btn-add {
  padding: 10px 24px;
  background: linear-gradient(135deg, #5FB8D6, #6BB3FF);
  border: none; border-radius: 6px;
  color: #0A0E17; font-size: 14px; font-weight: 600; cursor: pointer;
  margin-bottom: 20px; transition: opacity 0.2s;
  font-family: inherit;
}
.btn-add:hover { opacity: 0.9; }

.list { display: flex; flex-direction: column; gap: 12px; }
.list-item {
  display: flex; align-items: flex-start; justify-content: space-between;
  background: var(--bg-glass); border: 1px solid var(--color-border);
  backdrop-filter: blur(var(--glass-blur));
  border-radius: 8px; padding: 16px 20px; gap: 16px;
}
.list-item__info { flex: 1; min-width: 0; }
.list-item__title { font-size: 15px; color: var(--color-text); font-weight: 600; margin-bottom: 4px; }
.list-item__sub { font-size: 13px; color: var(--color-text-secondary); }
.list-item__desc { font-size: 13px; color: var(--color-text-secondary); margin-top: 6px; line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.list-item__actions { display: flex; gap: 8px; flex-shrink: 0; }

.btn-sm { padding: 6px 14px; border-radius: 4px; font-size: 13px; cursor: pointer; border: 1px solid transparent; font-family: inherit; }
.btn-sm--edit { border-color: var(--color-primary); color: var(--color-primary); background: transparent; }
.btn-sm--edit:hover { background: rgba(95,184,214,0.1); }
.btn-sm--delete { border-color: var(--color-danger); color: var(--color-danger); background: transparent; }
.btn-sm--delete:hover { background: rgba(224,82,82,0.1); }

.empty { text-align: center; padding: 60px 0; color: var(--color-text-secondary); }
.empty-hint { font-size: 13px; margin-top: 8px; opacity: 0.6; }

.form-toast { padding: 10px 14px; border-radius: 6px; font-size: 13px; margin-bottom: 16px; }
.form-toast--error { background: rgba(224,82,82,0.12); color: var(--color-danger); border: 1px solid rgba(224,82,82,0.25); }
.form-toast--success { background: rgba(95,184,141,0.12); color: var(--color-success); border: 1px solid rgba(95,184,141,0.25); }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: var(--bg-glass-strong); backdrop-filter: blur(var(--glass-blur-heavy)); border: 1px solid var(--color-border); border-radius: 12px; padding: 28px; width: 90%; max-width: 520px; max-height: 80vh; overflow-y: auto; }
.modal-title { font-size: 18px; margin-bottom: 20px; color: var(--color-text); }
.form-group { margin-bottom: 14px; }
.form-group label { display: block; font-size: 13px; color: var(--color-text-secondary); margin-bottom: 4px; }
.form-group input, .form-group select, .form-group textarea {
  width: 100%; padding: 10px 12px; background: var(--color-bg); border: 1px solid var(--color-border);
  border-radius: 6px; color: var(--color-text); font-size: 14px; outline: none; font-family: inherit;
}
.form-group input:focus, .form-group select:focus, .form-group textarea:focus { border-color: var(--color-primary); }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px; }
.btn-cancel { padding: 10px 20px; border: 1px solid var(--color-border); border-radius: 6px; background: transparent; color: var(--color-text-secondary); cursor: pointer; font-size: 14px; font-family: inherit; }
.btn-save { padding: 10px 24px; background: var(--color-primary); border: none; border-radius: 6px; color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; font-family: inherit; }
.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }

@media (max-width: 640px) { .form-row { grid-template-columns: 1fr; } }

/* ===== 移动端触摸优化 ===== */
@media (max-width: 767px) {
  .profile-list-page {
    padding: 24px var(--container-px) 60px;
  }
  .page-title {
    font-size: 22px;
  }

  /* 添加按钮全宽 */
  .btn-add {
    width: 100%;
    text-align: center;
    min-height: var(--touch-min);
  }

  /* 列表项垂直堆叠 */
  .list-item {
    flex-direction: column;
    align-items: flex-start;
    padding: 14px 16px;
    gap: 10px;
  }
  .list-item__actions {
    width: 100%;
    justify-content: flex-end;
  }
  .btn-sm {
    min-height: var(--touch-min);
    min-width: var(--touch-min);
    padding: 8px 16px;
  }

  /* 弹窗底部滑入 */
  .modal-overlay {
    align-items: flex-end;
    padding: 0;
  }
  .modal {
    border-radius: 16px 16px 0 0;
    width: 100%;
    max-width: 100%;
    max-height: 85vh;
    padding: 24px 20px;
  }

  /* 弹窗表单 */
  .form-group input,
  .form-group select,
  .form-group textarea {
    min-height: var(--input-min-h);
    font-size: 16px;
  }
  .form-row {
    grid-template-columns: 1fr;
  }

  /* 弹窗按钮区 */
  .modal-actions {
    flex-direction: column-reverse;
    gap: 8px;
  }
  .btn-cancel,
  .btn-save {
    width: 100%;
    min-height: var(--touch-min);
    text-align: center;
  }
}
</style>
