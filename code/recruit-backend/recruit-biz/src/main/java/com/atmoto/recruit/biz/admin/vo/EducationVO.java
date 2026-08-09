package com.atmoto.recruit.biz.admin.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 教育经历VO
 *
 * @author atmoto-recruit
 */
@Data
public class EducationVO {

    private Long id;
    private String schoolName;
    private String major;
    private String degree;
    private LocalDate startDate;
    private LocalDate endDate;
    private String gpa;
}
