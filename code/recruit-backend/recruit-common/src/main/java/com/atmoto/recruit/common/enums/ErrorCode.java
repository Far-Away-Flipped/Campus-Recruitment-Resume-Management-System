package com.atmoto.recruit.common.enums;

import lombok.Getter;

/**
 * 业务错误码枚举
 * <p>
 * 分段规划：
 * 1xxxx 通用 | 2xxxx 认证与鉴权 | 3xxxx 学生 | 4xxxx 岗位 | 5xxxx 投递 | 6xxxx 文件 | 7xxxx 网络管理
 * </p>
 */
@Getter
public enum ErrorCode {

    // ────────── 1xxxx 通用 ──────────
    SUCCESS(200, "操作成功"),
    PARAM_INVALID(10001, "参数校验失败"),
    INTERNAL_ERROR(10002, "系统内部错误"),
    RESOURCE_NOT_FOUND(10003, "资源不存在"),
    RATE_LIMITED(10004, "操作过于频繁，请稍后再试"),
    REPEAT_SUBMIT(10005, "请勿重复提交"),

    // ────────── 2xxxx 认证与鉴权 ──────────
    UNAUTHORIZED(20001, "未登录或登录已过期，请重新登录"),
    LOGIN_FAILED(20002, "账号或密码错误"),
    NO_PERMISSION(20003, "无操作权限"),
    CAPTCHA_ERROR(20004, "验证码错误或已过期"),
    ACCOUNT_LOCKED(20005, "账号已被锁定，请15分钟后再试"),
    TOKEN_INVALID(20006, "Token无效或已过期"),
    TOKEN_REUSE_DETECTED(20007, "Token已被使用，请重新登录"),
    USER_DISABLED(20008, "账号已被停用，请联系管理员"),
    OLD_PASSWORD_INCORRECT(20009, "原密码不正确"),
    PASSWORD_TOO_WEAK(20010, "新密码强度不足（至少8位）"),
    HR_DIRECTOR_ROLE_REQUIRED(20011, "该操作仅限超级管理员执行"),

    // ────────── 3xxxx 学生 ──────────
    STUDENT_PHONE_EXISTS(30001, "手机号已注册"),
    STUDENT_PROFILE_INCOMPLETE(30002, "请先完善基本资料"),
    STUDENT_RESUME_MISSING(30003, "请先上传简历附件"),
    STUDENT_EDUCATION_MISSING(30004, "教育经历至少填写一条"),
    STUDENT_ACCOUNT_DISABLED(30005, "账号已被禁用"),

    // ────────── 4xxxx 岗位 ──────────
    JOB_NOT_FOUND(40001, "岗位不存在"),
    JOB_NOT_PUBLISHED(40002, "岗位未发布或已下架"),
    JOB_DEADLINE_EXPIRED(40003, "岗位已截止投递"),

    // ────────── 5xxxx 投递 ──────────
    APPLICATION_DUPLICATE(50001, "您已投递过该岗位"),
    APPLICATION_JOB_CLOSED(50002, "该岗位已停止招聘"),
    APPLICATION_BATCH_LIMIT(50003, "单次批量操作不能超过200条"),
    EXPORT_DAILY_LIMIT(50004, "今日导出已达上限（2000条），请明天再试"),

    // ────────── 6xxxx 文件 ──────────
    FILE_SIZE_EXCEEDED(60001, "文件大小不能超过10MB"),
    FILE_FORMAT_UNSUPPORTED(60002, "文件格式不支持，仅接受PDF和Word"),
    FILE_MAGIC_MISMATCH(60003, "文件内容与扩展名不匹配，上传被拒绝"),
    FILE_NOT_FOUND(60004, "文件不存在"),
    FILE_PREVIEW_PROCESSING(60005, "文件预览处理中，请稍后再试"),
    FILE_PREVIEW_FAILED(60006, "文件预览处理失败，请下载原件查看"),

    // ────────── 7xxxx 网络管理 ──────────
    NETWORK_ORIGIN_DUPLICATE(70001, "该Origin已存在于白名单中"),
    NETWORK_ORIGIN_WILDCARD_FORBIDDEN(70002, "CORS白名单不允许配置通配符Origin（*）。当前系统已启用凭证模式，根据浏览器CORS规范，通配符Origin与凭证模式不能同时生效，该组合会使白名单形同虚设"),
    NETWORK_ORIGIN_FORMAT_INVALID(70003, "Origin格式不正确，需为完整来源地址（如 https://campus.atmoto.cn），不能包含路径、查询参数或结尾斜杠"),
    NETWORK_ORIGIN_NOT_FOUND(70004, "该CORS白名单记录不存在或已被删除"),
    NETWORK_CIDR_FORMAT_INVALID(70005, "IP/网段格式不正确"),
    NETWORK_CIDR_RANGE_FORBIDDEN(70006, "网段范围不合法：仅允许RFC1918私有地址段（10.0.0.0/8、172.16.0.0/12、192.168.0.0/16），且前缀不得短于/16"),
    NETWORK_CIDR_DUPLICATE(70007, "该网段已存在于白名单中"),
    NETWORK_ADMIN_ROLE_REQUIRED(70008, "该操作仅限超级管理员执行，当前账号无修改权限"),
    NETWORK_BUILTIN_RULE_PROTECTED(70009, "内置规则不可删除，可禁用");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
