package com.atmoto.recruit.biz.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 技能/证书/语言能力 VO
 * <p>用于简历详情页中的技能证书子列表展示（数据来自投递快照 snapshot_certificates，
 * 字段名与快照 JSON key 一致以便直接反序列化）</p>
 *
 * @author atmoto-recruit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateBriefVO {

    /** 记录ID */
    private Long id;

    /** 证书类型：CERT-证书, SKILL-技能, LANGUAGE-语言能力 */
    private String certType;

    /** 证书类型中文标签 */
    private String certTypeLabel;

    /** 名称 */
    private String certName;

    /** 等级/分数 */
    private String certLevel;

    /** 补充说明 */
    private String description;
}
