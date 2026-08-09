<template>
  <div class="job-card" @click="$router.push(`/jobs/${job.jobId}`)">
    <div class="job-card-header">
      <h3 class="job-name">{{ job.title }}</h3>
      <span class="job-deadline" v-if="job.deadline">
        截止 {{ formatDate(job.deadline) }}
      </span>
    </div>
    <div class="job-meta">
      <span class="meta-item" v-if="job.deptName">{{ job.deptName }}</span>
      <span class="meta-item" v-if="job.location">{{ job.location }}</span>
      <span class="meta-item" v-if="job.degreeRequirement">{{ job.degreeRequirement }}</span>
    </div>
    <div class="job-tags" v-if="job.tags && job.tags.length">
      <span class="tag" v-for="tag in job.tags" :key="tag">{{ tag }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  job: { type: Object, required: true },
});

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
</script>

<style scoped>
.job-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
}
.job-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-card);
  transform: translateY(-2px);
}
.job-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.job-name {
  font-size: 18px;
  color: var(--color-text);
  font-weight: 600;
}
.job-deadline {
  font-size: 12px;
  color: var(--color-warning);
}
.job-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}
.meta-item {
  font-size: 14px;
  color: var(--color-text-secondary);
}
.job-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.tag {
  background: rgba(95, 184, 214, 0.12);
  color: var(--color-primary);
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 12px;
}
</style>
