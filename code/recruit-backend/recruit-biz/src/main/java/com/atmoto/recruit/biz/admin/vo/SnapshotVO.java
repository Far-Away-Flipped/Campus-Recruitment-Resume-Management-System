package com.atmoto.recruit.biz.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 快照版本VO
 *
 * @author atmoto-recruit
 */
@Data
public class SnapshotVO {

    private Long snapshotId;
    private Integer versionNo;
    private LocalDateTime snapshotTime;

    /** 快照profile数据（JSON反序列化后的结构化数据） */
    private Object snapshotProfile;

    /** 快照教育经历数据 */
    private Object snapshotEducations;

    /** 快照简历文件数据 */
    private Object snapshotResumeFile;
}
