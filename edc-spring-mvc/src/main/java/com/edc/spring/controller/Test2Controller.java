package com.edc.spring.controller;

import com.edc.spring.annotation.*;
import com.edc.spring.service.TestService;
import com.edc.spring.service.TestServiceTwo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by edward.coo on 2018/8/9.
 */
@CooController
@CooRequestMapping("/test/")
public class Test2Controller {

    @CooResource(name = "testService")
    private TestService testService;
    @CooAutowired
    private TestServiceTwo testServiceTwo;

    @CooRequestMapping({"query","query4"})
    public void query(HttpServletRequest request, HttpServletResponse response, @CooRequestParam("name") String name, @CooRequestParam Integer age) {
        try {
            response.setHeader("Content-type", "text/html;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            PrintWriter writer = response.getWriter();
            String result = testService.query(name, age);
            writer.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @CooRequestMapping("query2")
    public String query2(HttpServletRequest request, HttpServletResponse response, @CooRequestParam("name") String name, @CooRequestParam("age") Integer age) {
        String result = testServiceTwo.query(name, age);
        return result;
    }

    @CooResponseBody
    @CooRequestMapping("query3")
    public String query3(HttpServletRequest request, HttpServletResponse response, @CooRequestParam("name") String name, @CooRequestParam("age") Integer age) {
        String result = testServiceTwo.query(name, age);
        return result;
    }
    public static void main(String[] args) {
        System.out.println(111);
    }

}
