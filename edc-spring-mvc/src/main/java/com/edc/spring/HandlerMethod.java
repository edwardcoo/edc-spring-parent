package com.edc.spring;

import java.lang.reflect.Method;

/**
 * Created by edward.coo on 2018/8/30.
 */
public class HandlerMethod {
    private Object bean;
    private Method method;
    public HandlerMethod(Method method,Object bean){
        if (method == null) {
            throw new IllegalArgumentException("Method is required");
        }
        if (bean == null) {
            throw new IllegalArgumentException("Bean is required");
        }
        this.method = method;
        this.bean = bean;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getBean() {
        return bean;
    }

    public void setBean(Object bean) {
        this.bean = bean;
    }
}
