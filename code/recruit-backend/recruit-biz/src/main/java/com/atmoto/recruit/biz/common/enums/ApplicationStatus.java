package com.atmoto.recruit.biz.common.enums;

import lombok.Getter;

/**
 * 候选人投递状态枚举
 * <p>
 * 一期（P0）仅开启：PENDING_SCREEN → SCREEN_PASSED / ELIMINATED。
 * P2 状态值预置在枚举与字典表中，不开放流转。
 * DB 默认值：PENDING_SCREEN
 * </p>
 */
@Getter
public enum ApplicationStatus {

    PENDING_SCREEN("PENDING_SCREEN", "待筛选", false, true),
    SCREEN_PASSED("SCREEN_PASSED", "筛选通过", false, true),
    ELIMINATED("ELIMINATED", "已淘汰", true, true),

    // ── P2 预留 ──
    PENDING_INTERVIEW("PENDING_INTERVIEW", "待面试", false, false),
    IN_INTERVIEW("IN_INTERVIEW", "面试中", false, false),
    INTERVIEW_PASSED("INTERVIEW_PASSED", "面试通过", false, false),
    PENDING_OFFER("PENDING_OFFER", "待录用", false, false),
    OFFER_SENT("OFFER_SENT", "已发Offer", false, false),
    ACCEPTED("ACCEPTED", "已接受", false, false),
    REJECTED("REJECTED", "已拒绝", true, false),
    ONBOARDED("ONBOARDED", "已入职", true, false);

    /** 英文码（API 传输用） */
    private final String code;
    /** 中文标签 */
    private final String label;
    /** 是否终态 */
    private final boolean terminal;
    /** 一期（P0）是否启用 */
    private final boolean p0Active;

    ApplicationStatus(String code, String label, boolean terminal, boolean p0Active) {
        this.code = code;
        this.label = label;
        this.terminal = terminal;
        this.p0Active = p0Active;
    }

    /** 根据英文码查枚举 */
    public static ApplicationStatus fromCode(String code) {
        for (ApplicationStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("未知状态码：" + code);
    }
}
