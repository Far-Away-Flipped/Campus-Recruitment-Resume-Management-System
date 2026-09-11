/**
 * 移动端 / iOS 媒体查询与设备判定工具
 *
 * 为什么需要它：
 * - MediaQueryList.addEventListener 在 iOS Safari ≤13 上不存在（只有已废弃的 addListener），
 *   直接调用会抛 TypeError，导致整个组件 setup 失败。
 * - iPadOS 13+ 的 UA 伪装成 macOS，只能靠"MacIntel + 多点触控"识别。
 */

export const MOBILE_QUERY = '(max-width: 767px)';

/** iOS / iPadOS 设备判定 */
export function isIOS() {
  if (typeof navigator === 'undefined') return false;
  const ua = navigator.userAgent || '';
  return /iPad|iPhone|iPod/.test(ua)
    || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
}

/**
 * 是否需要把 PDF 交给系统阅读器处理（不能用页内 iframe）
 *
 * iOS Safari 的 iframe 不会渲染 PDF（只显示空白）；
 * 且 Safari 17.2+ 对 blob URL 做了按顶层来源分区，新标签打开 blob 也不可靠。
 * 窄屏 / 触屏设备上即使能渲染，内嵌浮层体验也差，一并走系统阅读器。
 */
export function needsNativePdfHandoff() {
  if (typeof window === 'undefined') return false;
  if (isIOS()) return true;
  if (window.matchMedia(MOBILE_QUERY).matches) return true;
  // 浏览器明确声明没有内嵌 PDF 查看器（部分安卓 WebView / 微信内置浏览器）
  return navigator.pdfViewerEnabled === false;
}

/**
 * 监听 MediaQueryList 变化，返回解绑函数。
 * 兼容 iOS ≤13 的 addListener/removeListener。
 */
export function onMediaChange(mql, handler) {
  if (!mql) return () => {};
  if (typeof mql.addEventListener === 'function') {
    mql.addEventListener('change', handler);
    return () => mql.removeEventListener('change', handler);
  }
  mql.addListener(handler);
  return () => mql.removeListener(handler);
}
