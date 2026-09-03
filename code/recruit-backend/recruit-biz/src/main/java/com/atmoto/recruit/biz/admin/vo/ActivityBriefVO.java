package com.atmoto.recruit.biz.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 社团/校园经历 VO
 * <p>用于简历详情页中的社团经历子列表展示（数据来自投递快照 snapshot_activities，
 * 字段名与快照 JSON key 一致以便直接反序列化）</p>
 *
 * @author atmoto-recruit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityBriefVO {

    /** 记录ID */
    private Long id;

    /** 社团/组织名称 */
    private String orgName;

    /** 担任职务 */
    private String position;

    /** 主要职责及业绩 */
    private String description;
}
