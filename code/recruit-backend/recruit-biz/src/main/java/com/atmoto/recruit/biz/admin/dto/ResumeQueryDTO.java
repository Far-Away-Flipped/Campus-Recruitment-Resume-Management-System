package com.atmoto.recruit.biz.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历多维筛选查询DTO
 * <p>支持按岗位、状态、关键字、学校、专业、学历、投递时间范围筛选</p>
 *
 * @author atmoto-recruit
 */
@Data
public class ResumeQueryDTO {

    /** 岗位ID（精确筛选） */
    private Long jobId;

    /** 投递状态 */
    private String status;

    /** 关键字（模糊匹配姓名或学校） */
    private String keyword;

    /** 学校名称筛选（模糊匹配冗余列 snapshotSchool） */
    private String school;

    /** 专业筛选（模糊匹配冗余列 snapshotMajor） */
    private String major;

    /** 学历筛选（精确匹配冗余列 snapshotDegree） */
    private String degree;

    /** 投递时间起始 */
    private LocalDateTime applyTimeStart;

    /** 投递时间截止 */
    private LocalDateTime applyTimeEnd;
}
