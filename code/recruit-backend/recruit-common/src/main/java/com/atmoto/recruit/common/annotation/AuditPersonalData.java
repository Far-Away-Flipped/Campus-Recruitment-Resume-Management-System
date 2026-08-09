package com.atmoto.recruit.common.annotation;

import java.lang.annotation.*;

/** 个人信息访问审计注解 —— 标记需留痕的操作 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditPersonalData {
    /** 操作类型：VIEW_DETAIL/VIEW_ATTACHMENT/DOWNLOAD_ATTACHMENT/EXPORT_EXCEL */
    String operation();
    /** 操作描述 */
    String description() default "";
}
