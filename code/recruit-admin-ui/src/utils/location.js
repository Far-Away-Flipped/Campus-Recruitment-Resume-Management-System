/**
 * 工作地点工具
 * 岗位 location 存储为 JSON 数组文本（如 ["BEIJING","SHANGHAI"]），码值来自 work_location 字典。
 * 兼容三种形态：JSON 数组 / 单值字符串（北京 或 BEIJING）/ JSON 数组文本。
 */

/** 码值 → 中文名 硬编码兜底（与 work_location 字典一致，共 10 项） */
export const LOCATION_LABELS = {
  BEIJING: '北京',
  SHANGHAI: '上海',
  XIAN: '西安',
  SHENZHEN: '深圳',
  CHENGDU: '成都',
  WUHAN: '武汉',
  HANGZHOU: '杭州',
  NANJING: '南京',
  HEFEI: '合肥',
  GUANGZHOU: '广州',
};

/** 解析 location 为码值数组 */
export function parseLoc(v) {
  if (Array.isArray(v)) return v.filter(Boolean);
  if (v == null) return [];
  if (typeof v !== 'string') return [];
  const s = v.trim();
  if (!s) return [];
  if (s.startsWith('[')) {
    try {
      const arr = JSON.parse(s);
      return Array.isArray(arr) ? arr.filter(Boolean) : [];
    } catch {
      return [];
    }
  }
  return [s];
}

/** 格式化 location 为中文名拼接（如 "北京、上海"），未知码值原样返回 */
export function formatLoc(v) {
  const codes = parseLoc(v);
  return codes
    .map(code => LOCATION_LABELS[code] || code)
    .join('、');
}

/**
 * 学历工具：岗位学历要求存 education_degree 字典码值（如 BACHELOR）。
 */

/** 学历码值 → 中文 硬编码兜底（与 education_degree 字典一致，共 6 项） */
export const DEGREE_LABELS = {
  ASSOCIATE: '大专',
  BACHELOR: '本科',
  MASTER: '硕士',
  DOCTOR: '博士',
  OTHER: '其他',
  NONE: '不限',
};

/** 格式化学历码值为中文，未知码值/空值原样处理 */
export function formatDegree(v) {
  if (v == null || v === '') return '';
  return DEGREE_LABELS[v] || v;
}

/**
 * 标签工具：岗位标签存合法 JSON 数组文本（如 ["急聘","Java"]）。
 */

/** 解析 tags 为字符串数组，兼容数组/JSON 数组文本/逗号分隔字符串/空 */
export function parseTags(v) {
  if (Array.isArray(v)) return v.filter(t => t && t.trim());
  if (v == null) return [];
  if (typeof v !== 'string') return [];
  const s = v.trim();
  if (!s) return [];
  if (s.startsWith('[')) {
    try {
      const arr = JSON.parse(s);
      return Array.isArray(arr) ? arr.filter(t => t && t.trim()) : [];
    } catch {
      return [];
    }
  }
  return s.split(',').map(t => t.trim()).filter(Boolean);
}
