package com.edc.spring.annotation;

import java.lang.annotation.*;

/**
 * Created by edward.coo on 2018/8/9.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CooController {
    String value() default "";
}
