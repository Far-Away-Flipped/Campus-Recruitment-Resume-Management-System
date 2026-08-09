package com.atmoto.recruit.biz.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 实习/项目经历VO
 * <p>用于学生详情页中的实习/项目经历子列表展示</p>
 *
 * @author atmoto-recruit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternshipBriefVO {

    /** 经历ID */
    private Long id;

    /** 记录类型：I-实习经历, P-项目经历 */
    private String recordType;

    /** 公司/项目名称 */
    private String orgName;

    /** 岗位/角色 */
    private String position;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** 描述 */
    private String description;
}
