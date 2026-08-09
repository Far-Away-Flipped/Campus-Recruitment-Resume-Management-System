<template>
  <el-container class="admin-layout">
    <el-aside :width="isCollapse ? '64px' : '200px'">
      <Sidebar :isCollapse="isCollapse" />
    </el-aside>
    <el-container>
      <el-header><Navbar @toggle="isCollapse = !isCollapse" :isCollapse="isCollapse" /></el-header>
      <el-main><AppMain /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue';
import Sidebar from './Sidebar.vue';
import Navbar from './Navbar.vue';
import AppMain from './AppMain.vue';
const isCollapse = ref(false);
</script>

<style scoped>
.admin-layout { height: 100vh; }
.el-header { padding: 0; height: 56px; }
.el-main { background: #f5f7fa; padding: 16px; }
</style>
