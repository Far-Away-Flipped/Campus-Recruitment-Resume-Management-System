<template>
  <div class="messages-page">
    <h1 class="page-title">
      消息中心
      <span class="unread-badge" v-if="unreadCount > 0">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
    </h1>

    <LoadingSpinner :visible="loading" text="加载消息..." />

    <div class="form-toast form-toast--error" v-if="error">{{ error }}</div>

    <div class="messages-list" v-if="!loading && messages.length > 0">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-item"
        :class="{ 'message-item--unread': msg.isRead === '0' }"
        @click="openDetail(msg)"
      >
        <div class="message-item__status">
          <span class="unread-dot" v-if="msg.isRead === '0'"></span>
        </div>
        <div class="message-item__content">
          <p class="message-item__title">{{ msg.title }}</p>
          <p class="message-item__preview">{{ msg.content?.substring(0, 100) }}</p>
          <p class="message-item__time">{{ formatTime(msg.createTime) }}</p>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="totalPages > 1">
        <button :disabled="page === 1" @click="page--; loadMessages()">上一页</button>
        <span>{{ page }} / {{ totalPages }}</span>
        <button :disabled="page >= totalPages" @click="page++; loadMessages()">下一页</button>
      </div>
    </div>

    <div class="empty" v-if="!loading && messages.length === 0">
      <p>暂无消息</p>
    </div>

    <!-- 消息详情弹窗 -->
    <div class="modal-overlay" v-if="showDetail" @click.self="showDetail = false">
      <div class="modal">
        <h3>{{ detailMsg.title }}</h3>
        <p class="modal-time">{{ formatTime(detailMsg.createTime) }}</p>
        <div class="modal-body">{{ detailMsg.content }}</div>
        <div class="modal-actions">
          <button
            v-if="detailMsg.messageType === 'APPLICATION_STATUS_CHANGED' && detailMsg.refId"
            class="btn-progress"
            @click="goToApplication"
          >查看投递进度</button>
          <button class="btn-close" @click="showDetail = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import api from '../utils/axios.js';
import LoadingSpinner from '../components/LoadingSpinner.vue';

const router = useRouter();

const loading = ref(true);
const error = ref('');
const messages = ref([]);
const page = ref(1);
const pageSize = 10;
const total = ref(0);
const showDetail = ref(false);
const detailMsg = ref({});
const unreadCount = ref(0);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

async function loadMessages() {
  loading.value = true;
  try {
    const res = await api.get(`/messages?pageNum=${page.value}&pageSize=${pageSize}`);
    if (res.data) {
      messages.value = res.data.rows || [];
      total.value = res.data.total || 0;
    }
  } catch (e) {
    error.value = e.response?.data?.msg || '加载消息失败';
  } finally {
    loading.value = false;
  }
}

async function loadUnreadCount() {
  try {
    const res = await api.get('/messages/unread-count');
    unreadCount.value = res.data?.count || 0;
  } catch { /* 未读角标拉取失败不影响消息列表 */ }
}

function goToApplication() {
  if (detailMsg.value.refId) {
    router.push(`/my-applications?applicationId=${detailMsg.value.refId}`);
    showDetail.value = false;
  }
}

async function openDetail(msg) {
  detailMsg.value = msg;
  showDetail.value = true;
  if (msg.isRead === '0') {
    try {
      await api.put(`/messages/${msg.id}/read`);
      msg.isRead = '1';
      loadUnreadCount(); // 标记已读后刷新未读角标
    } catch { /* ignore */ }
  }
}

function formatTime(t) {
  if (!t) return '';
  return new Date(t).toLocaleString('zh-CN');
}

onMounted(() => {
  loadMessages();
  loadUnreadCount();
});
</script>

<style scoped>
.messages-page { max-width: 800px; margin: 0 auto; padding: 40px 24px; }
.page-title { font-size: 28px; margin-bottom: 24px; color: var(--color-text); }
.form-toast--error { padding: 10px 14px; border-radius: 6px; font-size: 13px; margin-bottom: 16px; background: rgba(224,82,82,0.12); color: var(--color-danger); }

.messages-list { display: flex; flex-direction: column; gap: 8px; }
.message-item { display: flex; gap: 12px; padding: 16px; background: var(--color-card); border: 1px solid var(--color-border); border-radius: 8px; cursor: pointer; transition: border-color 0.2s; }
.message-item:hover { border-color: var(--color-primary); }
.message-item--unread { border-left: 3px solid var(--color-primary); }
.message-item__status { flex-shrink: 0; padding-top: 4px; }
.unread-dot { display: block; width: 8px; height: 8px; border-radius: 50%; background: var(--color-primary); }
.message-item__content { flex: 1; min-width: 0; }
.message-item__title { font-size: 15px; font-weight: 600; color: var(--color-text); margin-bottom: 4px; }
.message-item__preview { font-size: 13px; color: var(--color-text-secondary); line-height: 1.5; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.message-item__time { font-size: 12px; color: var(--color-text-secondary); opacity: 0.6; margin-top: 6px; }

.empty { text-align: center; padding: 60px 0; color: var(--color-text-secondary); }
.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 20px; }
.pagination button { padding: 6px 16px; border: 1px solid var(--color-border); border-radius: 4px; background: var(--color-card); color: var(--color-text); cursor: pointer; font-family: inherit; }
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: var(--color-card); border-radius: 12px; padding: 32px; width: 90%; max-width: 560px; max-height: 70vh; overflow-y: auto; }
.modal h3 { font-size: 18px; margin-bottom: 8px; color: var(--color-text); }
.modal-time { font-size: 12px; color: var(--color-text-secondary); margin-bottom: 16px; }
.modal-body { font-size: 14px; color: var(--color-text); line-height: 1.7; white-space: pre-wrap; margin-bottom: 20px; }
.btn-close { padding: 10px 24px; background: var(--color-primary); border: none; border-radius: 6px; color: #fff; font-size: 14px; cursor: pointer; font-family: inherit; }

.unread-badge { display: inline-block; margin-left: 8px; padding: 2px 9px; min-width: 22px; text-align: center; font-size: 12px; font-weight: 600; line-height: 1.5; color: #fff; background: var(--color-danger, #e05252); border-radius: 11px; vertical-align: middle; }

.modal-actions { display: flex; gap: 12px; }
.btn-progress { padding: 10px 24px; background: none; border: 1px solid var(--color-primary); border-radius: 6px; color: var(--color-primary); font-size: 14px; cursor: pointer; font-family: inherit; }
.btn-progress:hover { background: rgba(95, 184, 214, 0.1); }

/* ===== 移动端触摸优化 ===== */
@media (max-width: 767px) {
  .messages-page {
    padding: 24px var(--container-px) 60px;
  }
  .page-title {
    font-size: 22px;
  }

  /* 消息列表项触摸优化 */
  .message-item {
    padding: 14px 16px;
    min-height: 72px;
  }
  .message-item__title {
    font-size: 15px;
  }
  .message-item__preview {
    font-size: 13px;
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

  /* 关闭按钮全宽 */
  .btn-close {
    width: 100%;
    min-height: var(--touch-min);
    text-align: center;
  }

  /* 操作按钮移动端纵向排列 + 触摸优化 */
  .modal-actions {
    flex-direction: column;
  }
  .btn-progress {
    width: 100%;
    min-height: var(--touch-min);
    text-align: center;
  }

  /* 分页按钮触摸优化 */
  .pagination button {
    min-height: var(--touch-min);
    padding: 10px 20px;
  }
}
</style>
