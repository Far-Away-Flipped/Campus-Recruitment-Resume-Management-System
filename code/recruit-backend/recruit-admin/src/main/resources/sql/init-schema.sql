-- ============================================================================
-- 遨天科技 校园招聘管理系统 - 初始化建表脚本
-- 数据库：MySQL 8.0, 库名 atmoto_recruit
-- 来源：设计文档 04-数据架构设计 第4节
-- 说明：全部自建表（23张），不含 RuoYi 框架自带表
-- ============================================================================

-- ============================================
-- 1. 学生账号表
-- ============================================
CREATE TABLE `stu_user` (
    `student_id`      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '学生ID',
    `phone`           VARCHAR(20)   NOT NULL COMMENT '手机号（登录账号，唯一）',
    `password_hash`   VARCHAR(128)  NOT NULL COMMENT '密码哈希（BCrypt）',
    `real_name`       VARCHAR(64)   DEFAULT NULL COMMENT '真实姓名',
    `email`           VARCHAR(128)  DEFAULT NULL COMMENT '邮箱',
    `status`          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态：ACTIVE-正常, DISABLED-禁用, LOCKED-临时锁定',
    `last_login_time` DATETIME      DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip`   VARCHAR(64)   DEFAULT NULL COMMENT '最后登录IP',
    `login_fail_count` INT          NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    `lock_until`      DATETIME      DEFAULT NULL COMMENT '账号锁定截止时间',
    `data_retention_days` INT       NOT NULL DEFAULT 730 COMMENT '数据保留天数',
    `auto_cleanup_date` DATE         DEFAULT NULL COMMENT '自动清理日期',
    `privacy_agreed`  CHAR(1)       NOT NULL DEFAULT '0' COMMENT '隐私政策同意标记',
    `privacy_agreed_time` DATETIME  DEFAULT NULL COMMENT '隐私政策同意时间',
    `create_by`       VARCHAR(64)   DEFAULT 'SELF' COMMENT '创建者',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)   DEFAULT NULL COMMENT '更新者',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`        CHAR(1)       NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`student_id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status` (`status`),
    KEY `idx_cleanup_date` (`auto_cleanup_date`),
    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生账号表（独立于sys_user，学生是外部用户）';

-- ============================================
-- 2. 学生基本资料表 S-004
-- ============================================
CREATE TABLE `stu_profile` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资料ID',
    `student_id`        BIGINT       NOT NULL COMMENT '关联学生账号ID',
    `real_name`         VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名（注册时建空资料，可为空）',
    `gender`            CHAR(1)      DEFAULT NULL COMMENT '性别：M-男, F-女, O-其他',
    `birth_date`        DATE         DEFAULT NULL COMMENT '出生日期',
    `phone`             VARCHAR(20)  NOT NULL COMMENT '手机号',
    `email`             VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `native_place`      VARCHAR(128) DEFAULT NULL COMMENT '户籍所在地',
    `current_residence` VARCHAR(128) DEFAULT NULL COMMENT '现居住地',
    `avatar_url`        VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `create_by`         VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`         VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`          CHAR(1)      NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_id` (`student_id`),
    KEY `idx_real_name` (`real_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生基本资料表 S-004';

-- ============================================
-- 3. 教育经历表 S-005（支持多条）
-- ============================================
CREATE TABLE `stu_education` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '教育经历ID',
    `student_id`  BIGINT       NOT NULL COMMENT '关联学生账号ID',
    `school_name` VARCHAR(128) NOT NULL COMMENT '学校名称',
    `major`       VARCHAR(128) NOT NULL COMMENT '专业名称',
    `degree`      VARCHAR(32)  NOT NULL COMMENT '学历：大专/本科/硕士/博士/其他',
    `start_date`  DATE         DEFAULT NULL COMMENT '入学时间',
    `end_date`    DATE         DEFAULT NULL COMMENT '毕业时间',
    `gpa_rank`    VARCHAR(20)  DEFAULT NULL COMMENT 'GPA或排名',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
    `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    CHAR(1)      NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_school_degree` (`school_name`, `degree`),
    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生教育经历表 S-005（支持多条记录）';

-- ============================================
-- 4. 实习/项目经历表 S-006（P1，一期建表留空）
-- ============================================
CREATE TABLE `stu_internship` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '经历ID',
    `student_id`  BIGINT       NOT NULL COMMENT '关联学生账号ID',
    `record_type` CHAR(1)      NOT NULL DEFAULT 'I' COMMENT '记录类型：I-实习经历, P-项目经历',
    `org_name`    VARCHAR(128) NOT NULL COMMENT '公司名称/项目名称',
    `position`    VARCHAR(128) DEFAULT NULL COMMENT '岗位名称/担任角色',
    `start_date`  DATE         DEFAULT NULL COMMENT '开始时间',
    `end_date`    DATE         DEFAULT NULL COMMENT '结束时间',
    `description` TEXT         DEFAULT NULL COMMENT '工作描述/成果描述',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
    `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    CHAR(1)      NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_record_type` (`record_type`),
    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生实习/项目经历表 S-006（P1预留，一期建表不实现业务逻辑）';

-- ============================================
-- 5. 技能/证书表 S-008（P1，一期建表留空）
-- ============================================
CREATE TABLE `stu_certificate` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '证书ID',
    `student_id`  BIGINT       NOT NULL COMMENT '关联学生账号ID',
    `cert_type`   VARCHAR(32)  NOT NULL COMMENT '类型：SKILL-技能, CERT-证书, LANGUAGE-语言能力',
    `cert_name`   VARCHAR(128) NOT NULL COMMENT '名称',
    `cert_level`  VARCHAR(64)  DEFAULT NULL COMMENT '等级/分数',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '补充说明',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
    `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    CHAR(1)      NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生技能/证书表 S-008（P1预留，一期建表不实现业务逻辑）';

-- ============================================
-- 6. 社团经历表 S-009（P1，一期建表留空）
-- ============================================
CREATE TABLE `stu_activity` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '社团经历ID',
    `student_id`  BIGINT       NOT NULL COMMENT '关联学生账号ID',
    `org_name`    VARCHAR(128) NOT NULL COMMENT '社团/组织名称',
    `position`    VARCHAR(64)  DEFAULT NULL COMMENT '担任职务',
    `description` TEXT         DEFAULT NULL COMMENT '主要职责及业绩',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
    `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    CHAR(1)      NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生社团经历表 S-009（P1预留，一期建表不实现业务逻辑）';

-- ============================================
-- 7. 简历附件表 S-007
-- ============================================
CREATE TABLE `stu_resume_file` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '附件ID',
    `student_id`      BIGINT       NOT NULL COMMENT '关联学生账号ID',
    `file_name`       VARCHAR(256) NOT NULL COMMENT '原始文件名',
    `file_path`       VARCHAR(512) NOT NULL COMMENT '文件存储路径',
    `file_size`       BIGINT       NOT NULL COMMENT '文件大小（字节）',
    `file_type`       VARCHAR(10)  NOT NULL COMMENT '文件类型：PDF/DOC/DOCX',
    `is_current`      CHAR(1)      NOT NULL DEFAULT '1' COMMENT '是否为当前版本',
    `preview_status`  VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '预览转换状态',
    `preview_path`    VARCHAR(512) DEFAULT NULL COMMENT '预览PDF路径',
    `preview_error`   VARCHAR(512) DEFAULT NULL COMMENT '预览转换失败原因',
    `upload_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `create_by`       VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`        CHAR(1)      NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_student_current` (`student_id`, `is_current`),
    KEY `idx_preview_status` (`preview_status`),
    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生简历附件表 S-007';

-- ============================================
-- 8. 岗位类别表 A-002（层级树）
-- ============================================
CREATE TABLE `job_category` (
    `category_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '类别ID',
    `category_name` VARCHAR(64)  NOT NULL COMMENT '类别名称',
    `category_code` VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '类别编码（唯一）',
    `parent_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '父级类别ID（0=顶级）',
    `ancestors`     VARCHAR(512) DEFAULT '' COMMENT '祖级列表',
    `sort_order`     INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
    `status`        CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0-停用, 1-启用',
    `create_by`     VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`category_id`),
    UNIQUE KEY `uk_category_code` (`category_code`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位类别表（层级树，反映公司真实业务序列）';

-- ============================================
-- 9. 岗位主表 H-001
-- ============================================
CREATE TABLE `job_position` (
    `job_id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `title`             VARCHAR(128) NOT NULL COMMENT '岗位名称',
    `department_id`     BIGINT       NOT NULL COMMENT '所属部门ID（关联sys_dept.dept_id）',
    `category_id`       BIGINT       NOT NULL COMMENT '岗位类别ID（关联job_category.id）',
    `location`          VARCHAR(128) NOT NULL COMMENT '工作地点',
    `degree_requirement` VARCHAR(32) DEFAULT NULL COMMENT '学历要求',
    `headcount`         INT          NOT NULL DEFAULT 1 COMMENT '招聘人数',
    `description`       MEDIUMTEXT   NOT NULL COMMENT '岗位职责描述（富文本HTML）',
    `requirement`       MEDIUMTEXT   NOT NULL COMMENT '任职要求（富文本HTML）',
    `tags`              VARCHAR(512) DEFAULT NULL COMMENT '岗位标签（JSON数组）',
    `deadline`          DATETIME     NOT NULL COMMENT '投递截止日期',
    `status`            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '岗位状态',
    `sort_order`        INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
    `view_count`        INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
    `apply_count`       INT          NOT NULL DEFAULT 0 COMMENT '投递数',
    `offline_reason`    VARCHAR(256) DEFAULT NULL COMMENT '下线原因',
    `owner_user_id`     BIGINT       DEFAULT NULL COMMENT '岗位负责人HR用户ID（数据范围权限锚点）',
    `create_by`         VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`         VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`          CHAR(1)      NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`job_id`),
    KEY `idx_department_status` (`department_id`, `status`),
    KEY `idx_category_status` (`category_id`, `status`),
    KEY `idx_status_deadline` (`status`, `deadline`),
    KEY `idx_location` (`location`),
    KEY `idx_sort_order` (`sort_order`),
    KEY `idx_del_flag` (`del_flag`),
    KEY `idx_owner_user_id` (`owner_user_id`),
    FULLTEXT INDEX `ft_job_search` (`title`, `description`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位主表 H-001';

-- ============================================
-- 10. 岗位模板表 H-003（P1，一期建表留空）
-- ============================================
CREATE TABLE `job_template` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_name`     VARCHAR(128) NOT NULL COMMENT '模板名称',
    `title`             VARCHAR(128) DEFAULT NULL COMMENT '岗位名称',
    `department_id`     BIGINT       DEFAULT NULL COMMENT '所属部门ID',
    `category_id`       BIGINT       DEFAULT NULL COMMENT '岗位类别ID',
    `location`          VARCHAR(128) DEFAULT NULL COMMENT '工作地点',
    `degree_requirement` VARCHAR(32) DEFAULT NULL COMMENT '学历要求',
    `headcount`         INT          DEFAULT 1 COMMENT '招聘人数',
    `description`       MEDIUMTEXT   DEFAULT NULL COMMENT '岗位职责描述',
    `requirement`       MEDIUMTEXT   DEFAULT NULL COMMENT '任职要求',
    `tags`              VARCHAR(512) DEFAULT NULL COMMENT '岗位标签',
    `create_by`         VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`         VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`          CHAR(1)      NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位模板表 H-003（P1预留，一期建表不实现业务逻辑）';

-- ============================================
-- 11. 投递记录表 S-013（核心表）
-- ============================================
CREATE TABLE `app_application` (
    `application_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '投递ID',
    `student_id`           BIGINT       NOT NULL COMMENT '学生ID',
    `job_id`               BIGINT       NOT NULL COMMENT '岗位ID',
    `status`               VARCHAR(32)  NOT NULL DEFAULT 'PENDING_SCREEN' COMMENT '投递状态',
    `source`               VARCHAR(32)  DEFAULT NULL COMMENT '渠道来源',
    `source_detail`        VARCHAR(256) DEFAULT NULL COMMENT '渠道详情',
    `current_snapshot_id`  BIGINT       DEFAULT NULL COMMENT '当前快照ID指针（对应app_snapshot.snapshot_id）',
    `apply_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
    `version_no`           INT          NOT NULL DEFAULT 1 COMMENT '投递版本号（乐观锁）',
    `snapshot_profile`     JSON         DEFAULT NULL COMMENT '基本资料快照',
    `snapshot_educations`  JSON         DEFAULT NULL COMMENT '教育经历快照',
    `snapshot_internships` JSON         DEFAULT NULL COMMENT '实习/项目经历快照',
    `snapshot_certificates` JSON        DEFAULT NULL COMMENT '技能/证书快照',
    `snapshot_activities`  JSON         DEFAULT NULL COMMENT '社团经历快照',
    `snapshot_resume_file` JSON         DEFAULT NULL COMMENT '简历附件快照',
    `data_retention_days`  INT          NOT NULL DEFAULT 730 COMMENT '数据保留天数',
    `auto_cleanup_date`    DATE         DEFAULT NULL COMMENT '自动清理日期',
    `snapshot_school`      VARCHAR(128) DEFAULT NULL COMMENT '筛选冗余-毕业院校（C-06裁决，与实体对齐）',
    `snapshot_major`       VARCHAR(128) DEFAULT NULL COMMENT '筛选冗余-专业',
    `snapshot_degree`      VARCHAR(64)  DEFAULT NULL COMMENT '筛选冗余-学历',
    `snapshot_name`        VARCHAR(64)  DEFAULT NULL COMMENT '筛选冗余-姓名',
    `allow_resubmit`       CHAR(1)      DEFAULT '0' COMMENT '允许撤回重投：0-否 1-是',
    `create_by`            VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`            VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`             CHAR(1)      NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`application_id`),
    UNIQUE KEY `uk_student_job` (`student_id`, `job_id`),
    KEY `idx_job_status` (`job_id`, `status`),
    KEY `idx_status_time` (`status`, `apply_time`),
    KEY `idx_student_status` (`student_id`, `status`),
    KEY `idx_source` (`source`),
    KEY `idx_cleanup_date` (`auto_cleanup_date`),
    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投递记录表 S-013（含简历快照，同岗位唯一约束防重复投递）';

-- ============================================
-- 12. 投递状态流转历史表（不可变日志，仅追加）
-- ============================================
CREATE TABLE `app_status_history` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '历史记录ID',
    `application_id` BIGINT       NOT NULL COMMENT '投递记录ID',
    `from_status`    VARCHAR(32)  DEFAULT NULL COMMENT '原状态',
    `to_status`      VARCHAR(32)  NOT NULL COMMENT '新状态',
    `operator_id`    BIGINT       DEFAULT NULL COMMENT '操作人ID',
    `operator_type`  VARCHAR(20)  NOT NULL COMMENT '操作人类型',
    `remark`         VARCHAR(512) DEFAULT NULL COMMENT '操作备注',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投递状态流转历史表（不可变日志，仅追加）';

-- ============================================
-- 13. HR 内部备注表 H-005
-- ============================================
CREATE TABLE `app_hr_note` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '备注ID',
    `application_id` BIGINT       NOT NULL COMMENT '投递记录ID',
    `hr_user_id`     BIGINT       NOT NULL COMMENT 'HR用户ID',
    `note_content`   TEXT         NOT NULL COMMENT '备注内容',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`),
    KEY `idx_hr_user_id` (`hr_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='HR内部备注表 H-005（仅HR可见，学生不可见）';

-- ============================================
-- 14. 面试安排表 H-008（P2预留，一期仅建表）
-- ============================================
CREATE TABLE `ivw_schedule` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '面试安排ID',
    `application_id`  BIGINT       NOT NULL COMMENT '投递记录ID',
    `interview_round` VARCHAR(32)  NOT NULL COMMENT '面试轮次',
    `interview_type`  VARCHAR(32)  NOT NULL COMMENT '面试方式',
    `interviewer_id`  BIGINT       NOT NULL COMMENT '面试官ID',
    `scheduled_time`  DATETIME     NOT NULL COMMENT '面试预定时间',
    `duration_minutes` INT         NOT NULL DEFAULT 60 COMMENT '面试时长（分钟）',
    `location`        VARCHAR(256) DEFAULT NULL COMMENT '面试地点',
    `meeting_link`    VARCHAR(512) DEFAULT NULL COMMENT '视频会议链接',
    `status`          VARCHAR(32)  NOT NULL DEFAULT 'SCHEDULED' COMMENT '状态',
    `remark`          VARCHAR(512) DEFAULT NULL COMMENT '面试备注',
    `create_by`       VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`        CHAR(1)      NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`),
    KEY `idx_interviewer_time` (`interviewer_id`, `scheduled_time`),
    KEY `idx_status_time` (`status`, `scheduled_time`),
    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试安排表 H-008（P2预留，一期不实现业务逻辑）';

-- ============================================
-- 15. 面试反馈表 H-009（P2预留，一期仅建表）
-- ============================================
CREATE TABLE `ivw_feedback` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
    `schedule_id`    BIGINT       NOT NULL COMMENT '面试安排ID',
    `interviewer_id` BIGINT       NOT NULL COMMENT '面试官ID',
    `scores`         JSON         DEFAULT NULL COMMENT '评分维度与得分',
    `overall_rating` TINYINT      DEFAULT NULL COMMENT '综合评价：1-5星',
    `comment`        TEXT         DEFAULT NULL COMMENT '面试评语',
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_schedule_id` (`schedule_id`),
    KEY `idx_interviewer_id` (`interviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试反馈表 H-009（P2预留，一期不实现业务逻辑）';

-- ============================================
-- 16. 站内消息表 S-015（P1预留，一期仅建表）
-- ============================================
CREATE TABLE `not_message` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `recipient_id`   BIGINT       NOT NULL COMMENT '接收人ID',
    `recipient_type` VARCHAR(16)  NOT NULL COMMENT '接收人类型：STUDENT/HR',
    `message_type`   VARCHAR(32)  NOT NULL COMMENT '消息类型',
    `title`          VARCHAR(256) NOT NULL COMMENT '消息标题',
    `content`        TEXT         NOT NULL COMMENT '消息正文',
    `ref_id`         BIGINT       DEFAULT NULL COMMENT '关联业务ID',
    `is_read`        CHAR(1)      NOT NULL DEFAULT '0' COMMENT '已读标记',
    `read_time`      DATETIME     DEFAULT NULL COMMENT '阅读时间',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_recipient_read` (`recipient_id`, `recipient_type`, `is_read`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内消息表 S-015（P1预留，一期不实现业务逻辑）';

-- ============================================
-- 17. 通知发送记录表 S-016（P1预留，一期仅建表）
-- ============================================
CREATE TABLE `not_record` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知记录ID',
    `message_id`     BIGINT       DEFAULT NULL COMMENT '关联站内消息ID',
    `recipient_id`   BIGINT       NOT NULL COMMENT '接收人ID',
    `recipient_type` VARCHAR(16)  NOT NULL COMMENT '接收人类型：STUDENT/HR',
    `channel`        VARCHAR(16)  NOT NULL COMMENT '通知渠道：SMS/EMAIL/WECHAT',
    `contact_info`   VARCHAR(128) NOT NULL COMMENT '联系方式',
    `template_code`  VARCHAR(64)  DEFAULT NULL COMMENT '模板编码',
    `content`        TEXT         DEFAULT NULL COMMENT '实际发送内容',
    `send_status`    VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '发送状态',
    `send_time`      DATETIME     DEFAULT NULL COMMENT '实际发送时间',
    `retry_count`    INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    `error_msg`      VARCHAR(512) DEFAULT NULL COMMENT '发送失败原因',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_message_id` (`message_id`),
    KEY `idx_recipient` (`recipient_id`, `recipient_type`),
    KEY `idx_send_status` (`send_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知发送记录表 S-016（P1预留，一期不实现业务逻辑）';

-- ============================================
-- 18. 品牌配置表 A-001
-- ============================================
CREATE TABLE `sys_brand_config` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_key`   VARCHAR(64)  NOT NULL COMMENT '配置键',
    `config_value` TEXT         DEFAULT NULL COMMENT '配置值',
    `config_type`  VARCHAR(32)  NOT NULL DEFAULT 'STRING' COMMENT '配置值类型',
    `config_group` VARCHAR(32)  NOT NULL DEFAULT 'GENERAL' COMMENT '配置分组',
    `description`  VARCHAR(256) DEFAULT NULL COMMENT '配置说明',
    `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='品牌配置表 A-001（区别于RuoYi的sys_config，专门存放门户展示类配置）';

-- ============================================
-- 19. Banner/公告表 A-001
-- ============================================
CREATE TABLE `sys_banner` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Banner ID',
    `title`       VARCHAR(128) NOT NULL COMMENT 'Banner标题/公告标题',
    `banner_type` VARCHAR(16)  NOT NULL DEFAULT 'IMAGE' COMMENT '类型：IMAGE/TEXT',
    `image_url`   VARCHAR(512) DEFAULT NULL COMMENT 'Banner图片URL',
    `link_url`    VARCHAR(512) DEFAULT NULL COMMENT '点击跳转链接',
    `content`     TEXT         DEFAULT NULL COMMENT '公告正文',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
    `is_active`   CHAR(1)      NOT NULL DEFAULT '1' COMMENT '启用标记',
    `start_time`  DATETIME     DEFAULT NULL COMMENT '展示开始时间',
    `end_time`    DATETIME     DEFAULT NULL COMMENT '展示结束时间',
    `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    CHAR(1)      NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_active_sort` (`is_active`, `sort_order`),
    KEY `idx_time_range` (`start_time`, `end_time`),
    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Banner/公告表 A-001';

-- ============================================
-- 20. 简历访问审计表（合规要求——个保法）
-- ============================================
CREATE TABLE `audit_resume_access` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '审计记录ID',
    `operator_id`        BIGINT       NOT NULL COMMENT '操作人ID',
    `operator_name`      VARCHAR(64)  NOT NULL COMMENT '操作人姓名',
    `target_student_id`  BIGINT       NOT NULL COMMENT '目标学生ID',
    `target_student_name` VARCHAR(64) NOT NULL COMMENT '目标学生姓名',
    `target_application_id` BIGINT    DEFAULT NULL COMMENT '关联投递记录ID',
    `operation_type`     VARCHAR(32)  NOT NULL COMMENT '操作类型',
    `operation_detail`   VARCHAR(512) DEFAULT NULL COMMENT '操作详情补充',
    `ip_address`         VARCHAR(64)  DEFAULT NULL COMMENT '操作IP地址',
    `user_agent`         VARCHAR(512) DEFAULT NULL COMMENT '浏览器User-Agent',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_operator_time` (`operator_id`, `create_time`),
    KEY `idx_target_student` (`target_student_id`, `create_time`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历访问审计表（个保法合规：记录所有对个人简历数据的查看/导出/删除操作）';

-- ============================================
-- 21. 学生 Refresh Token 表（C-04 裁决：落库可吊销）
-- ============================================
CREATE TABLE `stu_refresh_token` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Token ID',
    `student_id`  BIGINT       NOT NULL COMMENT '学生ID',
    `token_hash`  VARCHAR(128) NOT NULL COMMENT 'Token哈希值',
    `device_info` VARCHAR(256) DEFAULT NULL COMMENT '设备信息',
    `expire_time` DATETIME     NOT NULL COMMENT '过期时间',
    `rotated_at`  DATETIME     DEFAULT NULL COMMENT '上次刷新时间',
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/REVOKED/EXPIRED',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_token_hash` (`token_hash`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生Refresh Token表（C-04裁决：落库可吊销）';

-- ============================================
-- 22. 学生登录日志表
-- ============================================
CREATE TABLE `stu_login_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `student_id`   BIGINT       NOT NULL COMMENT '学生ID',
    `login_type`   VARCHAR(20)  NOT NULL DEFAULT 'PHONE' COMMENT '登录方式：PHONE/EMAIL/WECHAT',
    `login_ip`     VARCHAR(64)  DEFAULT NULL COMMENT '登录IP',
    `user_agent`   VARCHAR(512) DEFAULT NULL COMMENT '浏览器UA',
    `login_status` VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS' COMMENT '登录状态：SUCCESS/FAILED',
    `fail_reason`  VARCHAR(256) DEFAULT NULL COMMENT '失败原因',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_login_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生登录日志表';

-- ============================================
-- 23. 短信发送日志表
-- ============================================
CREATE TABLE `sms_send_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '短信记录ID',
    `phone`          VARCHAR(20)  NOT NULL COMMENT '接收手机号',
    `template_code`  VARCHAR(64)  NOT NULL COMMENT '短信模板编码',
    `content`        VARCHAR(512) DEFAULT NULL COMMENT '短信内容',
    `send_status`    VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '发送状态：PENDING/SUCCESS/FAILED',
    `provider_code`  VARCHAR(32)  DEFAULT NULL COMMENT '服务商返回码',
    `provider_msg`   VARCHAR(256) DEFAULT NULL COMMENT '服务商返回消息',
    `retry_count`    INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_phone` (`phone`),
    KEY `idx_send_status` (`send_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信发送日志表';

-- ============================================
-- [Docker/全新部署修复] RuoYi 体系基础表
-- 说明：init-data.sql 依赖以下表（种子数据插入），但原 DDL 一直未包含它们——
--       历史上是 RuoYi-Vue 脚手架建出的，本文件"全部自建表"的注释因此过时。
--       列为对齐 recrrut-system 实体 SysUser/SysDept/SysRole/SysMenu/
--       SysDictType/SysDictData 的字段（含逻辑删除 del_flag），
--       仅用于全新库初始化；已存在的库不受影响（本文件不会对其重跑）。
-- ============================================

-- R1. 系统用户表（HR 与管理员）
CREATE TABLE IF NOT EXISTS `sys_user` (
    `user_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `dept_id`      BIGINT       DEFAULT NULL COMMENT '部门ID',
    `user_name`    VARCHAR(30)  NOT NULL COMMENT '登录账号',
    `nick_name`    VARCHAR(30)  NOT NULL COMMENT '用户昵称',
    `user_type`    VARCHAR(2)   DEFAULT '00' COMMENT '用户类型：00-系统用户',
    `email`        VARCHAR(50)  DEFAULT '' COMMENT '邮箱',
    `phonenumber`  VARCHAR(11)  DEFAULT '' COMMENT '手机号',
    `sex`          CHAR(1)      DEFAULT '0' COMMENT '性别：0-男 1-女 2-未知',
    `avatar`       VARCHAR(100) DEFAULT '' COMMENT '头像路径',
    `password`     VARCHAR(100) DEFAULT '' COMMENT '密码（BCrypt）',
    `status`       CHAR(1)      DEFAULT '0' COMMENT '状态：0-正常 1-停用',
    `login_ip`     VARCHAR(128) DEFAULT '' COMMENT '最后登录IP',
    `login_date`   DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    `create_by`    VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time`  DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_by`    VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    `update_time`  DATETIME     DEFAULT NULL COMMENT '更新时间',
    `remark`       VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `del_flag`     CHAR(1)      DEFAULT '0' COMMENT '删除标志：0-存在 2-删除',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_user_name` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表（HR 与管理员）';

-- R2. 部门表
CREATE TABLE IF NOT EXISTS `sys_dept` (
    `dept_id`     BIGINT      NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    `parent_id`   BIGINT      DEFAULT 0 COMMENT '父部门ID',
    `ancestors`   VARCHAR(50) DEFAULT '' COMMENT '祖级列表',
    `dept_name`   VARCHAR(30) NOT NULL COMMENT '部门名称',
    `order_num`   INT         DEFAULT 0 COMMENT '显示顺序',
    `leader`      VARCHAR(20) DEFAULT NULL COMMENT '负责人',
    `phone`       VARCHAR(11) DEFAULT NULL COMMENT '联系电话',
    `email`       VARCHAR(50) DEFAULT NULL COMMENT '邮箱',
    `status`      CHAR(1)     DEFAULT '0' COMMENT '状态：0-正常 1-停用',
    `create_by`   VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME    DEFAULT NULL COMMENT '创建时间',
    `update_by`   VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME    DEFAULT NULL COMMENT '更新时间',
    `del_flag`    CHAR(1)     DEFAULT '0' COMMENT '删除标志：0-存在 2-删除',
    PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';

-- R3. 角色表（含菜单/部门树严格模式列，实体 SysRole 显式映射）
CREATE TABLE IF NOT EXISTS `sys_role` (
    `role_id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name`           VARCHAR(30)  NOT NULL COMMENT '角色名称',
    `role_key`            VARCHAR(100) NOT NULL DEFAULT '' COMMENT '角色权限字符串',
    `role_sort`           INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `data_scope`          CHAR(1)      DEFAULT '1' COMMENT '数据范围：1-全部 2-自定义 3-本部门 4-本部门及以下 5-仅本人',
    `menu_check_strictly` TINYINT(1)   DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
    `dept_check_strictly` TINYINT(1)   DEFAULT 1 COMMENT '部门树选择项是否关联显示',
    `status`              CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0-正常 1-停用',
    `create_by`           VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time`         DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_by`           VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    `update_time`         DATETIME     DEFAULT NULL COMMENT '更新时间',
    `remark`              VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `del_flag`            CHAR(1)      DEFAULT '0' COMMENT '删除标志：0-存在 2-删除',
    PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色表';

-- R4. 菜单表
CREATE TABLE IF NOT EXISTS `sys_menu` (
    `menu_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `menu_name`   VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    `parent_id`   BIGINT       DEFAULT 0 COMMENT '父菜单ID',
    `order_num`   INT          DEFAULT 0 COMMENT '显示顺序',
    `path`        VARCHAR(200) DEFAULT '' COMMENT '路由地址',
    `component`   VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    `query`       VARCHAR(255) DEFAULT NULL COMMENT '路由参数',
    `route_name`  VARCHAR(200) DEFAULT '' COMMENT '路由名称',
    `is_frame`    CHAR(1)      DEFAULT '1' COMMENT '是否外链：0-是 1-否',
    `is_cache`    CHAR(1)      DEFAULT '0' COMMENT '是否缓存：0-缓存 1-不缓存',
    `menu_type`   CHAR(1)      DEFAULT '' COMMENT '菜单类型：M-目录 C-菜单 F-按钮',
    `visible`     CHAR(1)      DEFAULT '0' COMMENT '显示状态：0-显示 1-隐藏',
    `status`      CHAR(1)      DEFAULT '0' COMMENT '菜单状态：0-正常 1-停用',
    `perms`       VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    `icon`        VARCHAR(100) DEFAULT '#' COMMENT '菜单图标',
    `create_by`   VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `del_flag`    CHAR(1)      DEFAULT '0' COMMENT '删除标志：0-存在 2-删除',
    PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统菜单表';

-- R5. 用户-角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户和角色关联表';

-- R6. 角色-菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和菜单关联表';

-- R7. 字典类型表
CREATE TABLE IF NOT EXISTS `sys_dict_type` (
    `dict_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典主键',
    `dict_name`   VARCHAR(100) DEFAULT '' COMMENT '字典名称',
    `dict_type`   VARCHAR(100) DEFAULT '' COMMENT '字典类型',
    `status`      CHAR(1)      DEFAULT '0' COMMENT '状态：0-正常 1-停用',
    `create_by`   VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `del_flag`    CHAR(1)      DEFAULT '0' COMMENT '删除标志：0-存在 2-删除',
    PRIMARY KEY (`dict_id`),
    UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';

-- R8. 字典数据表
CREATE TABLE IF NOT EXISTS `sys_dict_data` (
    `dict_code`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典编码',
    `dict_sort`   INT          DEFAULT 0 COMMENT '字典排序',
    `dict_label`  VARCHAR(100) DEFAULT '' COMMENT '字典标签',
    `dict_value`  VARCHAR(100) DEFAULT '' COMMENT '字典键值',
    `dict_type`   VARCHAR(100) DEFAULT '' COMMENT '字典类型',
    `css_class`   VARCHAR(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
    `list_class`  VARCHAR(100) DEFAULT NULL COMMENT '表格回显样式',
    `is_default`  CHAR(1)      DEFAULT 'N' COMMENT '是否默认：Y-是 N-否',
    `status`      CHAR(1)      DEFAULT '0' COMMENT '状态：0-正常 1-停用',
    `create_by`   VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `del_flag`    CHAR(1)      DEFAULT '0' COMMENT '删除标志：0-存在 2-删除',
    PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';

-- R9. 通知模板表（原文件仅在末尾"维护 SQL"段之后创建，导致维护 ALTER 在全新库上先于建表执行而报错；移至主建表区）
CREATE TABLE IF NOT EXISTS `notify_template` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_code` VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '模板编码',
    `template_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '模板名称',
    `channel`       VARCHAR(20)  NOT NULL DEFAULT 'IN_APP' COMMENT '通知渠道：IN_APP/SMS/EMAIL',
    `content`       TEXT         DEFAULT NULL COMMENT '模板内容',
    `status`        CHAR(1)      DEFAULT '0' COMMENT '状态：0-正常 1-停用',
    `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by`     VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`      CHAR(1)      DEFAULT '0' COMMENT '删除标志：0-存在 2-删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知模板表';

-- ============================================================================
-- 维护 SQL：对已存在数据库补加字段默认值（幂等，可反复执行）
-- 日期：2026-08-07
-- ============================================================================

-- BUG-0: stu_profile.real_name 注册时建空资料不填姓名，须允许为空
ALTER TABLE stu_profile MODIFY COLUMN real_name VARCHAR(64) DEFAULT NULL COMMENT '真实姓名';

-- BUG-1: job_category.category_code 新增时未提供默认值
ALTER TABLE job_category MODIFY COLUMN category_code VARCHAR(32) NOT NULL DEFAULT '';

-- BUG-2: notify_template.template_code 新增时未提供默认值
ALTER TABLE notify_template MODIFY COLUMN template_code VARCHAR(64) NOT NULL DEFAULT '';

-- BUG-2补充: notify_template.channel 新增时未提供默认值
ALTER TABLE notify_template MODIFY COLUMN channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP';

-- BUG-3: sys_role.role_key / role_sort / status 新增时无默认值
ALTER TABLE sys_role MODIFY COLUMN role_key VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE sys_role MODIFY COLUMN role_sort INT DEFAULT 0;
ALTER TABLE sys_role MODIFY COLUMN status CHAR(1) NOT NULL DEFAULT '0';

-- Extra: stu_resume_file.preview_status 确保默认值为 PENDING（建表已有，此处保底）
ALTER TABLE stu_resume_file MODIFY COLUMN preview_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- BUG-5: job_position 多个 NOT NULL 字段无默认值，未传值时 MySQL 抛异常
ALTER TABLE job_position MODIFY COLUMN location VARCHAR(128) NOT NULL DEFAULT '';
ALTER TABLE job_position MODIFY COLUMN description MEDIUMTEXT NOT NULL DEFAULT (_utf8mb4'');
ALTER TABLE job_position MODIFY COLUMN requirement MEDIUMTEXT NOT NULL DEFAULT (_utf8mb4'');
ALTER TABLE job_position MODIFY COLUMN department_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE job_position MODIFY COLUMN category_id BIGINT NOT NULL DEFAULT 0;
-- 注意: owner_user_id 列在建表DDL中不存在，若数据库中已手动添加此列，取消下面注释后执行
-- ALTER TABLE job_position MODIFY COLUMN owner_user_id BIGINT DEFAULT NULL;

-- ---[新增表]--------------------------------------------------------
-- notify_template 已上移至主建表区（R9），此处不再重复创建。
-- 历史原因：此表原在"维护 SQL"段之后创建，导致全新库上维护 ALTER 先于建表执行而报错。

-- ============================================================================
-- 补充: 新建环境须知
-- ============================================================================
-- 以下历史差异已在主建表区修正，全新库无需再手动执行：
--
-- 1. ✅ stu_user 主键 —— 原 DDL 定义 id，实体(Student.java)使用 @TableId student_id。
--    已在 CREATE TABLE 中直接使用 student_id。
--
-- 2. ✅ app_application 主键 —— 原 DDL 定义 id，实体(Application.java)使用 @TableId application_id。
--    已在 CREATE TABLE 中直接使用 application_id，并补齐实体新增列
--    （current_snapshot_id、snapshot_school/major/degree/name、allow_resubmit）。
--
-- 3. ✅ app_snapshot 表 —— 已在下方以 CREATE TABLE IF NOT EXISTS 建出（C-01裁决:快照外移为独立版本表）。
-- ============================================================================

-- ============================================
-- 24. 简历快照版本表（C-01裁决:独立版本表,不可变,仅追加）
-- ============================================
CREATE TABLE IF NOT EXISTS `app_snapshot` (
    `snapshot_id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '快照ID',
    `application_id`          BIGINT       NOT NULL COMMENT '关联投递记录ID',
    `student_id`              BIGINT       NOT NULL COMMENT '学生ID',
    `version_no`              INT          NOT NULL DEFAULT 1 COMMENT '快照版本号',
    `snapshot_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间',
    `snapshot_profile`        JSON         DEFAULT NULL COMMENT '基本资料快照JSON',
    `snapshot_educations`     JSON         DEFAULT NULL COMMENT '教育经历快照JSON',
    `snapshot_internships`    JSON         DEFAULT NULL COMMENT '实习/项目经历快照JSON',
    `snapshot_certificates`   JSON         DEFAULT NULL COMMENT '技能/证书快照JSON',
    `snapshot_activities`     JSON         DEFAULT NULL COMMENT '社团经历快照JSON',
    `snapshot_resume_file`    JSON         DEFAULT NULL COMMENT '简历附件快照JSON',
    PRIMARY KEY (`snapshot_id`),
    KEY `idx_application_id` (`application_id`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历快照版本表（C-01裁决：独立版本表，不可变，仅追加）';

-- ============================================================================
-- 网络管理模块（HR后台「网络管理」模块设计方案V1.0 第4.2节，阶段1）
-- 日期：2026-08-09
-- ============================================================================

-- ============================================
-- 25. CORS 白名单表（EXACT + CIDR 统一表，4.2.1节）
-- ============================================
CREATE TABLE `sys_cors_origin` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `rule_type`    VARCHAR(16)  NOT NULL COMMENT '规则类型：EXACT-精确Origin匹配, CIDR-局域网网段匹配',
    `rule_value`   VARCHAR(128) NOT NULL COMMENT 'EXACT时为完整Origin(scheme://host:port)；CIDR时为网段(如192.168.31.0/24)',
    `description`  VARCHAR(256) DEFAULT NULL COMMENT '规则说明（如"HR办公室WiFi网段"）',
    `is_active`    CHAR(1)      NOT NULL DEFAULT '1' COMMENT '启用标记：1-启用 0-停用（停用不删除，保留历史）',
    `is_builtin`   CHAR(1)      NOT NULL DEFAULT '0' COMMENT '是否内置默认规则：1-是（拒绝物理删除，只可禁用）',
    `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_value` (`rule_value`),
    KEY `idx_type_active` (`rule_type`, `is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='CORS动态白名单表（Origin精确匹配 + 局域网CIDR网段匹配统一存储，rule_type判别列，参照stu_internship的record_type判别列模式）';

-- ============================================
-- 26. 网络配置开关表（局域网访问开关等标量配置，4.2.2节）
-- ============================================
CREATE TABLE `sys_network_config` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_key`   VARCHAR(64)  NOT NULL COMMENT '配置键',
    `config_value` TEXT         DEFAULT NULL COMMENT '配置值',
    `config_type`  VARCHAR(32)  NOT NULL DEFAULT 'STRING' COMMENT '配置值类型：STRING/BOOLEAN/NUMBER',
    `config_group` VARCHAR(32)  NOT NULL DEFAULT 'NETWORK' COMMENT '配置分组',
    `description`  VARCHAR(256) DEFAULT NULL COMMENT '配置说明',
    `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='网络配置开关表（局域网访问开关等标量配置，独立于sys_brand_config——后者对匿名公网用户全表暴露，绝不能共用）';

-- ============================================
-- 27. 网络配置变更审计表（4.2.3节，表结构在阶段1建好，写入逻辑属阶段3）
-- ============================================
CREATE TABLE `audit_network_config` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '审计记录ID',
    `rule_id`          BIGINT       DEFAULT NULL COMMENT '关联规则ID（规则被删除后此字段仍保留历史值）',
    `config_table`     VARCHAR(32)  NOT NULL COMMENT '被修改的配置来源：CORS_ORIGIN-白名单条目, NETWORK_CONFIG-开关配置',
    `operation_type`   VARCHAR(32)  NOT NULL COMMENT '操作类型：ADD/UPDATE/DELETE/ENABLE/DISABLE/TOGGLE',
    `old_value`        VARCHAR(512) DEFAULT NULL COMMENT '变更前的值（JSON快照或简单值）',
    `new_value`        VARCHAR(512) DEFAULT NULL COMMENT '变更后的值',
    `operation_detail` VARCHAR(512) DEFAULT NULL COMMENT '操作详情补充',
    `operator_id`      BIGINT       NOT NULL COMMENT '操作人ID（关联sys_user.user_id）',
    `operator_name`    VARCHAR(64)  NOT NULL COMMENT '操作人姓名',
    `ip_address`       VARCHAR(64)  DEFAULT NULL COMMENT '操作来源IP地址',
    `user_agent`       VARCHAR(512) DEFAULT NULL COMMENT '浏览器User-Agent',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_operator_time` (`operator_id`, `create_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='网络配置变更审计表（不可变日志，仅追加，参照audit_resume_access结构范式并扩展old_value/new_value）';
