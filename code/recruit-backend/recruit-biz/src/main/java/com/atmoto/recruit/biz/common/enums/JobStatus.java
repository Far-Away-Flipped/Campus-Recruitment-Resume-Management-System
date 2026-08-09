package com.atmoto.recruit.biz.common.enums;

import lombok.Getter;

/** 岗位状态 */
@Getter
public enum JobStatus {

    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    CLOSED("CLOSED", "已下架"),
    EXPIRED("EXPIRED", "已到期");

    private final String code;
    private final String label;

    JobStatus(String code, String label) { this.code = code; this.label = label; }
}
