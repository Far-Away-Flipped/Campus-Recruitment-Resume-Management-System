-- ============================================================================
-- V1: 工作地点多选改造
--
-- 目标：
--   1) job_position.location / job_template.location 由单值 VARCHAR(128) 扩为
--      VARCHAR(512)，存储 JSON 数组文本（如 ["BEIJING","SHANGHAI"]）
--   2) 字典补"广州"（work_location）
--   3) 存量单值数据 → 码值 JSON 数组文本
--
-- 幂等性：本文件可安全重复执行（崩溃重跑不报错、不产生脏数据）。
--         已应用版本靠 schema_version 去重，文件本身也不依赖版本表。
-- 约定：initdb 基线 = V0；编辑已应用版本文件 = 反模式，一律新增版本。
-- ============================================================================

-- 1. job_position.location 扩列（已是 512 时同定义 MODIFY 为 no-op，幂等）
ALTER TABLE `job_position`
    MODIFY COLUMN `location` VARCHAR(512) NOT NULL DEFAULT ''
    COMMENT '工作地点（JSON数组，如["BEIJING","SHANGHAI"]）';

-- 2. job_template.location 扩列
ALTER TABLE `job_template`
    MODIFY COLUMN `location` VARCHAR(512) DEFAULT NULL
    COMMENT '工作地点（JSON数组，如["BEIJING","SHANGHAI"]）';

-- 3. 字典补"广州"（sys_dict_data 无唯一键，用 WHERE NOT EXISTS 保幂等）
INSERT INTO `sys_dict_data`
    (`dict_sort`, `dict_label`, `dict_value`, `dict_type`,
     `css_class`, `is_default`, `status`, `create_by`, `create_time`)
SELECT 10, '广州', 'GUANGZHOU', 'work_location',
       '', 'N', '0', 'admin', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dict_data`
    WHERE `dict_type` = 'work_location' AND `dict_value` = 'GUANGZHOU'
);

-- 4. 存量单值 → 码值 JSON 数组文本
--    命中条件：非空、trim 后非空、不以 [ 开头（已转换过的跳过）
--    已知中文标签 → 码值数组；未知值（含已是码值的单值）→ 原样包数组
UPDATE `job_position`
SET `location` = CASE TRIM(`location`)
        WHEN '北京' THEN '["BEIJING"]'
        WHEN '上海' THEN '["SHANGHAI"]'
        WHEN '西安' THEN '["XIAN"]'
        WHEN '深圳' THEN '["SHENZHEN"]'
        WHEN '成都' THEN '["CHENGDU"]'
        WHEN '武汉' THEN '["WUHAN"]'
        WHEN '杭州' THEN '["HANGZHOU"]'
        WHEN '南京' THEN '["NANJING"]'
        WHEN '合肥' THEN '["HEFEI"]'
        WHEN '广州' THEN '["GUANGZHOU"]'
        ELSE CONCAT('["', REPLACE(TRIM(`location`), '"', '\\"'), '"]')
    END
WHERE `location` IS NOT NULL
  AND TRIM(`location`) <> ''
  AND TRIM(`location`) NOT LIKE '[%';
