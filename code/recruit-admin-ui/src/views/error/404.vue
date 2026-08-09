<template>
  <div class="error-page">
    <h1>404</h1>
    <p>页面未找到</p>
    <el-button type="primary" @click="$router.push('/dashboard')">返回工作台</el-button>
  </div>
</template>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  color: #666;
}
.error-page h1 { font-size: 72px; color: #5FB8D6; margin-bottom: 16px; }
.error-page p { font-size: 18px; margin-bottom: 24px; }
</style>
