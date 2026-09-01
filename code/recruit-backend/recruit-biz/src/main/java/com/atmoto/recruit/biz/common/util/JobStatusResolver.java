package com.atmoto.recruit.biz.common.util;

import com.atmoto.recruit.biz.common.enums.JobStatus;

import java.time.LocalDateTime;

/**
 * 岗位状态解析器
 * <p>岗位过期状态为实时派生：status=PUBLISHED 且 deadline 早于当前时间时，展示为 EXPIRED。
 * 仅用于输出层覆盖，不持久化 EXPIRED（见 V6 迁移，存量 EXPIRED 已归一为 PUBLISHED）。</p>
 *
 * @author atmoto-recruit
 */
public final class JobStatusResolver {

    private JobStatusResolver() {
    }

    /**
     * 计算岗位的展示状态：已发布且已过截止日期 → EXPIRED，否则原样返回
     *
     * @param status   存库状态（PUBLISHED/DRAFT/CLOSED 等）
     * @param deadline 截止时间
     * @param now      当前后端时间
     */
    public static String resolveDisplayStatus(String status, LocalDateTime deadline, LocalDateTime now) {
        if (JobStatus.PUBLISHED.getCode().equals(status) && deadline != null && deadline.isBefore(now)) {
            return JobStatus.EXPIRED.getCode();
        }
        return status;
    }
}
