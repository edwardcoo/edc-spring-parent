package com.edc.spring.annotation;

import java.lang.annotation.*;

/**
 * Created by edward.coo on 2018/8/9.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CooResource {
    String name() default "";
    Class<?> type() default java.lang.Object.class;
}
