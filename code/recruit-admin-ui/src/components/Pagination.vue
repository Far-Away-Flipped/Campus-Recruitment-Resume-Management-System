<template>
  <div class="pagination-wrapper">
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  limit: { type: Number, default: 10 },
});

const emit = defineEmits(['pagination', 'update:page', 'update:limit']);

const currentPage = ref(props.page);
const pageSize = ref(props.limit);

watch(() => props.page, (val) => { currentPage.value = val; });
watch(() => props.limit, (val) => { pageSize.value = val; });

function handleSizeChange(val) {
  emit('update:limit', val);
  emit('pagination', { page: currentPage.value, limit: val });
}

function handleCurrentChange(val) {
  emit('update:page', val);
  emit('pagination', { page: val, limit: pageSize.value });
}
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}
</style>
