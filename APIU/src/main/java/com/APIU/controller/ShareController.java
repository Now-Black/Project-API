package com.APIU.controller;


import com.APIU.annotation.GlobalInterceptor;
import com.APIU.annotation.VerifyParam;
import com.APIU.entity.constants.Constants;
import com.APIU.entity.dto.SessionWebUserDto;
import com.APIU.entity.po.FileShare;
import com.APIU.entity.query.FileShareQuery;
import com.APIU.entity.query.SimplePage;
import com.APIU.entity.vo.PaginationResultVO;
import com.APIU.entity.vo.ResponseVO;
import com.APIU.service.FileShareService;
import org.apache.tomcat.util.bcel.Const;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController("ShareController")
@RequestMapping("share")
public class ShareController extends ABaseController{
    @Resource
    private FileShareService fileShareService;
    @RequestMapping("loadShareList")
    public ResponseVO loadShareList(HttpSession session , FileShareQuery query){
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        query.setUserId(webUserDto.getUserId());
        query.setOrderBy("share_time desc");
        PaginationResultVO<FileShare> paginationResultVO = fileShareService.findListByPage(query);
        ABaseController aBaseController  = new ABaseController();
        return getSuccessResponseVO(paginationResultVO);
    }
    @RequestMapping("shareFile")
    @GlobalInterceptor(checkParams = true,checklogin = true)
    public ResponseVO shareFile(@VerifyParam(required = true) String fileId,
                                String validType,String code,HttpSession session){
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        FileShare fileShare = new FileShare();
        fileShare.setFileId(fileId);
        fileShare.setUserId(webUserDto.getUserId());
        fileShare.setCode(code);
        fileShareService.fileshare(fileShare);
        return getSuccessResponseVO(fileShare);
    }
    @RequestMapping("cancelShare")
    public ResponseVO cancelShare(HttpSession session , String shareIds){
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        fileShareService.cancelShare(shareIds,webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }

}
