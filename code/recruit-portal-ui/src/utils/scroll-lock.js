/**
 * 滚动相关工具：body 滚动锁定 + 回到顶部
 *
 * body 锁定用引用计数而不是直接赋值：同时存在多层弹层时，
 * 关闭任意一层都不会把还开着的那层的锁定误释放。
 */

let lockCount = 0;

export function lockBodyScroll() {
  lockCount += 1;
  if (lockCount === 1) {
    document.body.style.overflow = 'hidden';
  }
}

export function unlockBodyScroll() {
  if (lockCount === 0) return;
  lockCount -= 1;
  if (lockCount === 0) {
    document.body.style.overflow = '';
  }
}

/** 组件卸载兜底：清掉可能残留的锁定，避免整个页面再也滚不动 */
export function resetBodyScroll() {
  lockCount = 0;
  document.body.style.overflow = '';
}

/**
 * 回到页面顶部
 *
 * Safari 15.4 之前不支持 ScrollToOptions 对象写法，
 * 且 { behavior: 'instant' } 在旧版 WebKit 会直接抛 TypeError，
 * 一旦抛出就会中断调用方后续逻辑（例如路由 watcher 里的数据刷新）。
 */
export function scrollToTop(smooth = false) {
  try {
    window.scrollTo({ top: 0, behavior: smooth ? 'smooth' : 'auto' });
  } catch {
    window.scrollTo(0, 0);
  }
}
