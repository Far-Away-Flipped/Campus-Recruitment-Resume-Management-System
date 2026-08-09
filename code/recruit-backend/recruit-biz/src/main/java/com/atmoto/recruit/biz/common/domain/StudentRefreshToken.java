package com.atmoto.recruit.biz.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生 Refresh Token（C-04 裁决：落库可吊销）
 */
@Data
@TableName("stu_refresh_token")
public class StudentRefreshToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private String tokenHash;
    private String deviceInfo;
    private LocalDateTime expireTime;
    private LocalDateTime rotatedAt;
    private String status;
    private LocalDateTime createTime;
}
