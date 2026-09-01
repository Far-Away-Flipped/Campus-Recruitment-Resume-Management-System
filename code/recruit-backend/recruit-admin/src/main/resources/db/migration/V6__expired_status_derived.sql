-- ============================================================================
-- V6: 岗位过期状态实时派生，不再持久化 EXPIRED
--
-- 背景：过期状态原由定时任务 JobExpireTask 把 status=PUBLISHED 且 deadline<NOW()
--       的岗位写死为 EXPIRED。一旦写入，即使修改 deadline 到未来也不会恢复，
--       且存在 5 分钟窗口期。
--
-- 修复：EXPIRED 改为后端按 deadline 实时计算（仅输出层覆盖，不存库）。
--       存量所有 EXPIRED 行归一为 PUBLISHED，让实时计算统一接管。
--
-- 幂等性：UPDATE WHERE status='EXPIRED' 天然幂等（无 EXPIRED 行时零命中）。
-- ============================================================================

UPDATE `job_position` SET `status` = 'PUBLISHED' WHERE `status` = 'EXPIRED';
