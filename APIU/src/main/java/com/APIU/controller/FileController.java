package com.APIU.controller;


import com.APIU.annotation.GlobalInterceptor;
import com.APIU.entity.constants.Constants;
import com.APIU.entity.dto.SessionWebUserDto;
import com.APIU.entity.dto.UploadResultDto;
import com.APIU.entity.enums.FileCategoryEnums;
import com.APIU.entity.enums.FileDelFlagEnums;
import com.APIU.entity.po.FileInfo;
import com.APIU.entity.query.FileInfoQuery;
import com.APIU.entity.vo.FileInfoVO;
import com.APIU.entity.vo.PaginationResultVO;
import com.APIU.entity.vo.ResponseVO;
import com.APIU.service.FileInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController("fileInfoController")
@RequestMapping("/file")
public class FileController extends ABaseController{
    @Resource
    private FileInfoService fileInfoService;
    @RequestMapping("/loadDataList")
    public ResponseVO loadDataList(HttpSession session , FileInfoQuery query,String category){
        FileCategoryEnums fileCategoryEnums = FileCategoryEnums.getByCode(category);
        if(fileCategoryEnums !=null){
            query.setFileCategory(fileCategoryEnums.getCategory());
        }
        SessionWebUserDto webUserDto = (SessionWebUserDto)session.getAttribute(Constants.SESSION_KEY);
        query.setUserId(webUserDto.getUserId());
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setOrderBy("last_update_time desc");
        PaginationResultVO<FileInfo> paginationResultVO = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convertoresultvo(paginationResultVO, FileInfoVO.class));
    }
    @RequestMapping("/uploadFile")
    @GlobalInterceptor(checklogin = true)
    public ResponseVO uploadFile(HttpSession session, String fileId, String fileName,
                                 MultipartFile file,String filePid, String fileMd5,
                                 Integer chunkIndex,Integer chunks){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        UploadResultDto uploadResultDto = fileInfoService.uploadFile(sessionWebUserDto,fileId,
                fileName,file,filePid,fileMd5,chunkIndex,chunks);
        return getSuccessResponseVO(uploadResultDto);
    }


}
