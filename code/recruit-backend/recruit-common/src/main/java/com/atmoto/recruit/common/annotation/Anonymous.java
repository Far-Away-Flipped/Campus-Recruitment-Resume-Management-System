package com.atmoto.recruit.common.annotation;

import java.lang.annotation.*;

/** 匿名访问标记 —— 标注无需登录的接口 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Anonymous {
}
