import { ref } from 'vue';
import { defineStore } from 'pinia';
import request from '@/utils/request';

/**
 * 管理后台字典 store —— 以后端字典为唯一真源，按需加载 + 进程内缓存
 * <p>供 formatLoc/formatDegree 反查码值中文名（work_location / education_degree），
 * 与门户端 stores/dict.js 对称，避免前端硬编码映射与后台字典脱钩导致显示字典码
 * （历史 bug 根治）。路径：/api/admin/dict/data/{dictType}（HR 登录态可读）。</p>
 */
export const useDictStore = defineStore('dict', () => {
  /** dictType -> [{label, value}]；加载成功/失败都会填充（空数组标记已尝试，避免反复请求） */
  const optionsMap = ref({});
  /** dictType -> Promise，用于并发去重 */
  const loadingMap = ref({});

  /**
   * 确保某字典已加载。幂等：已加载直接返回缓存；加载中返回同一 Promise；否则发起请求。
   * @returns {Promise<Array>} 字典项 [{label,value}]
   */
  function ensureLoaded(dictType) {
    if (optionsMap.value[dictType]) {
      return Promise.resolve(optionsMap.value[dictType]);
    }
    if (loadingMap.value[dictType]) {
      return loadingMap.value[dictType];
    }
    const p = request
      .get(`/dict/data/${dictType}`)
      .then(res => {
        const list = (res && res.code === 200 && Array.isArray(res.data)) ? res.data : [];
        optionsMap.value[dictType] = list;
        return list;
      })
      .catch(() => {
        optionsMap.value[dictType] = []; // 标记已尝试，避免每次渲染重试刷屏；调用方回退硬编码
        return [];
      })
      .finally(() => {
        delete loadingMap.value[dictType];
      });
    loadingMap.value[dictType] = p;
    return p;
  }

  /**
   * 反查码值中文 label；字典未加载或未命中返回 ''
   */
  function labelOf(dictType, value) {
    const list = optionsMap.value[dictType];
    if (!list) return '';
    const hit = list.find(o => o.value === value);
    return hit ? hit.label : '';
  }

  /** 字典是否已加载（成功或失败尝试过） */
  function hasLoaded(dictType) {
    return !!optionsMap.value[dictType];
  }

  return { optionsMap, loadingMap, ensureLoaded, labelOf, hasLoaded };
});
