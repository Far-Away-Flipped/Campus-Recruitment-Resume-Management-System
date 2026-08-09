package com.atmoto.recruit.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/** 接口限流注解 —— 基于Caffeine替代Redis Lua脚本 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {
    /** 限流key前缀 */
    String key() default "";
    /** 时间窗口 */
    int time() default 60;
    /** 时间单位 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    /** 窗口内最大请求数 */
    int count() default 10;
}
