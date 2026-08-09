-- ============================================================================
-- 遨天科技 校园招聘管理系统 - 初始化数据脚本
-- 数据库：MySQL 8.0, 库名 atmoto_recruit
-- 说明：此脚本在系统首次部署时执行，初始化字典、角色、账号、品牌配置等基础数据
-- ============================================================================

SET NAMES utf8mb4;

-- ============================================================================
-- 1. 字典类型表（sys_dict_type）
-- ============================================================================

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark) VALUES
('学历',          'education_degree',       '0', 'admin', NOW(), '候选人学历字典'),
('候选人状态',    'candidate_status',       '0', 'admin', NOW(), '候选人投递状态（一期仅启用部分）'),
('岗位类别',      'job_category_type', '0', 'admin', NOW(), '岗位分类三大序列'),
('工作地点',      'work_location',    '0', 'admin', NOW(), '办公城市列表'),
('通知渠道',      'notify_channel',   '0', 'admin', NOW(), '通知发送渠道：短信/邮件/系统消息');

-- ============================================================================
-- 2. 字典数据表（sys_dict_data）
-- ============================================================================

-- 2.1 学历字典
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, is_default, status, create_by, create_time) VALUES
(1, '大专',   'ASSOCIATE', 'education_degree', '', 'N', '0', 'admin', NOW()),
(2, '本科',   'BACHELOR',  'education_degree', '', 'N', '0', 'admin', NOW()),
(3, '硕士',   'MASTER',    'education_degree', '', 'N', '0', 'admin', NOW()),
(4, '博士',   'DOCTOR',    'education_degree', '', 'N', '0', 'admin', NOW()),
(5, '其他',   'OTHER',     'education_degree', '', 'N', '0', 'admin', NOW());

-- 2.2 候选人状态字典（12个状态，标注 p0_active）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, is_default, status, create_by, create_time, remark) VALUES
(1,  '待筛选',   'PENDING_SCREEN',     'candidate_status', '', 'Y', '0', 'admin', NOW(), '一期启用，初始默认状态'),
(2,  '筛选通过', 'SCREEN_PASSED',      'candidate_status', '', 'N', '0', 'admin', NOW(), '一期启用'),
(3,  '已淘汰',   'ELIMINATED',         'candidate_status', '', 'N', '0', 'admin', NOW(), '一期启用，终态'),
(4,  '待面试',   'PENDING_INTERVIEW',  'candidate_status', '', 'N', '0', 'admin', NOW(), 'P2 预留'),
(5,  '面试中',   'IN_INTERVIEW',       'candidate_status', '', 'N', '0', 'admin', NOW(), 'P2 预留'),
(6,  '面试通过', 'INTERVIEW_PASSED',   'candidate_status', '', 'N', '0', 'admin', NOW(), 'P2 预留'),
(7,  '待录用',   'PENDING_OFFER',      'candidate_status', '', 'N', '0', 'admin', NOW(), 'P2 预留'),
(8,  '已发Offer','OFFER_SENT',         'candidate_status', '', 'N', '0', 'admin', NOW(), 'P2 预留'),
(9,  '已接受',   'ACCEPTED',           'candidate_status', '', 'N', '0', 'admin', NOW(), 'P2 预留'),
(10, '已拒绝',   'REJECTED',           'candidate_status', '', 'N', '0', 'admin', NOW(), 'P2 预留，终态'),
(11, '已入职',   'ONBOARDED',          'candidate_status', '', 'N', '0', 'admin', NOW(), 'P2 预留，终态'),
(12, '已撤回',   'WITHDRAWN',          'candidate_status', '', 'N', '0', 'admin', NOW(), '候选人主动撤回，终态');

-- 2.3 工作地点
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, is_default, status, create_by, create_time) VALUES
(1, '北京', 'BEIJING',   'work_location', '', 'N', '0', 'admin', NOW()),
(2, '上海', 'SHANGHAI',  'work_location', '', 'N', '0', 'admin', NOW()),
(3, '西安', 'XIAN',      'work_location', '', 'N', '0', 'admin', NOW()),
(4, '深圳', 'SHENZHEN',  'work_location', '', 'N', '0', 'admin', NOW()),
(5, '成都', 'CHENGDU',   'work_location', '', 'N', '0', 'admin', NOW()),
(6, '武汉', 'WUHAN',     'work_location', '', 'N', '0', 'admin', NOW()),
(7, '杭州', 'HANGZHOU',  'work_location', '', 'N', '0', 'admin', NOW()),
(8, '南京', 'NANJING',   'work_location', '', 'N', '0', 'admin', NOW()),
(9, '合肥', 'HEFEI',     'work_location', '', 'N', '0', 'admin', NOW());

