package com.edc.spring.controller;

import com.edc.spring.annotation.*;
import com.edc.spring.service.TestServiceTwo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by edward.coo on 2018/8/9.
 */
@CooController
public class Test2Controller {

    @CooAutowired
    private TestServiceTwo testServiceTwo;

    @CooResponseBody
    @CooRequestMapping("test//method")
    public String query3(HttpServletRequest request, HttpServletResponse response,
                         @CooRequestParam("name") String name, @CooRequestParam("age") Integer age) {
        String result = testServiceTwo.query(name, age);
        return result;
    }

}
