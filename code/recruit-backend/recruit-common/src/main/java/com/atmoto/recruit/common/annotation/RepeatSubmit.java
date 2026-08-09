package com.atmoto.recruit.common.annotation;

import java.lang.annotation.*;

/** 防重复提交注解 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {
    /** 间隔时间（毫秒），默认10000 */
    int interval() default 10000;
    /** 提示消息 */
    String message() default "请勿重复提交";
}
