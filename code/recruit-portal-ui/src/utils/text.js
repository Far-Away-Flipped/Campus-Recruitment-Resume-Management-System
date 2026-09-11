/**
 * 文本展示辅助：把被压平成一行的小标题条目拆回多行
 *
 * 背景（不要删这段注释，换人接手极易踩回去）：
 * 后端保存岗位描述/任职要求时对纯文本字段调用了 `Jsoup.clean(text, SAFE_HTML)`
 * （recruit-biz JobAdminController），HTML 解析器会把文本里的换行按空白折叠。
 * HR 在后台 textarea 里写的「1. xxx⏎2. yyy⏎3. zzz」入库时就变成了
 * 「1. xxx 2. yyy 3. zzz」——换行已经在**存储环节**丢失，前端拿不到。
 *
 * 因此这里只能按序号标记反推分段。注意这是**有损兜底**：
 * 没有序号的纯多段落文本无法还原（换行已不可恢复），
 * 根治要在后端保存时不要对纯文本做 HTML 清洗（或在清洗前把 \n 转成 <br>）。
 */

/** 序号标记：行首或空白之后，1-2 位数字 + 分隔符，且分隔符后不能紧跟数字（排除 3.8、2026.9.30 这类小数/日期） */
const MARK_G = /(?:^|\s)\d{1,2}\s*[.、,，．)）](?!\d)/g;
/** 同一规则的零宽版本，用于 split 切分点 */
const MARK_SPLIT = /(?=(?:\s|^)\d{1,2}\s*[.、,，．)）](?!\d))/;

/**
 * 把一段文本拆成展示用的行数组。
 * - 本身带换行的：先按换行拆
 * - 被压平的「1. … 2. … 3. …」：按序号拆（至少出现 2 个序号才拆，避免误伤正文里的单个数字）
 * - 其余原样返回一条
 */
export function splitNumberedLines(text) {
  const out = [];
  for (const rawLine of String(text ?? '').split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line) continue;

    const marks = line.match(MARK_G);
    if (!marks || marks.length < 2) {
      out.push(line);
      continue;
    }
    out.push(...line.split(MARK_SPLIT).map((s) => s.trim()).filter(Boolean));
  }
  return out;
}

/**
 * 卡片摘要用：拆段后只保留前若干段，超出部分收成省略号。
 * 段落之间会换行展示，所以这里按「段」而不是按字符总数控制长度，
 * 避免把一条条目从中间切断。
 *
 * @param {string} text 原始文本
 * @param {number} maxLines 最多展示几段
 * @param {number} maxCharsPerLine 每段最多多少字符
 * @returns {string[]} 展示用文本行
 */
export function summarizeLines(text, maxLines = 3, maxCharsPerLine = 40) {
  const lines = splitNumberedLines(text);
  const shown = lines.slice(0, maxLines).map((line) =>
    line.length > maxCharsPerLine ? `${line.slice(0, maxCharsPerLine)}…` : line
  );
  if (lines.length > maxLines && shown.length > 0) {
    shown[shown.length - 1] = `${shown[shown.length - 1].replace(/…$/, '')} …`;
  }
  return shown;
}
