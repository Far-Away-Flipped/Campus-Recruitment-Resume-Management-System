-- ============================================================================
-- V9: 新增"投递来源渠道"字典(apply_source)
--
-- 背景：投递记录 app_application.source 此前由前端直存中文（如"官网"），
--       且后端 SourceChannel 枚举有另一套英文码，两处不一致、无法在字典
--       管理页维护。本次新增 apply_source 字典，字典项 value 与 SourceChannel
--       枚举码对齐，供来源渠道下拉/导出映射使用。
--
-- 幂等性：本文件可安全重复执行（崩溃重跑不报错、不产生脏数据）。
--         sys_dict_type 有 uk_dict_type 唯一键 → 用 ON DUPLICATE KEY 保幂等；
--         sys_dict_data 无唯一键 → 用 WHERE NOT EXISTS 保幂等。
-- ============================================================================

-- 1. 字典类型：投递来源渠道
INSERT INTO `sys_dict_type`
    (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
VALUES
    ('投递来源渠道', 'apply_source', '0', 'admin', NOW(), '简历投递来源渠道（与SourceChannel枚举一致）')
ON DUPLICATE KEY UPDATE
    `dict_name` = VALUES(`dict_name`),
    `remark` = VALUES(`remark`);

-- 2. 字典数据：官网/宣讲会/内推/其他
INSERT INTO `sys_dict_data`
    (`dict_sort`, `dict_label`, `dict_value`, `dict_type`,
     `css_class`, `is_default`, `status`, `create_by`, `create_time`)
SELECT 1, '官网',   'OFFICIAL_SITE', 'apply_source', '', 'Y', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'apply_source' AND `dict_value` = 'OFFICIAL_SITE');

INSERT INTO `sys_dict_data`
    (`dict_sort`, `dict_label`, `dict_value`, `dict_type`,
     `css_class`, `is_default`, `status`, `create_by`, `create_time`)
SELECT 2, '宣讲会', 'CAMPUS_TALK', 'apply_source', '', 'N', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'apply_source' AND `dict_value` = 'CAMPUS_TALK');

INSERT INTO `sys_dict_data`
    (`dict_sort`, `dict_label`, `dict_value`, `dict_type`,
     `css_class`, `is_default`, `status`, `create_by`, `create_time`)
SELECT 3, '内推',   'REFERRAL', 'apply_source', '', 'N', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'apply_source' AND `dict_value` = 'REFERRAL');

INSERT INTO `sys_dict_data`
    (`dict_sort`, `dict_label`, `dict_value`, `dict_type`,
     `css_class`, `is_default`, `status`, `create_by`, `create_time`)
SELECT 4, '其他',   'OTHER', 'apply_source', '', 'N', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'apply_source' AND `dict_value` = 'OTHER');
