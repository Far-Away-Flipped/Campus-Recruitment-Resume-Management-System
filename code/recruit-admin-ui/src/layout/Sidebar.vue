<template>
  <div class="admin-sidebar">
    <div class="logo-area">
      <span v-if="!isCollapse" class="logo-text">校招管理</span>
      <span v-else class="logo-text-mini">招</span>
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
        <el-menu-item index="/recruit/brand">品牌配置</el-menu-item>
        <el-menu-item index="/recruit/categories">岗位类别</el-menu-item>
      </el-sub-menu>
      <el-sub-menu index="/system">
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
</el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup>
defineProps({ isCollapse: { type: Boolean, default: false } });
</script>

<style scoped>
.admin-sidebar { background: #304156; height: 100%; overflow-y: auto; }
.logo-area { height: 56px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold; border-bottom: 1px solid rgba(255,255,255,0.1); }
.logo-text-mini { font-size: 20px; }
.el-menu { border-right: none; }
</style>
