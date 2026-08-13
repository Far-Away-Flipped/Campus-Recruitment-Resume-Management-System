package com.atmoto.recruit.biz.notify;

/**
 * 站内信通知 Service 接口（P0 核心通知）
 * <p>提供给学生和HR的消息通知能力。开发环境使用 log.info 打印，生产环境对接真实推送通道。</p>
 *
 * @author atmoto-recruit
 */
public interface NotifyService {

    /**
     * 给学生发送站内信
     * <p>开发环境：log.info 打印消息。生产环境：写入消息表或推送WebSocket。</p>
     *
     * @param receiverStudentId 接收学生ID
     * @param title             消息标题
     * @param content           消息内容
     */
    void sendInAppMessage(Long receiverStudentId, String title, String content);

    /**
     * 给岗位负责人HR发送通知
     * <p>根据岗位ID查询 ownerUserId，向对应HR发送新投递提醒。
     * 开发环境：log.info 打印消息。</p>
     *
     * @param jobId   岗位ID（用于查询负责人）
     * @param title   消息标题
     * @param content 消息内容
     */
    void sendHrNotification(Long jobId, String title, String content);

    /**
     * 发送简历状态变更站内信（同步）
     * <p>HR 更新投递状态后，向学生推送「投递进度更新」通知。同步发送 + dedup_key 幂等防重。</p>
     *
     * @param receiverStudentId 接收学生ID
     * @param applicationId     投递记录ID（ref_id）
     * @param jobId             岗位ID（用于查询岗位名）
     * @param statusLabel       目标状态中文标签
     * @param historyId         状态变更历史ID（用于 dedup_key 幂等）
     */
    void sendStatusChangeNotice(Long receiverStudentId, Long applicationId,
                                Long jobId, String statusLabel, Long historyId);
}