-- 2.4 岗位类别字典（三大序列）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, is_default, status, create_by, create_time) VALUES
(1, '硬件研发', 'HARDWARE_RD', 'job_category_type', '', 'N', '0', 'admin', NOW()),
(2, '软件研发', 'SOFTWARE_RD', 'job_category_type', '', 'N', '0', 'admin', NOW()),
(3, '职能',     'FUNCTION',    'job_category_type', '', 'N', '0', 'admin', NOW());

-- 2.5 通知渠道
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, is_default, status, create_by, create_time) VALUES
(1, '短信',     'SMS',     'notify_channel', '', 'N', '0', 'admin', NOW()),
(2, '邮件',     'EMAIL',   'notify_channel', '', 'N', '0', 'admin', NOW()),
(3, '系统消息', 'SYSTEM',  'notify_channel', '', 'N', '0', 'admin', NOW());

-- ============================================================================
-- 3. 岗位类别表（job_category）
-- 三大序列 + 硬件研发下设子类别
-- ============================================================================

INSERT INTO job_category (category_id, parent_id, ancestors, category_name, category_code, sort_order, status, create_by, create_time) VALUES
-- 三大父级序列
(1,  0, '0',     '硬件研发', 'HARDWARE_RD', 1, '0', 'admin', NOW()),
(2,  0, '0',     '软件研发', 'SOFTWARE_RD', 2, '0', 'admin', NOW()),
(3,  0, '0',     '职能',     'FUNCTION',    3, '0', 'admin', NOW()),
-- 硬件研发子类别
(11, 1, '0,1',   '电源',     'HW_POWER',     1, '0', 'admin', NOW()),
(12, 1, '0,1',   '结构',     'HW_STRUCTURE', 2, '0', 'admin', NOW()),
(13, 1, '0,1',   '控制',     'HW_CONTROL',   3, '0', 'admin', NOW()),
(14, 1, '0,1',   '测试',     'HW_TEST',      4, '0', 'admin', NOW()),
(15, 1, '0,1',   '工艺',     'HW_PROCESS',   5, '0', 'admin', NOW());

-- ============================================================================
-- 4. 菜单表（sys_menu）—— 管理后台菜单树
-- ============================================================================

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
-- 一级菜单
(1,  '系统管理', 0, 1, 'system',     NULL,    '1', '0', 'M', '0', '0', '',        'system',   'admin', NOW()),
(2,  '业务管理', 0, 2, 'biz',         NULL,    '1', '0', 'M', '0', '0', '',        'chart',    'admin', NOW()),
-- 系统管理子菜单
(10, '用户管理', 1, 1, 'user',       'system/user/index',       '1', '0', 'C', '0', '0', 'system:user:list',    'user',    'admin', NOW()),
(11, '角色管理', 1, 2, 'role',       'system/role/index',       '1', '0', 'C', '0', '0', 'system:role:list',    'peoples', 'admin', NOW()),
(12, '菜单管理', 1, 3, 'menu',       'system/menu/index',       '1', '0', 'C', '0', '0', 'system:menu:list',    'tree',    'admin', NOW()),
(13, '部门管理', 1, 4, 'dept',       'system/dept/index',       '1', '0', 'C', '0', '0', 'system:dept:list',    'tree',    'admin', NOW()),
(14, '字典管理', 1, 5, 'dict',       'system/dict/index',       '1', '0', 'C', '0', '0', 'system:dict:list',    'dict',    'admin', NOW()),
-- 业务管理子菜单
(20, '品牌配置', 2, 1, 'brand',      'biz/brand/index',         '1', '0', 'C', '0', '0', 'biz:brand:list',      'example',        'admin', NOW()),
(21, '岗位类别', 2, 2, 'jobCategory','biz/jobCategory/index',   '1', '0', 'C', '0', '0', 'biz:jobCategory:list','cascader',       'admin', NOW()),
(22, 'Banner管理',2, 3, 'banner',    'biz/banner/index',        '1', '0', 'C', '0', '0', 'biz:banner:list',     'list',           'admin', NOW()),
(23, '通知模板', 2, 4, 'notify',     'biz/notify/index',        '1', '0', 'C', '0', '0', 'biz:notify:list',     'message',        'admin', NOW());

-- ============================================================================
-- 5. 角色表（sys_role）
-- ============================================================================

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, create_by, create_time, remark) VALUES
(1, '人力资源总监', 'hr_director',   1, '1', '0', 'admin', NOW(), '拥有全部管理权限（含数据导出）'),
(2, '招聘专员',     'hr_recruiter',  2, '2', '0', 'admin', NOW(), '仅可查看所负责岗位的简历，不可导出');

-- ============================================================================
-- 6. 角色-菜单关联表（sys_role_menu）
-- ============================================================================

-- hr_director 拥有所有菜单权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14),
(1, 20), (1, 21), (1, 22), (1, 23);

-- hr_recruiter 仅拥有业务管理下的岗位类别和Banner查看权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 2), (2, 21), (2, 22);

