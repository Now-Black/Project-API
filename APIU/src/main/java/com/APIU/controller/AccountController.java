package com.APIU.controller;

import java.io.IOException;
import java.util.List;

import com.APIU.entity.constants.Constants;
import com.APIU.entity.dto.CreateImageCode;
import com.APIU.entity.query.UserInfoQuery;
import com.APIU.entity.po.UserInfo;
import com.APIU.entity.vo.ResponseVO;
import com.APIU.exception.BusinessException;
import com.APIU.service.EmailCodeService;
import com.APIU.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 用户信息 Controller
 */
@RestController("userInfoController")
public class AccountController extends ABaseController{
    @Resource
    private EmailCodeService emailCodeService;

    /*加载验证码，设置响应头，保存验证码code至session，推送至客户端*/
    @RequestMapping("/checkCode")
    public void checkCode(HttpServletResponse httpServletResponse, HttpSession session,Integer type)
            throws IOException{
        CreateImageCode createImageCode = new CreateImageCode(130,38,5,10);
        httpServletResponse.setHeader("Pragma","no-cache");
        httpServletResponse.setHeader("Cache-control","no-cache");
        httpServletResponse.setDateHeader("Expires",0);
        httpServletResponse.setContentType("image/jpeg");
        String s  = createImageCode.getCode();
        if(type == null || type == 0){
            session.setAttribute(Constants.CHECK_CODE_KEY,s);
        }else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL,s);
        }
        createImageCode.write(httpServletResponse.getOutputStream());
    }
    @RequestMapping("/sendEmailCode")
    public ResponseVO sendEmailCode(String email,String checkCode,Integer type,HttpSession session){
        try {
            if(session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL) != checkCode){
                throw new BusinessException("图片验证码错误");
            }
            emailCodeService.sendEmailCode(email,type);
            return getSuccessResponseVO(null);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }

    @RequestMapping("/register")
    public ResponseVO register(String email,String nickName,String password,String checkCode,String emailCode,
                                HttpSession session){
        try {
            if(! checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))){

            }


        }finally {

        }



    return getSuccessResponseVO(null);
    }
}