package com.atmoto.recruit.biz.portal.controller;

import com.atmoto.recruit.biz.common.domain.NotMessage;
import com.atmoto.recruit.biz.common.mapper.NotMessageMapper;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.framework.security.context.PortalUserHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生端站内消息 Controller（S-015 P1）
 * <p>提供学生端消息列表查询与已读标记功能。
 * 铁律：studentId 从 PortalUserHolder 获取，绝不从请求参数传入。</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal/messages")
public class PortalMessageController {

    private final NotMessageMapper notMessageMapper;

    /**
     * 获取当前学生的消息列表
     * <p>按发送时间降序排列，仅返回站内信。</p>
     */
    @GetMapping
    public AjaxResult getMessages() {
        Long studentId = PortalUserHolder.get();
        List<NotMessage> msgs = notMessageMapper.selectList(
                new LambdaQueryWrapper<NotMessage>()
                        .eq(NotMessage::getRecipientId, studentId)
                        .eq(NotMessage::getRecipientType, "STUDENT")
                        .orderByDesc(NotMessage::getCreateTime));
        return AjaxResult.success(msgs);
    }

    /**
     * 标记消息为已读
     * <p>仅操作属于当前学生的消息，防止越权。</p>
     *
     * @param id 消息ID
     */
    @PutMapping("/{id}/read")
    public AjaxResult markRead(@PathVariable Long id) {
        Long studentId = PortalUserHolder.get();
        notMessageMapper.update(null,
                new LambdaUpdateWrapper<NotMessage>()
                        .eq(NotMessage::getId, id)
                        .eq(NotMessage::getRecipientId, studentId)
                        .eq(NotMessage::getRecipientType, "STUDENT")
                        .set(NotMessage::getIsRead, "1")
                        .set(NotMessage::getReadTime, LocalDateTime.now()));
        return AjaxResult.success("已标记为已读");
    }
}
