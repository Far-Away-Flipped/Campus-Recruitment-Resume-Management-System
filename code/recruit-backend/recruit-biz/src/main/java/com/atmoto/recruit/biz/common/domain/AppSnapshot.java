package com.atmoto.recruit.biz.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历快照版本表（C-01裁决：外移为独立版本表）
 * <p>不继承 BaseEntity，快照不可变，保留完整版本历史</p>
 */
@Data
@TableName("app_snapshot")
public class AppSnapshot {

    @TableId(type = IdType.AUTO)
    private Long snapshotId;

    private Long applicationId;
    private Long studentId;
    private Integer versionNo;
    private LocalDateTime snapshotTime;

    /** 快照数据（JSON，约3KB/条） */
    private String snapshotProfile;
    private String snapshotEducations;
    private String snapshotInternships;
    private String snapshotCertificates;
    private String snapshotActivities;
    private String snapshotResumeFile;
}
