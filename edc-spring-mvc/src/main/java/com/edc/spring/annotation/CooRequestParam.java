package com.edc.spring.annotation;

import java.lang.annotation.*;

/**
 * Created by edward.coo on 2018/8/9.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CooRequestParam {
    String value() default "";
    boolean required() default true;

}
