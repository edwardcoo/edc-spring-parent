package com.edc.spring.service.impl;

import com.edc.spring.annotation.CooAutowired;
import com.edc.spring.annotation.CooService;
import com.edc.spring.service.TestService;
import com.edc.spring.service.TestServiceTwo;

/**
 * Created by edward.coo on 2018/8/9.
 */
@CooService("testService")
public class TestServiceImpl implements TestService {
    @CooAutowired
    TestServiceTwo testServiceTwo;
    public String query(String name, Integer age) {
        return "this is query result: name="+name+",age="+age;
    }
}
