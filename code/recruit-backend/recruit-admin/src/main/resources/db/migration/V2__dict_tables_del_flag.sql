-- ============================================================================
-- V2: 修复字典表缺失 del_flag 列
--
-- 背景：SysDictType/SysDictData 实体继承 BaseEntity（其 delFlag 字段带
--       @TableLogic 注解），MyBatis-Plus 查询自动加 WHERE del_flag = 0；
--       但 sys_dict_type / sys_dict_data 建表漏了 del_flag 列，
--       导致字典管理接口全部报 Unknown column 'del_flag'（500）。
--
-- 幂等性：MySQL 8 无 ADD COLUMN IF NOT EXISTS，用 information_schema
--         判断列已存在则跳过，可安全重复执行。
-- ============================================================================

-- 1. sys_dict_type 补 del_flag 列（列已存在则跳过）
SET @col_type = (SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'sys_dict_type'
                   AND column_name = 'del_flag');
SET @sql_type = IF(@col_type = 0,
    'ALTER TABLE `sys_dict_type` ADD COLUMN `del_flag` CHAR(1) DEFAULT ''0'' COMMENT ''删除标志：0-存在 2-删除''',
    'SELECT 1');
PREPARE s_type FROM @sql_type;
EXECUTE s_type;
DEALLOCATE PREPARE s_type;

-- 2. sys_dict_data 补 del_flag 列（列已存在则跳过）
SET @col_data = (SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'sys_dict_data'
                   AND column_name = 'del_flag');
SET @sql_data = IF(@col_data = 0,
    'ALTER TABLE `sys_dict_data` ADD COLUMN `del_flag` CHAR(1) DEFAULT ''0'' COMMENT ''删除标志：0-存在 2-删除''',
    'SELECT 1');
PREPARE s_data FROM @sql_data;
EXECUTE s_data;
DEALLOCATE PREPARE s_data;
