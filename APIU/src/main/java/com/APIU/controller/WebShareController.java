package com.APIU.controller;


import com.APIU.annotation.GlobalInterceptor;
import com.APIU.entity.constants.Constants;
import com.APIU.entity.dto.SessionShareDto;
import com.APIU.entity.dto.SessionWebUserDto;
import com.APIU.entity.enums.FileDelFlagEnums;
import com.APIU.entity.enums.ResponseCodeEnum;
import com.APIU.entity.po.FileInfo;
import com.APIU.entity.po.FileShare;
import com.APIU.entity.po.UserInfo;
import com.APIU.entity.query.FileInfoQuery;
import com.APIU.entity.vo.PaginationResultVO;
import com.APIU.entity.vo.ResponseVO;
import com.APIU.entity.vo.ShareInfoVO;
import com.APIU.exception.BusinessException;
import com.APIU.service.FileInfoService;
import com.APIU.service.FileShareService;
import com.APIU.service.UserInfoService;
import com.APIU.utils.CopyTools;
import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;
import org.omg.CORBA.PUBLIC_MEMBER;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController("webShareController")
@RequestMapping("showShare")
public class WebShareController extends ABaseController{

    @Resource
    private FileShareService fileShareService;
    @Resource
    private FileInfoService fileInfoService;
    @Resource
    private UserInfoService userInfoService;
    @RequestMapping("getShareLoginInfo")
    public ResponseVO getShareLoginInfo(String shareId , HttpSession session){
        SessionShareDto shareDto  = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        if(shareDto == null){
            return getSuccessResponseVO(null);
        }
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        ShareInfoVO shareInfoVO = getCommonShare(shareDto.getShareId());
        if(webUserDto==null || !webUserDto.getUserId().equals(shareDto.getShareId())){
            shareInfoVO.setCurrentUser(false);
        }else {
            shareInfoVO.setCurrentUser(true);
        }
        return getSuccessResponseVO(shareInfoVO);
    }

    private ShareInfoVO getCommonShare(String shareId){
        FileShare share = fileShareService.getFileShareByShareId(shareId);
        if(share == null){
            throw new BusinessException(ResponseCodeEnum.CODE_404.getMsg());
        }
        ShareInfoVO shareInfoVO = CopyTools.copy(share,ShareInfoVO.class);
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(share.getFileId(),share.getUserId());
        if(fileInfo == null){
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        shareInfoVO.setFileName(fileInfo.getFileName());
        UserInfo userInfo = userInfoService.getUserInfoByUserId(share.getUserId());
        shareInfoVO.setAvatar(userInfo.getQqAvatar());
        shareInfoVO.setNickName(userInfo.getNickName());
        return shareInfoVO;
    }
    @RequestMapping("getShareInfo")
    public ResponseVO getShareInfo(String shareId){
        return getSuccessResponseVO(getCommonShare(shareId));
    }

    /**
     *
     * @param shareId
     * @param code
     * @param session
     * @return
     */
    @RequestMapping("checkShareCode")
    public ResponseVO checkShareCode(String shareId , String code , HttpSession session){
        SessionShareDto shareDto = fileShareService.checkShareCode(code,shareId);
        session.setAttribute(Constants.SESSION_SHARE_KEY + shareId , shareDto);
        return getSuccessResponseVO(null);
    }

    /**
     *
     * @param shareId
     * @param filePid
     * @return
     */
    @RequestMapping("loadFileList")
    @GlobalInterceptor(checklogin = false,checkParams = false)
    public ResponseVO loadFileList(String shareId , String filePid , HttpSession session){
        SessionShareDto shareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        FileInfoQuery query = new FileInfoQuery();
        if(filePid == null || filePid.equals(Constants.ZERO_STR)){
            query.setFileId(shareDto.getFileId());
        }else {
            query.setFilePid(filePid);
            fileInfoService.checkRoot(filePid,shareDto.getShareUserId(),shareDto.getFileId());
        }
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setOrderBy("last_update_time desc");
        query.setUserId(shareDto.getShareUserId());
        PaginationResultVO resultVO = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convertoresultvo(resultVO,FileInfo.class));
    }

}
