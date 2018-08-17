package com.edc.spring.service.impl;

import com.edc.spring.annotation.CooService;
import com.edc.spring.service.TestServiceTwo;

/**
 * Created by edward.coo on 2018/8/9.
 */
@CooService("testServiceTwo")
public class TestServiceTwoImpl implements TestServiceTwo {
    public String query(String name, Integer age) {
        return "this is queryTwo result: name="+name+",age="+age;
    }
}
