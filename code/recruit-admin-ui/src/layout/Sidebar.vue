<template>
  <div class="admin-sidebar">
    <div class="logo-area">
      <router-link to="/dashboard" class="logo-link">
        <img class="logo-img" src="/logo.png" alt="遨天科技" />
      </router-link>
      <span v-if="!isCollapse" class="logo-sub">校园招聘管理</span>
    </div>
    <el-menu
      :default-active="$route.path"
      :collapse="isCollapse"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#5FB8D6"
      router
    >
      <el-menu-item index="/dashboard">
        <el-icon><HomeFilled /></el-icon>
        <span>工作台</span>
      </el-menu-item>
      <el-sub-menu index="/recruit">
        <template #title>
          <el-icon><Briefcase /></el-icon>
          <span>招聘管理</span>
        </template>
        <el-menu-item index="/recruit/jobs">岗位管理</el-menu-item>
        <el-menu-item index="/recruit/job/templates">岗位模板</el-menu-item>
        <el-menu-item index="/recruit/students">学生管理</el-menu-item>
        <el-menu-item index="/recruit/resumes">简历管理</el-menu-item>
        <el-menu-item index="/recruit/reports">数据报表</el-menu-item>
        <el-menu-item index="/recruit/categories">岗位类别</el-menu-item>
      </el-sub-menu>
      <el-sub-menu index="/system" v-if="isSuperAdmin">
        <template #title>
          <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </template>
        <el-menu-item index="/system/users">HR账号</el-menu-item>
        <el-menu-item index="/system/roles">角色管理</el-menu-item>
        <el-menu-item index="/system/depts">部门管理</el-menu-item>
        <el-menu-item index="/system/dict">字典管理</el-menu-item>
        <el-menu-item index="/system/templates">通知模板</el-menu-item>
        <el-menu-item index="/system/audit">操作审计</el-menu-item>
        <el-menu-item index="/system/network">网络管理</el-menu-item>
        <el-menu-item index="/recruit/brand">品牌配置</el-menu-item>
</el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useAuthStore } from '@/stores/auth';

defineProps({ isCollapse: { type: Boolean, default: false } });

const authStore = useAuthStore();
const isSuperAdmin = computed(() => authStore.isSuperAdmin);
</script>

<style scoped>
.admin-sidebar { background: #304156; height: 100%; overflow-y: auto; }
.logo-area { height: auto; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 14px 12px 8px; border-bottom: 1px solid rgba(255,255,255,0.1); }
.logo-link { display: flex; align-items: center; justify-content: center; }
.logo-img { height: 30px; width: auto; }
.logo-sub { font-size: 11px; color: #8899aa; margin-top: 6px; }
.el-menu { border-right: none; }
</style>
