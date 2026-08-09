import { defineStore } from 'pinia';

export const useBrandStore = defineStore('brand', {
  state: () => ({
    primaryColor: '#5FB8D6',
    companyName: '遨天科技',
  }),
  getters: {
    brandStyle: (state) => ({
      '--color-primary': state.primaryColor,
    }),
  },
});
