<template>
  <div class="app-shell">
    <PortalHeader />
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <PortalFooter />
  </div>
</template>

<script setup>
import PortalHeader from './components/PortalHeader.vue';
import PortalFooter from './components/PortalFooter.vue';
</script>

<style scoped>
.main-content { padding-bottom: var(--safe-bottom); }
</style>
