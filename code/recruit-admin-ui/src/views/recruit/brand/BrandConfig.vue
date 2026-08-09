<template>
  <div class="brand-page">
    <el-row :gutter="16">
      <!-- 配置表单 -->
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="form-card">
          <template #header><span class="card-header-title">品牌信息配置</span></template>
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="100px"
            size="default"
            v-loading="pageLoading"
          >
            <el-form-item label="公司全称" prop="company_name">
              <el-input v-model="form.company_name" placeholder="如：XX科技有限公司" maxlength="50" />
            </el-form-item>
            <el-form-item label="公司简称" prop="company_short_name">
              <el-input v-model="form.company_short_name" placeholder="如：XX科技" maxlength="20" />
            </el-form-item>
            <el-form-item label="品牌色" prop="brand_color">
              <div class="color-pick-row">
                <el-color-picker v-model="form.brand_color" show-alpha />
                <el-input v-model="form.brand_color" placeholder="#5FB8D6" style="width: 130px; margin-left: 10px;" maxlength="9" />
              </div>
            </el-form-item>
            <el-form-item label="亮色" prop="brand_bright_color">
              <div class="color-pick-row">
                <el-color-picker v-model="form.brand_bright_color" show-alpha />
                <el-input v-model="form.brand_bright_color" placeholder="#6BB3FF" style="width: 130px; margin-left: 10px;" maxlength="9" />
              </div>
            </el-form-item>
            <el-form-item label="背景色" prop="brand_bg_color">
              <div class="color-pick-row">
                <el-color-picker v-model="form.brand_bg_color" show-alpha />
                <el-input v-model="form.brand_bg_color" placeholder="#0A0E17" style="width: 130px; margin-left: 10px;" maxlength="9" />
              </div>
            </el-form-item>
            <el-form-item label="Logo URL" prop="logo_url">
              <el-input v-model="form.logo_url" placeholder="请输入 Logo 图片 URL" maxlength="500" />
              <span class="upload-tip">建议尺寸 200x60px，请填写可公开访问的图片 URL</span>
            </el-form-item>
            <el-form-item label="页脚文案" prop="footer_text">
              <el-input
                v-model="form.footer_text"
                type="textarea"
                :rows="2"
                placeholder="如：Copyright 2024 XX科技有限公司 版权所有"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="联系邮箱" prop="contact_email">
              <el-input v-model="form.contact_email" placeholder="hr@example.com" maxlength="100" />
            </el-form-item>
            <el-form-item label="LibreOffice路径" prop="libreoffice_path">
              <el-input v-model="form.libreoffice_path" placeholder="例如: D:/Program Files/LibreOffice/program/soffice.exe" maxlength="500" />
              <span class="upload-tip">Word转PDF依赖此路径。若为空则自动探测 Program Files 目录</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saveLoading" @click="handleSave">
                保存配置
              </el-button>
              <el-button @click="loadConfig">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 预览区 -->
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="preview-card">
          <template #header><span class="card-header-title">实时预览</span></template>
          <div class="preview-section">
            <h4 class="preview-subtitle">导航栏</h4>
            <div class="preview-navbar" :style="{ backgroundColor: form.brand_bg_color || '#0A0E17', borderBottomColor: form.brand_color || '#5FB8D6' }">
              <div class="preview-logo-area">
                <img v-if="form.logo_url" :src="form.logo_url" class="preview-logo-img" />
                <span v-else class="preview-logo-text" :style="{ color: form.brand_color || '#5FB8D6' }">
                  {{ form.company_short_name || '公司简称' }}
                </span>
              </div>
            </div>
            <h4 class="preview-subtitle">品牌色</h4>
            <div class="preview-colors">
              <div class="preview-color-swatch" :style="{ backgroundColor: form.brand_color || '#5FB8D6' }">
                <span>品牌色</span>
              </div>
              <div class="preview-color-swatch" :style="{ backgroundColor: form.brand_bright_color || '#6BB3FF' }">
                <span>亮色</span>
              </div>
              <div class="preview-color-swatch" :style="{ backgroundColor: form.brand_bg_color || '#0A0E17' }">
                <span>背景色</span>
              </div>
            </div>
            <h4 class="preview-subtitle">按钮效果</h4>
            <div class="preview-buttons">
              <el-button type="primary" :style="{ '--el-color-primary': form.brand_color || '#5FB8D6' }">主按钮</el-button>
              <el-button :style="{ borderColor: form.brand_bright_color || '#6BB3FF', color: form.brand_bright_color || '#6BB3FF' }">次要按钮</el-button>
            </div>
            <h4 class="preview-subtitle">页脚</h4>
            <div class="preview-footer" :style="{ backgroundColor: form.brand_bg_color || '#0A0E17', color: '#909399' }">
              {{ form.footer_text || 'Copyright 2024 版权所有' }}
              <span v-if="form.contact_email" style="margin-left: 12px;">{{ form.contact_email }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';

const pageLoading = ref(false);
const saveLoading = ref(false);
const formRef = ref(null);

const form = reactive({
  company_name: '',
  company_short_name: '',
  brand_color: '#5FB8D6',
  brand_bright_color: '#6BB3FF',
  brand_bg_color: '#0A0E17',
  logo_url: '',
  footer_text: '',
  contact_email: '',
  libreoffice_path: '',
});

const rules = {
  company_name: [{ required: true, message: '请输入公司全称', trigger: 'blur' }],
  company_short_name: [{ required: true, message: '请输入公司简称', trigger: 'blur' }],
  contact_email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
};

async function loadConfig() {
  pageLoading.value = true;
  try {
    const res = await request.get('/brand/config');
    if (res.code === 200 && res.data) {
      const data = Array.isArray(res.data) ? res.data : [res.data];
      data.forEach(item => {
        if (item.configKey !== undefined && form.hasOwnProperty(item.configKey)) {
          form[item.configKey] = item.configValue || '';
        }
      });
    }
  } catch {
    // 错误已在拦截器处理
  } finally {
    pageLoading.value = false;
  }
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  saveLoading.value = true;
  try {
    const keys = Object.keys(form);
    for (const key of keys) {
      await request.post('/brand/config', {
        configKey: key,
        configValue: form[key],
      });
    }
    ElMessage.success('品牌配置已保存');
    loadConfig();
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.msg || e.message || ''));
  } finally {
    saveLoading.value = false;
  }
}

