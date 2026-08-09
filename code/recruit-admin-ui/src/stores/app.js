import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useAppStore = defineStore('app', () => {
  const sidebarCollapse = ref(false);
  const activeMenu = ref('');

  function toggleSidebar() {
    sidebarCollapse.value = !sidebarCollapse.value;
  }

  function setActiveMenu(path) {
    activeMenu.value = path;
  }

  return { sidebarCollapse, activeMenu, toggleSidebar, setActiveMenu };
});
