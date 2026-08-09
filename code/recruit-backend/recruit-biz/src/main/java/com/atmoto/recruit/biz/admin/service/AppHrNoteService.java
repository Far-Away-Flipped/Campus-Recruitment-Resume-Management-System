package com.atmoto.recruit.biz.admin.service;

import com.atmoto.recruit.biz.admin.dto.RemarkDTO;
import com.atmoto.recruit.biz.admin.vo.HrNoteVO;

import java.util.List;

/**
 * HR内部备注服务接口
 *
 * @author atmoto-recruit
 */
public interface AppHrNoteService {

    /**
     * 添加HR内部备注
     *
     * @param applicationId 投递记录ID
     * @param remarkDTO     备注内容
     * @param operatorUserId 操作HR的用户ID
     * @param operatorName   操作HR的用户名
     */
    void addRemark(Long applicationId, RemarkDTO remarkDTO, Long operatorUserId, String operatorName);

    /**
     * 查看备注列表
     *
     * @param applicationId 投递记录ID
     * @return 备注列表
     */
    List<HrNoteVO> listRemarks(Long applicationId);
}
