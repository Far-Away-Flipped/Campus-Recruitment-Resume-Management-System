package com.atmoto.recruit.biz.common.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学生技能/证书（S-008）
 * <p>certType: SKILL-技能, CERT-证书, LANGUAGE-语言能力</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stu_certificate")
public class StudentCertificate extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    @TableField("cert_type")
    private String certType;

    @TableField("cert_name")
    private String certName;

    @TableField("cert_level")
    private String certLevel;

    private String description;

    private Integer sortOrder;
}
