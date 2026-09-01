-- ============================================================================
-- V7: 修复 app_snapshot.application_id NOT NULL 导致投递失败
--
-- 背景：投递事务（ApplicationServiceImpl.submitApplication）先 INSERT 快照
--       （此刻不填 application_id，先拿自增 snapshot_id），拿到 application_id
--       后再回填。但 app_snapshot.application_id 建表为 NOT NULL 且无默认值，
--       导致首条 INSERT 报 "Field 'application_id' doesn't have a default value"
--       → DataIntegrityViolationException → 返回 10002 系统内部错误。
--
-- 修复：把 application_id 改为可空（DEFAULT NULL）。代码回填逻辑不变：
--       INSERT 时不传 application_id（置 NULL）→ 回填 UPDATE 仍有效。
--
-- 幂等性：仅当列当前为 NOT NULL 时才执行 MODIFY，可安全重复执行；
--         列已可空或表不存在时零命中（查询返回 0 → 跳过）。
-- ============================================================================

-- 1. 判断 app_snapshot.application_id 当前是否可空（IS_NULLABLE='NO' 才需修改）
SET @col_info = (SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'app_snapshot'
                   AND column_name = 'application_id'
                   AND is_nullable = 'NO');
SET @sql_modify = IF(@col_info > 0,
    'ALTER TABLE `app_snapshot` MODIFY COLUMN `application_id` BIGINT DEFAULT NULL COMMENT ''关联投递记录ID（投递成功后回填）''',
    'SELECT 1');
PREPARE s_modify FROM @sql_modify;
EXECUTE s_modify;
DEALLOCATE PREPARE s_modify;
