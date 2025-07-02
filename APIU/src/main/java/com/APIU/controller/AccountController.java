package com.APIU.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Random;

import com.APIU.annotation.GlobalInterceptor;
import com.APIU.annotation.VerifyParam;
import com.APIU.component.RedisUtils;
import com.APIU.entity.config.AppConfig;
import com.APIU.entity.constants.Constants;
import com.APIU.entity.dto.CreateImageCode;
import com.APIU.entity.dto.SessionWebUserDto;
import com.APIU.entity.dto.UserSpaceDto;
import com.APIU.entity.enums.VerifyRegexEnum;
import com.APIU.entity.query.UserInfoQuery;
import com.APIU.entity.po.UserInfo;
import com.APIU.entity.vo.ResponseVO;
import com.APIU.exception.BusinessException;
import com.APIU.mappers.UserInfoMapper;
import com.APIU.service.EmailCodeService;
import com.APIU.service.UserInfoService;
import com.APIU.utils.StringTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 用户信息 Controller
 */
@RestController("userInfoController")
public class AccountController extends ABaseController{
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private AppConfig appConfig;
    @Resource
    private EmailCodeService emailCodeService;
    @Resource
    private UserInfoService userInfoService;
    @Resource
    private UserInfoMapper<UserInfo,UserInfoQuery> userInfoMapper;
    /*加载验证码，设置响应头，保存验证码code至session，推送至客户端*/
    @GlobalInterceptor(checklogin = false)
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
    @GlobalInterceptor(checkParams = true)
    public ResponseVO register(@VerifyParam(required = true,regex = VerifyRegexEnum.EMAIL) String email,
                               @VerifyParam(required = true,max = 10,min = 0) String nickName,
                               @VerifyParam(required = true,max = 10,min = 0)String password,
                               @VerifyParam(required = true,max = 10,min = 0)String checkCode,
                               @VerifyParam(required = true,max = 10,min = 0)String emailCode,
                               HttpSession session){
        try {
            if(! checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))){
                throw new BusinessException("验证码输入错误");
            }
            userInfoService.register(email,nickName,password,emailCode);
            return getSuccessResponseVO(null);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @RequestMapping("/login")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO login(@VerifyParam(required = true) String email,
                            @VerifyParam(required = true) String password,
                            @VerifyParam(required = true) String code,
                            HttpSession session){
        try {
            if(! session.getAttribute(Constants.CHECK_CODE_KEY).equals(code)){
                throw new BusinessException("验证码输入错误");
            }
            SessionWebUserDto sessionWebUserDto = userInfoService.login(email,password);
            session.setAttribute(Constants.SESSION_KEY,sessionWebUserDto);
            return getSuccessResponseVO(sessionWebUserDto);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }
    @RequestMapping("/resetPwd")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO resetPwd(@VerifyParam(required = true,regex = VerifyRegexEnum.EMAIL) String email,
                               @VerifyParam(required = true) String password,
                               @VerifyParam(required = true) String checkCode,
                               @VerifyParam(required = true) String emailCode,
                               HttpSession session){
        try {
            if(! session.getAttribute(Constants.CHECK_CODE_KEY).equals(checkCode)){
                throw new BusinessException("验证码输入错误");
            }
            userInfoService.resetpassword(email,password,emailCode);
            return getSuccessResponseVO(null);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }
    @RequestMapping("/getAvatar/{userId}")
    public void getAvatar(HttpServletResponse response, @PathVariable String userId){
        String avatarname = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        File folder = new File(appConfig.getProjectFolder() + avatarname);
        if(!folder.exists()){
            folder.mkdirs();
        }
        String avatarnamepath = appConfig.getProjectFolder() + avatarname + userId + Constants.AVATAR_SUFFIX;
        File file = new File(avatarnamepath);
        if(!file.exists()){
            if(!new File(appConfig.getProjectFolder()+avatarname+Constants.AVATAR_DEFUALT).exists()){
                pringtnodefaultavatar(response);
                return;
            }
            avatarnamepath = appConfig.getProjectFolder() + avatarname + Constants.AVATAR_DEFUALT;
        }
        response.setContentType("image/jpg");
        readFile(response,avatarnamepath);
    }

    private void pringtnodefaultavatar(HttpServletResponse response){
        response.setHeader(CONTENT_TYPE,CONTENT_TYPE_VALUE);
        response.setStatus(HttpStatus.OK.value());
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.print("请在头像目录下放置默认头像default_avatar.jpg");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @GlobalInterceptor(checklogin = true)
    @RequestMapping("/getUseSpace")
    public ResponseVO getUseSpace(HttpSession session){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto)session.getAttribute(Constants.SESSION_KEY);
//        if(sessionWebUserDto == null){
//            throw new BusinessException("请登录！");
//        }
        UserSpaceDto userSpaceDto =(UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE+sessionWebUserDto.getUserId());
        return getSuccessResponseVO(userSpaceDto);

    }
    @RequestMapping("/getUserInfo")
    public ResponseVO getUserInfo(HttpSession session){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto)session.getAttribute(Constants.SESSION_KEY);
        return getSuccessResponseVO(sessionWebUserDto);
    }
    @RequestMapping("/logout")
    public ResponseVO logout(HttpSession session){
        session.invalidate();
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/updatePassword")
    public ResponseVO updatePassword(HttpSession session,String password){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfoMapper.updateByUserId(userInfo,sessionWebUserDto.getUserId());
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/updateUserAvatar")
    public ResponseVO updateUserAvatar(HttpSession session, MultipartFile avatar){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        String PathBase = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        File filefoloder = new File(PathBase);
        if(!filefoloder.exists()){
            filefoloder.mkdirs();
        }
        File tarfile = new File(filefoloder.getPath() + '/'+sessionWebUserDto.getUserId()+Constants.AVATAR_SUFFIX);
        try {
            avatar.transferTo(tarfile);
        } catch (IOException e) {
            throw new BusinessException("上传文件失败");
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setQqAvatar("");
        userInfoMapper.updateByUserId(userInfo,sessionWebUserDto.getUserId());
        sessionWebUserDto.setAvatar(null);
        session.setAttribute(Constants.SESSION_KEY,sessionWebUserDto);
        return getSuccessResponseVO(null);
    }
}