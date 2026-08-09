package com.atmoto.recruit.biz.common.enums;

import lombok.Getter;

/** 附件预览转换状态 */
@Getter
public enum ConvertStatus {

    PENDING("PENDING", "待转换"),
    CONVERTING("CONVERTING", "转换中"),
    SUCCESS("SUCCESS", "转换成功"),
    FAILED("FAILED", "转换失败");

    private final String code;
    private final String label;

    ConvertStatus(String code, String label) { this.code = code; this.label = label; }
}
