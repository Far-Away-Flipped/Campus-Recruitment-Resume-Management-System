package com.atmoto.recruit.biz.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历访问审计记录（合规要求——个保法）
 */
@Data
@TableName("audit_resume_access")
public class AuditResumeAccess {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;
    private String operatorName;
    private Long targetStudentId;
    private String targetStudentName;
    private Long targetApplicationId;
    private String operationType;
    private String operationDetail;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createTime;
}
