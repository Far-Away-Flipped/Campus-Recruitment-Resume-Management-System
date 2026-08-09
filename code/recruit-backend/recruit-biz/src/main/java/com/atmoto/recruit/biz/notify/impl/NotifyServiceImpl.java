package com.atmoto.recruit.biz.notify.impl;

import com.atmoto.recruit.biz.common.domain.JobPosition;
import com.atmoto.recruit.biz.common.domain.NotMessage;
import com.atmoto.recruit.biz.common.mapper.JobPositionMapper;
import com.atmoto.recruit.biz.common.mapper.NotMessageMapper;
import com.atmoto.recruit.biz.notify.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 站内信通知 Service 实现（S-015 P1）
 * <p>将站内信消息写入 not_message 表，支持学生端和HR端查询与已读标记。</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final JobPositionMapper jobPositionMapper;
    private final NotMessageMapper notMessageMapper;

    @Async
    @Override
    public void sendInAppMessage(Long receiverStudentId, String title, String content) {
        NotMessage msg = new NotMessage();
        msg.setRecipientId(receiverStudentId);
        msg.setRecipientType("STUDENT");
        msg.setTitle(title);
        msg.setContent(content);
        msg.setMessageType("APPLY_SUCCESS");
        msg.setIsRead("0");
        msg.setCreateTime(LocalDateTime.now());
        notMessageMapper.insert(msg);
        log.info("【站内信-学生】接收人studentId={}, 标题={}, 消息ID={}",
                receiverStudentId, title, msg.getId());
    }

    @Async
    @Override
    public void sendHrNotification(Long jobId, String title, String content) {
        // 查询岗位负责人
        JobPosition job = jobPositionMapper.selectById(jobId);
        if (job == null || job.getOwnerUserId() == null) {
            log.warn("【HR通知】岗位{}无负责人，跳过通知。标题={}", jobId, title);
            return;
        }
        Long ownerUserId = job.getOwnerUserId();

        NotMessage msg = new NotMessage();
        msg.setRecipientId(ownerUserId);
        msg.setRecipientType("HR");
        msg.setTitle(title);
        msg.setContent(content);
        msg.setMessageType("NEW_APPLICATION");
        msg.setIsRead("0");
        msg.setRefId(jobId);
        msg.setCreateTime(LocalDateTime.now());
        notMessageMapper.insert(msg);
        log.info("【站内信-HR】接收人ownerUserId={}, 岗位jobId={}, 标题={}, 消息ID={}",
                ownerUserId, jobId, title, msg.getId());
    }
}