onMounted(() => {
  loadConfig();
});
</script>

<style scoped>
.brand-page {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.form-card {
  margin-bottom: 16px;
}

/* Logo URL 输入提示 */
.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  display: block;
}

/* 颜色选择器行 */
.color-pick-row {
  display: flex;
  align-items: center;
}

/* 预览区 */
.preview-card {
  margin-bottom: 16px;
  position: sticky;
  top: 16px;
}

.preview-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-subtitle {
  font-size: 13px;
  font-weight: 600;
  color: #909399;
  margin-bottom: 4px;
}

.preview-navbar {
  height: 48px;
  border-bottom: 2px solid;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-radius: 4px 4px 0 0;
}

.preview-logo-area {
  display: flex;
  align-items: center;
}

.preview-logo-img {
  max-height: 36px;
  object-fit: contain;
}

.preview-logo-text {
  font-size: 18px;
  font-weight: 700;
}

.preview-colors {
  display: flex;
  gap: 12px;
}

.preview-color-swatch {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 8px;
}

.preview-color-swatch span {
  font-size: 11px;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
  background: rgba(0, 0, 0, 0.25);
  padding: 2px 6px;
  border-radius: 4px;
}

.preview-buttons {
  display: flex;
  gap: 12px;
}

.preview-footer {
  padding: 12px 16px;
  font-size: 12px;
  text-align: center;
  border-radius: 4px;
}
</style>
