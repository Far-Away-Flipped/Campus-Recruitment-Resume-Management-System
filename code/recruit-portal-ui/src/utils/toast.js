/**
 * 轻量级全局 Toast（学生门户无 UI 组件库，用原生 DOM 实现）
 *
 * 被 axios 拦截器统一调用，业务错误时弹提示，避免各视图重复维护 errorMsg。
 * 样式见 global.css 中的 .toast 系列。
 */

const CONTAINER_ID = 'app-toast-container';

function getContainer() {
  let container = document.getElementById(CONTAINER_ID);
  if (!container) {
    container = document.createElement('div');
    container.id = CONTAINER_ID;
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  return container;
}

/**
 * 展示一条全局 Toast
 * @param {string} message 提示文案
 * @param {('error'|'success'|'info')} [type=error] 类型，决定配色
 * @param {number} [duration=3000] 显示时长（ms）
 */
export function showToast(message, type = 'error', duration = 3000) {
  if (!message) return;
  const container = getContainer();

  const el = document.createElement('div');
  el.className = `toast toast--${type}`;
  el.textContent = message;

  container.appendChild(el);

  // 触发入场动画（下一帧再添加 show 类）
  requestAnimationFrame(() => {
    el.classList.add('toast--show');
  });

  // 自动关闭：先淡出再移除节点
  setTimeout(() => {
    el.classList.remove('toast--show');
    el.classList.add('toast--hide');
    setTimeout(() => el.remove(), 300);
  }, duration);
}

/** 便捷导出：错误提示 */
export const toastError = (message, duration) => showToast(message, 'error', duration);
/** 便捷导出：成功提示 */
export const toastSuccess = (message, duration) => showToast(message, 'success', duration);
