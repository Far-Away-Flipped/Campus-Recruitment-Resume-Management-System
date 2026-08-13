package com.atmoto.recruit.biz.admin.service.impl;

import com.atmoto.recruit.biz.admin.service.ResumeActionService;
import com.atmoto.recruit.biz.admin.vo.BatchResultVO;
import com.atmoto.recruit.biz.common.domain.AppStatusHistory;
import com.atmoto.recruit.biz.common.domain.Application;
import com.atmoto.recruit.biz.common.enums.ApplicationStatus;
import com.atmoto.recruit.biz.common.mapper.AppStatusHistoryMapper;
import com.atmoto.recruit.biz.common.mapper.ApplicationMapper;
import com.atmoto.recruit.biz.notify.NotifyService;
import com.atmoto.recruit.common.constant.BizConstants;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 简历操作服务实现
 * <p>HR简历筛选（通过/淘汰）逻辑，含乐观锁、状态校验、历史记录</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeActionServiceImpl implements ResumeActionService {

    private final ApplicationMapper applicationMapper;
    private final AppStatusHistoryMapper appStatusHistoryMapper;
    private final NotifyService notifyService;

    /** 状态变更操作类型常量 */
    private static final String OPERATOR_TYPE_HR = "HR";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void screenPass(Long applicationId, Long operatorUserId) {
        doScreen(applicationId, operatorUserId, ApplicationStatus.PENDING_SCREEN,
                ApplicationStatus.SCREEN_PASSED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void screenEliminate(Long applicationId, Long operatorUserId) {
        doScreen(applicationId, operatorUserId, ApplicationStatus.PENDING_SCREEN,
                ApplicationStatus.ELIMINATED);
    }

    @Override
    public List<BatchResultVO> batchScreen(List<Long> applicationIds, String action,
                                           Long operatorUserId, boolean hasAllDataScope) {
        // 数量上限校验
        if (applicationIds == null || applicationIds.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请选择要操作的简历");
        }
        if (applicationIds.size() > BizConstants.BATCH_OPERATION_LIMIT) {
            throw new BizException(ErrorCode.APPLICATION_BATCH_LIMIT);
        }

        // 操作类型参数校验
        if (!"pass".equals(action) && !"eliminate".equals(action)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "无效的操作类型，仅支持 pass 或 eliminate");
        }

        // 目标状态
        ApplicationStatus targetStatus = "pass".equals(action)
                ? ApplicationStatus.SCREEN_PASSED : ApplicationStatus.ELIMINATED;

        // 数据范围约束
        Long ownerUserId = hasAllDataScope ? null : operatorUserId;

        List<BatchResultVO> results = new ArrayList<>();

        for (Long applicationId : applicationIds) {
            try {
                // 先检查归属权限（含数据范围校验）
                Application app = applicationMapper.selectByIdWithScope(applicationId, ownerUserId);
                if (app == null) {
                    results.add(new BatchResultVO(applicationId, false, "投递记录不存在或无权限操作"));
                    continue;
                }

                doScreen(applicationId, operatorUserId, ApplicationStatus.PENDING_SCREEN, targetStatus);
                results.add(new BatchResultVO(applicationId, true, null));
            } catch (BizException e) {
                results.add(new BatchResultVO(applicationId, false, e.getMessage()));
            } catch (Exception e) {
                log.error("批量筛选异常：applicationId={}, action={}", applicationId, action, e);
                results.add(new BatchResultVO(applicationId, false, "系统异常：" + e.getMessage()));
            }
        }

        return results;
    }

    /**
     * 执行筛选操作（核心逻辑）
     * <p>
     * 乐观锁：WHERE application_id=? AND version=? AND status=?
     * 更新：status=新状态, version=version+1
     * 日志：INSERT app_status_history
     * </p>
     *
     * @param applicationId  投递记录ID
     * @param operatorUserId 操作HR的用户ID
     * @param fromStatus     期望的当前状态
     * @param toStatus       目标状态
     */
    private void doScreen(Long applicationId, Long operatorUserId,
                          ApplicationStatus fromStatus, ApplicationStatus toStatus) {
        // 1. 先查询当前记录
        Application app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "投递记录不存在");
        }

        // 2. 状态校验：当前必须是 PENDING_SCREEN
        if (!fromStatus.getCode().equals(app.getStatus())) {
            log.warn("状态不匹配：applicationId={}, 当前状态={}, 期望状态={}",
                    applicationId, app.getStatus(), fromStatus.getCode());
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "该简历当前状态不允许此操作（当前：" +
                            ApplicationStatus.fromCode(app.getStatus()).getLabel() +
                            "，仅待筛选状态可操作）");
        }

        // 3. 乐观锁更新：WHERE application_id=? AND version=? AND status=?
        Integer currentVersion = app.getVersion();
        LambdaUpdateWrapper<Application> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Application::getApplicationId, applicationId)
                .eq(Application::getVersion, currentVersion)
                .eq(Application::getStatus, fromStatus.getCode())
                .set(Application::getStatus, toStatus.getCode())
                .set(Application::getVersion, currentVersion != null ? currentVersion + 1 : 1);

        int affectedRows = applicationMapper.update(null, updateWrapper);
        if (affectedRows == 0) {
            log.warn("乐观锁冲突：applicationId={}, version={}", applicationId, currentVersion);
            throw new BizException(ErrorCode.REPEAT_SUBMIT,
                    "该简历已被其他同事操作，请刷新后重试");
        }

        // 4. 记录状态变更历史
        AppStatusHistory history = new AppStatusHistory();
        history.setApplicationId(applicationId);
        history.setFromStatus(fromStatus.getCode());
        history.setToStatus(toStatus.getCode());
        history.setOperatorType(OPERATOR_TYPE_HR);
        history.setOperatorId(operatorUserId);
        history.setRemark(String.format("HR筛选：%s → %s",
                fromStatus.getLabel(), toStatus.getLabel()));
        history.setOperateTime(LocalDateTime.now());
        appStatusHistoryMapper.insert(history);

        log.info("简历筛选操作成功：applicationId={}, from={}, to={}, operator={}",
                applicationId, fromStatus.getCode(), toStatus.getCode(), operatorUserId);

        // 5. 同步发送状态变更站内信通知学生（尽力而为，失败不影响主业务）
        try {
            notifyService.sendStatusChangeNotice(app.getStudentId(), applicationId,
                    app.getJobId(), toStatus.getLabel(), history.getId());
        } catch (Exception e) {
            log.error("发送状态变更通知失败：applicationId={}, studentId={}, historyId={}",
                    applicationId, app.getStudentId(), history.getId(), e);
        }
    }
}
