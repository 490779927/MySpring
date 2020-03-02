package com.mvcframework.action;

import com.mvcframework.annotation.MyController;
import com.mvcframework.annotation.MyRequestMapping;
import com.mvcframework.annotation.MyRequestParm;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ：yangjin.
 * @Date ：Created in 23:50 2020/3/2
 */
@MyController
public class DemoAction {

    @MyRequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse resp, @MyRequestParm("name") String name) {
        System.out.println("ok:" + name);
    }
}
