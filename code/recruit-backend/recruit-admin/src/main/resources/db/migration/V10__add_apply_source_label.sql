-- ============================================================================
-- V10: app_application 渠道来源快照化 —— 新增 source_label + 存量归一
--
-- 背景：source 此前由前端直存中文，与 SourceChannel 枚举码两套值共存。本次
--       将 source 统一为 apply_source 字典码（OFFICIAL_SITE 等），新增
--       source_label 存中文快照（投递时按字典固化，字典日后变动不影响历史展示）。
--
-- 幂等性：加列用 information_schema 判存在（参照 V2）；存量 UPDATE 只命中
--         source 仍为中文 / source_label 仍为空的行，重跑零命中（参照 V1/V4）。
-- ============================================================================

-- 1. 加列（列已存在则跳过）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'app_application'
                     AND column_name = 'source_label');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `app_application` ADD COLUMN `source_label` VARCHAR(32) DEFAULT NULL COMMENT ''渠道来源中文快照（投递时按字典固化，字典变更不影响历史）'' AFTER `source_detail`',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. 存量中文 → 字典码（只命中 4 个已知中文，重跑零命中）
UPDATE `app_application`
SET `source` = CASE `source`
        WHEN '官网'   THEN 'OFFICIAL_SITE'
        WHEN '宣讲会' THEN 'CAMPUS_TALK'
        WHEN '内推'   THEN 'REFERRAL'
        WHEN '其他'   THEN 'OTHER'
        ELSE `source`
    END
WHERE `del_flag` = '0' AND `source` IN ('官网','宣讲会','内推','其他');

-- 3. 回填 label 快照：以 apply_source 字典为准反查；字典无此项（已删码/脏值）时
--    原样落 label 以便审计（COALESCE 兜底），只命中 source_label 为 NULL 的行，重跑零命中
UPDATE `app_application` a
LEFT JOIN `sys_dict_data` d
       ON d.`dict_type` = 'apply_source' AND d.`dict_value` = a.`source`
      AND d.`del_flag` = '0' AND d.`status` = '0'
SET a.`source_label` = COALESCE(d.`dict_label`, a.`source`)
WHERE a.`del_flag` = '0'
  AND a.`source` IS NOT NULL AND TRIM(a.`source`) <> ''
  AND a.`source_label` IS NULL;
