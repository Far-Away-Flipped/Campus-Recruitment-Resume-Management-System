package com.atmoto.recruit.common.constant;

/** 缓存Key常量 —— 替代Ruoyi原Redis key常量，用于Caffeine去Redis改造 */
public class CacheConstants {
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";
    public static final String SYS_CONFIG_KEY = "sys_config:";
    public static final String SYS_DICT_KEY = "sys_dict:";
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";
    public static final String RATE_LIMIT_KEY = "rate_limit:";
    public static final String LOGIN_FAIL_KEY = "login_fail:";
    public static final String SMS_RATE_KEY = "sms_rate:";
    public static final String PORTAL_TOKEN_KEY = "portal_tokens:";
}
