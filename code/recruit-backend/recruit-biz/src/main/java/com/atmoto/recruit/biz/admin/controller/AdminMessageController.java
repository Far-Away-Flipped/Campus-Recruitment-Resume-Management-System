package com.atmoto.recruit.biz.admin.controller;

import com.atmoto.recruit.biz.common.domain.NotMessage;
import com.atmoto.recruit.biz.common.mapper.NotMessageMapper;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.framework.security.context.AdminUserHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HR端站内消息 Controller（S-015 P1）
 * <p>提供HR端消息列表查询与已读标记功能。</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/messages")
public class AdminMessageController {

    private final NotMessageMapper notMessageMapper;

    /**
     * 获取当前HR的消息列表
     * <p>按发送时间降序排列，仅返回站内信。</p>
     */
    @GetMapping
    public AjaxResult getMessages() {
        Long userId = AdminUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        List<NotMessage> msgs = notMessageMapper.selectList(
                new LambdaQueryWrapper<NotMessage>()
                        .eq(NotMessage::getRecipientId, userId)
                        .eq(NotMessage::getRecipientType, "HR")
                        .orderByDesc(NotMessage::getCreateTime));
        return AjaxResult.success(msgs);
    }

    /**
     * 标记消息为已读
     * <p>仅操作属于当前HR的消息，防止越权。</p>
     *
     * @param id 消息ID
     */
    @PutMapping("/{id}/read")
    public AjaxResult markRead(@PathVariable Long id) {
        Long userId = AdminUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        notMessageMapper.update(null,
                new LambdaUpdateWrapper<NotMessage>()
                        .eq(NotMessage::getId, id)
                        .eq(NotMessage::getRecipientId, userId)
                        .eq(NotMessage::getRecipientType, "HR")
                        .set(NotMessage::getIsRead, "1")
                        .set(NotMessage::getReadTime, LocalDateTime.now()));
        return AjaxResult.success("已标记为已读");
    }
}
