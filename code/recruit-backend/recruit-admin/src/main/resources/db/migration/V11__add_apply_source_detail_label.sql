-- ============================================================================
-- V11: apply_source 字典项 remark 用作投递页选填详情字段的显示名称
--
-- 背景：投递确认页选渠道后需支持附带选填详情（如内推的内推人+部门）。
--       约定：字典项 remark 非空 = 该渠道投递时显示一个选填详情输入框，
--       remark 即该字段的显示名称（HR 可在字典管理页修改）。
--       种子状态仅"内推"配置 remark，其余渠道不受影响。
--
-- 幂等性：remark 仅在为空时写入，不覆盖 HR 已自定义的值；
--         字典类型 remark 为约定说明，同值重写无副作用。
-- ============================================================================

-- 1. 内推字典项备注 = 投递页选填详情字段的显示名称（仅在为空时写入，不覆盖 HR 自定义）
UPDATE `sys_dict_data`
SET `remark` = '内推人+部门', `update_by` = 'admin', `update_time` = NOW()
WHERE `dict_type` = 'apply_source' AND `dict_value` = 'REFERRAL'
  AND (`remark` IS NULL OR `remark` = '');

-- 2. 字典类型备注补充约定说明（字典管理-类型编辑弹窗可见，幂等：同值重写）
UPDATE `sys_dict_type`
SET `remark` = '简历投递来源渠道（与SourceChannel枚举一致）；字典项备注=该渠道投递时选填详情字段的显示名称', `update_by` = 'admin', `update_time` = NOW()
WHERE `dict_type` = 'apply_source';
