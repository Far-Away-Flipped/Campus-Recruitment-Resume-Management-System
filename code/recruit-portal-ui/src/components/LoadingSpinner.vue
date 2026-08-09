<template>
  <div class="spinner-overlay" v-if="visible">
    <div class="spinner"></div>
    <p class="spinner-text" v-if="text">{{ text }}</p>
  </div>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  text: { type: String, default: '' },
});
</script>

<style scoped>
.spinner-overlay {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}
.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.spinner-text {
  margin-top: 12px;
  font-size: 14px;
  color: var(--color-text-secondary);
}
</style>
