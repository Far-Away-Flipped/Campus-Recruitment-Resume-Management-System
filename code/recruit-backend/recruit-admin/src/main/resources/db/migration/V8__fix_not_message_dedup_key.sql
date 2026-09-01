-- ============================================================================
-- V8: 修复 not_message 缺 dedup_key 列导致消息接口报错
--
-- 背景：NotMessage 实体含 dedup_key 字段（幂等键，格式
--       APPLICATION_STATUS_CHANGED:{app_status_history.id}，用于防止重复推送
--       状态变更通知，DB 唯一索引兜底），但 not_message 建表脚本漏了该列，
--       导致 MyBatis-Plus 查询/插入带上 dedup_key → Unknown column 报错
--       （/api/portal/messages 查询返回 10002）。
--
-- 修复：补 dedup_key 列 + 唯一索引 uk_dedup_key。
--       MySQL 唯一索引允许多个 NULL（NULL 不参与唯一约束），
--       故普通消息（无 dedup_key）不受影响，仅同 historyId 的
--       状态变更消息被唯一索引去重 —— 与 NotifyServiceImpl 注释一致。
--
-- 幂等性：列/索引存在时零命中（information_schema 判断），可安全重复执行。
-- ============================================================================

-- 1. 补 dedup_key 列（列不存在才 ADD）
SET @col_exist = (SELECT COUNT(*) FROM information_schema.columns
                  WHERE table_schema = DATABASE() AND table_name = 'not_message'
                    AND column_name = 'dedup_key');
SET @sql_add_col = IF(@col_exist = 0,
    'ALTER TABLE `not_message` ADD COLUMN `dedup_key` VARCHAR(128) DEFAULT NULL COMMENT ''幂等键：APPLICATION_STATUS_CHANGED:{historyId}，唯一防重''',
    'SELECT 1');
PREPARE s_add_col FROM @sql_add_col;
EXECUTE s_add_col;
DEALLOCATE PREPARE s_add_col;

-- 2. 补唯一索引 uk_dedup_key（索引不存在才建）
SET @idx_exist = (SELECT COUNT(*) FROM information_schema.statistics
                  WHERE table_schema = DATABASE() AND table_name = 'not_message'
                    AND index_name = 'uk_dedup_key');
SET @sql_add_idx = IF(@idx_exist = 0,
    'ALTER TABLE `not_message` ADD UNIQUE KEY `uk_dedup_key` (`dedup_key`)',
    'SELECT 1');
PREPARE s_add_idx FROM @sql_add_idx;
EXECUTE s_add_idx;
DEALLOCATE PREPARE s_add_idx;
