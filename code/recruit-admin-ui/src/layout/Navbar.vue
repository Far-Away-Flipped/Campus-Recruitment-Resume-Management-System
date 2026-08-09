<template>
  <div class="admin-navbar">
    <div class="navbar-left">
      <el-icon class="collapse-btn" @click="$emit('toggle')">
        <Fold v-if="!isCollapse" />
        <Expand v-else />
      </el-icon>
    </div>
    <div class="navbar-right">
      <el-dropdown trigger="click">
        <span class="user-info">
          <el-icon><UserFilled /></el-icon>
          <span class="username">管理员</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

defineProps({ isCollapse: { type: Boolean, default: false } });
defineEmits(['toggle']);

const router = useRouter();
const authStore = useAuthStore();

function handleLogout() {
  authStore.logout();
  router.push('/login');
}
</script>

<style scoped>
.admin-navbar { height: 56px; display: flex; align-items: center; justify-content: space-between; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,0.08); padding: 0 16px; }
.collapse-btn { font-size: 20px; cursor: pointer; }
.navbar-right { display: flex; align-items: center; }
.user-info { display: flex; align-items: center; gap: 6px; cursor: pointer; color: #333; }
.username { font-size: 14px; }
</style>
