<template>
  <div class="app-shell">
    <SpaceBackground />
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
import SpaceBackground from './components/SpaceBackground.vue';
import PortalHeader from './components/PortalHeader.vue';
import PortalFooter from './components/PortalFooter.vue';
</script>

<style scoped>
.app-shell { position: relative; min-height: 100vh; display: flex; flex-direction: column; }
.main-content {
  flex: 1;
  position: relative;
  z-index: 1;
  padding-bottom: var(--safe-bottom);
}
</style>
