package com.atmoto.recruit.biz.common.enums;

import lombok.Getter;

/**
 * 简历来源渠道（一期预留字段，二期实现渠道分析）
 */
@Getter
public enum SourceChannel {

    OFFICIAL_SITE("OFFICIAL_SITE", "官网"),
    CAMPUS_TALK("CAMPUS_TALK", "宣讲会"),
    REFERRAL("REFERRAL", "内推"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String label;

    SourceChannel(String code, String label) { this.code = code; this.label = label; }
}
