package com.APIU.controller;


import com.APIU.entity.vo.ResponseVO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController("ShareController")
public class ShareController {

    @RequestMapping("loadShareList")
    private ResponseVO loadShareList(HttpSession session , String pageno){

    }
}
