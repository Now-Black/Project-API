package com.APIU.controller;


import com.APIU.annotation.GlobalInterceptor;
import com.APIU.annotation.VerifyParam;
import com.APIU.entity.config.AppConfig;
import com.APIU.entity.constants.Constants;
import com.APIU.entity.dto.SessionWebUserDto;
import com.APIU.entity.dto.UploadResultDto;
import com.APIU.entity.enums.FileCategoryEnums;
import com.APIU.entity.enums.FileDelFlagEnums;
import com.APIU.entity.enums.FileFolderTypeEnums;
import com.APIU.entity.po.FileInfo;
import com.APIU.entity.query.FileInfoQuery;
import com.APIU.entity.vo.FileInfoVO;
import com.APIU.entity.vo.PaginationResultVO;
import com.APIU.entity.vo.ResponseVO;
import com.APIU.service.FileInfoService;
import com.APIU.utils.CopyTools;
import com.APIU.utils.StringTools;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.bcel.Const;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.PushBuilder;
import java.util.List;

@RestController("fileInfoController")
@RequestMapping("/file")
public class FileController extends CommonfileController{
    @Resource
    private AppConfig appConfig;
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
    @RequestMapping("/getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse  response,
                         @PathVariable String imageFolder, @PathVariable String imageName){
        readimage(response,imageFolder,imageName);
    }
    @RequestMapping("/ts/getVideoInfo/{fileId}")
    public void getVideInfo(HttpServletResponse response,@PathVariable String fileid ,
                            HttpSession session){
       SessionWebUserDto sessionWebUserDto = (SessionWebUserDto)session.getAttribute(Constants.SESSION_KEY);
       getfile(response,fileid, sessionWebUserDto.getUserId());
    }
    @RequestMapping("/getFile/{fileId}")
    public void getFile(@PathVariable String fileid , HttpSession session,HttpServletResponse response){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        getfile(response,fileid,sessionWebUserDto.getUserId());
    }
    @RequestMapping("/newFoloder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO newFoloder(HttpSession session,
                                 @VerifyParam(required = true) String filePid ,
                                 @VerifyParam(required = true) String fileName){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        FileInfo fileInfo = fileInfoService.newfolder(sessionWebUserDto.getUserId(),fileName,filePid);
        return getSuccessResponseVO(fileInfo);
    }
    @RequestMapping("/getFolderInfo")
    public ResponseVO getFolderInfo(HttpSession session,String path){
        SessionWebUserDto webUserDto = (SessionWebUserDto)session.getAttribute(Constants.SESSION_KEY);
        return getfildorinfo(webUserDto.getUserId(),path);
    }
    @RequestMapping("/rename")
    public ResponseVO rename(HttpSession session, String filename , String fileid){
        SessionWebUserDto sessionWebUserDto =(SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        FileInfo fileInfo = fileInfoService.rename(sessionWebUserDto.getUserId(),filename,fileid);
        return getSuccessResponseVO(CopyTools.copy(fileInfo,FileInfoVO.class));
    }
    @RequestMapping("loadAllFolder")
    public ResponseVO loadAllFolder(HttpSession session,String fileid , String filepid){
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(webUserDto.getUserId());
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        query.setFilePid(filepid);
        if(!StringTools.isEmpty(fileid)){
            String[] strings = fileid.split(",");
            query.setExculdefileidArray(strings);
        }
        query.setOrderBy("create_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        List<FileInfo> list = fileInfoService.findListByParam(query);
        return getSuccessResponseVO(CopyTools.copyList(list,FileInfoVO.class));
    }
}
