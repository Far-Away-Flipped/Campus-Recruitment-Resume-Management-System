-- ============================================================================
-- V4: 岗位学历要求字典化（存码值）
--
-- 背景：岗位学历要求（job_position/job_template.degree_requirement）原存中文
--       （如"本科"），改为存 education_degree 字典码值（BACHELOR 等），与学生
--       简历学历口径统一。字典补一项 NONE=不限（岗位下拉原有"不限"选项）。
--
-- 幂等性：CASE WHEN + WHERE IN 保证只命中待迁移的中文行，重跑零命中；
--         INSERT WHERE NOT EXISTS 保证字典不重复插入。可安全重复执行。
-- ============================================================================

-- 1. 字典 education_degree 补"不限"（NONE，status='0'，dict_sort=6）
INSERT INTO `sys_dict_data`
    (`dict_sort`, `dict_label`, `dict_value`, `dict_type`,
     `css_class`, `is_default`, `status`, `create_by`, `create_time`)
SELECT 6, '不限', 'NONE', 'education_degree',
       '', 'N', '0', 'admin', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dict_data`
    WHERE `dict_type` = 'education_degree' AND `dict_value` = 'NONE'
);

-- 2. job_position 存量中文 → 码值（WHERE IN 保证只命中待迁移行）
UPDATE `job_position`
SET `degree_requirement` = CASE `degree_requirement`
        WHEN '本科' THEN 'BACHELOR'
        WHEN '硕士' THEN 'MASTER'
        WHEN '博士' THEN 'DOCTOR'
        WHEN '大专' THEN 'ASSOCIATE'
        WHEN '其他' THEN 'OTHER'
        WHEN '不限' THEN 'NONE'
        ELSE `degree_requirement`
    END
WHERE `degree_requirement` IN ('本科','硕士','博士','大专','其他','不限');

-- 3. job_template 存量中文 → 码值
UPDATE `job_template`
SET `degree_requirement` = CASE `degree_requirement`
        WHEN '本科' THEN 'BACHELOR'
        WHEN '硕士' THEN 'MASTER'
        WHEN '博士' THEN 'DOCTOR'
        WHEN '大专' THEN 'ASSOCIATE'
        WHEN '其他' THEN 'OTHER'
        WHEN '不限' THEN 'NONE'
        ELSE `degree_requirement`
    END
WHERE `degree_requirement` IN ('本科','硕士','博士','大专','其他','不限');
