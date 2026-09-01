-- ============================================================================
-- V5: 清理岗位标签（tags）坏数据
--
-- 背景：旧前端把标签以逗号分隔字符串提交，后端 str() 对 List.toString() 存成
--       "[Java, 应届生]" 伪 JSON；回填后再次 split(',') 又嵌套一层方括号，
--       最终产生 [[[]]]、[[Java, 应届生]] 这类非法 JSON 数组文本。
--
-- 修复：只保留合法的 JSON 数组文本（如 ["急聘","Java"] 或空数组 []），
--       其余坏值置 NULL。本迁移幂等：已合法/已 NULL 的行不会被再次改动。
--
-- 说明：库存 2 条岗位的坏值 [[[]]]、[] 均为无意义标签，[] 是合法空数组可保留，
--       [[[]]] 不符合合法 JSON 数组文本正则 → 置 NULL。
-- ============================================================================

-- 1. job_position：非法 JSON 数组文本的 tags 置 NULL
UPDATE `job_position`
SET `tags` = NULL
WHERE `tags` IS NOT NULL AND TRIM(`tags`) <> ''
  AND TRIM(`tags`) NOT REGEXP '^\\[(\\"[^\\"]*\\"(,\\"[^\\"]*\\")*)?\\]$';

-- 2. job_template：同样清理
UPDATE `job_template`
SET `tags` = NULL
WHERE `tags` IS NOT NULL AND TRIM(`tags`) <> ''
  AND TRIM(`tags`) NOT REGEXP '^\\[(\\"[^\\"]*\\"(,\\"[^\\"]*\\")*)?\\]$';
