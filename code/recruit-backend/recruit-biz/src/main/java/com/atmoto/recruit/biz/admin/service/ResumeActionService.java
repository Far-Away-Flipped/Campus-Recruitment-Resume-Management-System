package com.atmoto.recruit.biz.admin.service;

import com.atmoto.recruit.biz.admin.vo.BatchResultVO;

import java.util.List;

/**
 * 简历操作服务接口
 * <p>HR对投递记录的单人筛选（通过/淘汰）和批量筛选操作</p>
 *
 * @author atmoto-recruit
 */
public interface ResumeActionService {

    /**
     * 单人筛选通过
     * <p>
     * 乐观锁：WHERE application_id=? AND version=?
     * 当前状态必须是 PENDING_SCREEN
     * 更新 status=SCREEN_PASSED, version+1
     * 记录状态变更历史
     * </p>
     *
     * @param applicationId 投递记录ID
     * @param operatorUserId 操作HR的用户ID
     */
    void screenPass(Long applicationId, Long operatorUserId);

    /**
     * 单人筛选淘汰
     * <p>逻辑同 screenPass，目标状态为 ELIMINATED</p>
     *
     * @param applicationId 投递记录ID
     * @param operatorUserId 操作HR的用户ID
     */
    void screenEliminate(Long applicationId, Long operatorUserId);

    /**
     * 批量筛选
     * <p>
     * 循环调单人筛选方法，部分失败时成功的提交、失败的返回原因清单。
     * 总数上限为 BizConstants.BATCH_OPERATION_LIMIT（200）。
     * 每个 applicationId 必须属于当前HR可见范围。
     * </p>
     *
     * @param applicationIds 投递记录ID列表
     * @param action         操作类型：pass（通过） / eliminate（淘汰）
     * @param operatorUserId 操作HR的用户ID
     * @param hasAllDataScope 是否拥有全部数据权限
     * @return 批量操作结果列表
     */
    List<BatchResultVO> batchScreen(List<Long> applicationIds, String action,
                                    Long operatorUserId, boolean hasAllDataScope);
}
