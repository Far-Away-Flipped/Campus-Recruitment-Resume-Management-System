/**
 * 日期解析工具
 *
 * 后端时间字段多为 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")，形如
 * "2026-09-03 14:20:00"。Chrome / Firefox 能容错解析这种非标准格式，
 * **Safari（含 iOS）会直接返回 Invalid Date**，页面上就显示 "NaN-NaN-NaN"。
 * 统一用 parseDateTime 替代裸 new Date(str)。
 */

/** 解析后端时间值，失败返回 null（调用方据此展示占位符） */
export function parseDateTime(value) {
  if (value == null || value === '') return null;
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value;

  // 只把日期与时间之间的第一个空格换成 'T'（ISO 8601 可被所有浏览器解析）
  const normalized = typeof value === 'string'
    ? value.trim().replace(' ', 'T')
    : value;

  const d = new Date(normalized);
  return Number.isNaN(d.getTime()) ? null : d;
}

/** 格式化为 yyyy-MM-dd HH:mm，解析失败返回 fallback */
export function formatDateTime(value, fallback = '-') {
  const d = parseDateTime(value);
  if (!d) return fallback;
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} `
    + `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** 格式化为 yyyy-MM-dd，解析失败返回 fallback */
export function formatDate(value, fallback = '--') {
  const d = parseDateTime(value);
  if (!d) return fallback;
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
