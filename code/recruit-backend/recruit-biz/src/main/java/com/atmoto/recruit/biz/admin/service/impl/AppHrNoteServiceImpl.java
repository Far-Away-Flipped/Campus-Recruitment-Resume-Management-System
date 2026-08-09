package com.atmoto.recruit.biz.admin.service.impl;

import com.atmoto.recruit.biz.admin.dto.RemarkDTO;
import com.atmoto.recruit.biz.admin.service.AppHrNoteService;
import com.atmoto.recruit.biz.admin.vo.HrNoteVO;
import com.atmoto.recruit.biz.common.domain.AppHrNote;
import com.atmoto.recruit.biz.common.domain.Application;
import com.atmoto.recruit.biz.common.mapper.AppHrNoteMapper;
import com.atmoto.recruit.biz.common.mapper.ApplicationMapper;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HR内部备注服务实现
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppHrNoteServiceImpl implements AppHrNoteService {

    private final AppHrNoteMapper appHrNoteMapper;
    private final ApplicationMapper applicationMapper;

    @Override
    public void addRemark(Long applicationId, RemarkDTO remarkDTO, Long operatorUserId, String operatorName) {
        // 参数校验
        if (remarkDTO == null || remarkDTO.getNoteContent() == null || remarkDTO.getNoteContent().trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "备注内容不能为空");
        }

        // 校验投递记录存在
        Application app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "投递记录不存在");
        }

        // 创建备注记录
        AppHrNote note = new AppHrNote();
        note.setApplicationId(applicationId);
        note.setContent(HtmlUtils.htmlEscape(remarkDTO.getNoteContent().trim()));
        note.setOperatorId(operatorUserId);
        note.setOperatorName(operatorName);
        note.setCreateTime(LocalDateTime.now());

        appHrNoteMapper.insert(note);
        log.info("HR添加备注：applicationId={}, operatorId={}, contentLength={}",
                applicationId, operatorUserId, remarkDTO.getNoteContent().length());
    }

    @Override
    public List<HrNoteVO> listRemarks(Long applicationId) {
        // 校验投递记录存在
        Application app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "投递记录不存在");
        }

        // 按创建时间升序查询备注
        List<AppHrNote> notes = appHrNoteMapper.selectList(
                new LambdaQueryWrapper<AppHrNote>()
                        .eq(AppHrNote::getApplicationId, applicationId)
                        .orderByAsc(AppHrNote::getCreateTime));

        if (notes == null || notes.isEmpty()) {
            return List.of();
        }

        return notes.stream().map(n -> {
            HrNoteVO vo = new HrNoteVO();
            vo.setId(n.getId());
            vo.setApplicationId(n.getApplicationId());
            vo.setContent(n.getContent());
            vo.setOperatorId(n.getOperatorId());
            vo.setOperatorName(n.getOperatorName());
            vo.setCreateTime(n.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }
}
