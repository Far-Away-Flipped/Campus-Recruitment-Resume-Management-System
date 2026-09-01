-- ============================================================================
-- V3: 清理冗余字典 job_category_type
--
-- 背景：岗位类别实际由 job_category 表管理（HR 后台创建岗位时下拉来自
--       /api/admin/job-categories/tree，读 job_category 表），字典中的
--       job_category_type 从未被任何流程消费，属冗余元数据，物理删除。
--
-- 幂等性：DELETE 天然幂等（删除不存在的行无副作用），可安全重复执行。
-- ============================================================================

-- 1. 删除 job_category_type 的字典数据（3 条：硬件研发/软件研发/职能）
DELETE FROM `sys_dict_data` WHERE `dict_type` = 'job_category_type';

-- 2. 删除 job_category_type 的字典类型（1 条）
DELETE FROM `sys_dict_type` WHERE `dict_type` = 'job_category_type';