-- ============================================================================
-- 7. 部门表（sys_dept）
-- ============================================================================

INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, create_by, create_time) VALUES
(1, 0, '0',     '遨天科技',      0, 'CEO',       '0', 'admin', NOW()),
(2, 1, '0,1',   '人文发展部',    1, 'HR总监',   '0', 'admin', NOW()),
(3, 1, '0,1',   '硬件研发中心',  2, '硬件VP',   '0', 'admin', NOW()),
(4, 1, '0,1',   '软件研发中心',  3, '软件VP',   '0', 'admin', NOW()),
(5, 1, '0,1',   '市场与运营部',  4, '市场总监', '0', 'admin', NOW());

-- ============================================================================
-- 8. 初始 HR 账号（sys_user）
--    ⚠️ 密码为 BCrypt 加密，明文: admin123
--    如需重置密码，请使用 BCryptPasswordEncoder 重新生成哈希值
--    Spring Boot 可使用 org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
-- ============================================================================

INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, password, status, create_by, create_time, remark) VALUES
(1, 2, 'admin', '系统管理员', '00', 'admin@atmoto.com', '13800000000', '0',
 '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
 '0', 'admin', NOW(), '初始管理员账号');

-- ============================================================================
-- 9. 用户-角色关联表（sys_user_role）
-- ============================================================================

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- ============================================================================
-- 10. 品牌配置（sys_brand_config）
-- ============================================================================

INSERT INTO sys_brand_config (config_key, config_value, description, create_by, create_time) VALUES
('company_name',  '遨天科技（北京）有限公司', '公司全称',         'admin', NOW()),
('company_short_name',    '遨天科技',               '公司简称',         'admin', NOW()),
('brand_color', '#5FB8D6',               '品牌主色调',       'admin', NOW()),
('brand_bright_color','#6BB3FF',                  '品牌亮色/强调色',  'admin', NOW()),
('brand_bg_color',    '#0A0E17',                  '深空背景色',       'admin', NOW()),
('logo_url',      '',                       '公司Logo图片地址', 'admin', NOW()),
('footer_text',   '遨天科技（北京）有限公司 版权所有', '页面底部版权信息', 'admin', NOW()),
('contact_email', 'hr@atmoto.com',          'HR 联系邮箱',     'admin', NOW()),
('homepage_title','遨天科技校园招聘',       '首页标题',         'admin', NOW()),
('homepage_desc', '加入遨天，逐梦星辰',     '首页副标题/描述',  'admin', NOW());

-- ============================================================================
-- 11. 网络管理模块种子数据（HR后台「网络管理」模块设计方案V1.0 第4.4节，阶段1）
--     日期：2026-08-09
-- ============================================================================

-- 11.1 将 CorsConfig.java 硬编码的 DEFAULT_ORIGINS 迁移为落库数据，保证上线瞬间不中断现有访问
INSERT INTO sys_cors_origin (rule_type, rule_value, description, is_active, is_builtin, create_by, create_time) VALUES
('EXACT', 'http://localhost:5173',    '开发环境 Vite 默认端口', '1', '1', 'system_migration', NOW()),
('EXACT', 'http://localhost:5174',    '开发环境 Vite 备用端口', '1', '1', 'system_migration', NOW()),
('EXACT', 'http://127.0.0.1:5173',    '开发环境本机回环地址', '1', '1', 'system_migration', NOW()),
('EXACT', 'http://127.0.0.1:5174',    '开发环境本机回环地址（备用端口）', '1', '1', 'system_migration', NOW()),
('EXACT', 'https://campus.atmoto.cn', '生产环境门户域名（禁止误删，删除会导致门户前端跨域请求全部失败）', '1', '1', 'system_migration', NOW());

-- 11.2 局域网访问开关默认true，与当前"未做任何IP限制"的现状一致，避免上线瞬间收紧访问范围
INSERT INTO sys_network_config (config_key, config_value, config_type, config_group, description, create_by, create_time) VALUES
('lan_access_enabled', 'true', 'BOOLEAN', 'NETWORK', '是否允许局域网/内网IP直接访问HR后台。默认true保持现状，如需收紧请先在白名单中添加需要放行的网段', 'system_migration', NOW());

-- ============================================================================
-- 12. 网络管理模块菜单挂载（HR后台「网络管理」模块设计方案V1.0 第4.3节，阶段2）
-- ============================================================================

-- menu_id=15 续用系统管理子项序号段（10-19），挂载于系统管理 parent_id=1 下
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(15, '网络管理', 1, 6, 'network', 'system/network/index', '1', '0', 'C', '0', '0', 'system:network:list', 'server', 'admin', NOW());

-- 两个角色都绑定（读操作两角色皆可访问；写操作的实际权限边界由后端 requireNetworkAdminRole() 方法级校验承担，
-- 菜单可见性本身不构成访问控制——见方案4.3节【裁决4】）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 15), (2, 15);
