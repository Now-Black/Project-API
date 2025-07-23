package com.APIU.controller;

import com.APIU.component.RedisUtils;
import com.APIU.entity.config.AppConfig;
import com.APIU.entity.constants.Constants;
import com.APIU.entity.dto.DownloadFileDto;
import com.APIU.entity.enums.*;
import com.APIU.entity.po.FileInfo;
import com.APIU.entity.query.FileInfoQuery;
import com.APIU.entity.vo.FileInfoVO;
import com.APIU.entity.vo.FolderVO;
import com.APIU.entity.vo.ResponseVO;
import com.APIU.exception.BusinessException;
import com.APIU.service.FileInfoService;
import com.APIU.utils.CopyTools;
import com.APIU.utils.StringTools;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;
import javax.mail.Folder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class CommonfileController extends ABaseController{
    @Resource
    private AppConfig appConfig;
    @Resource
    private FileInfoService fileInfoService;
    @Resource
    private RedisUtils redisUtils;
    protected void readimage(HttpServletResponse response, String imageFolder,
                   String imageName){
        if(StringTools.isEmpty(imageFolder) || StringUtils.isBlank(imageName)){
            return;
        }
        String suffix = StringTools.getFileSuffix(imageName);
        String path = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE
                + imageFolder + "/" +imageName;
        suffix = suffix.replace(".","");
        String contentType = "image/" + suffix;
        response.setHeader("Cache-Control", "max-age=2592000");
        response.setContentType(contentType);
        readFile(response,path);
    }

    protected void getfile(HttpServletResponse response, String fileid , String userid){
        String filepath = null;
        if(fileid.endsWith(".ts")){
            String[] arrayts = fileid.split("_");
            String realfileid = arrayts[0];
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(realfileid,userid);
            if(fileInfo == null){
                FileInfoQuery query = new FileInfoQuery();
                query.setFileId(realfileid);
                List<FileInfo> list = fileInfoService.findListByParam(query);
                FileInfo file = list.get(0);
                if(file==null){
                    return;
                }

                query = new FileInfoQuery();
                query.setFilePath(fileInfo.getFilePath());
                query.setUserId(userid);
                Integer coutnt = fileInfoService.findCountByParam(query);
                if(coutnt==0){
                    return;
                }
            }
            String filename= fileInfo.getFilePath();
            filename = StringTools.getFileNameNoSuffix(filename) + "/" +fileid;
            filepath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + filename  ;
        }else {
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileid,userid);
            FileTypeEnums fileTypeEnums = FileTypeEnums.getByType(fileInfo.getFileType()) ;
            if(fileInfo == null || !fileInfo.getDelFlag().equals(FileDelFlagEnums.USING.getFlag())){
                return;
            }
            if(FileCategoryEnums.VIDEO.getCategory().equals(fileInfo.getFileCategory())){
                String filenamereal = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
                filepath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE
                        +filenamereal +"/"+Constants.M3U8_NAME;
            }else {
                filepath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE
                        +fileInfo.getFilePath();
            }
        }
        File file = new File(filepath);
        if(!file.exists()){
            return;
        }
        readFile(response,file.getPath());
    }
    protected ResponseVO getfildorinfo(String userid,String path){
        String[] filepath = path.split("/");
        FileInfoQuery query = new FileInfoQuery();
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        query.setUserId(userid);
        query.setFileidArray(filepath);
        List<FileInfo> fileInfo = fileInfoService.findListByParam(query);
        return getSuccessResponseVO(CopyTools.copyList(fileInfo, FileInfoVO.class));
    }
    protected ResponseVO createDownloadUrl(String userid , String fileid){
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileid,userid);
        if(fileInfo == null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if(fileInfo.getFolderType().equals(FileFolderTypeEnums.FOLDER)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        DownloadFileDto downloadFileDto = new DownloadFileDto();
        String code = StringTools.getRandomNumber(50);
        downloadFileDto.setDownloadCode(code);
        downloadFileDto.setFileName(fileInfo.getFileName());
        downloadFileDto.setFilePath(fileInfo.getFilePath());
        redisUtils.setex(code,downloadFileDto,Constants.REDIS_KEY_EXPIRES_DAY);
        return getSuccessResponseVO(code);
    }
    void download(HttpServletResponse response , HttpServletRequest request, String code) throws UnsupportedEncodingException {
        DownloadFileDto fileDto = (DownloadFileDto)redisUtils.get(code);
        if(fileDto == null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String downpath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileDto.getFilePath();
        String fileName = fileDto.getFileName();
        response.setContentType("application/x-msdownload; charset=UTF-8");
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {//IE浏览器
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } else {
            fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        readFile(response,downpath);
    }

    protected ResponseVO getfolderinfo(String path , String userid){
        String[] paths = path.split("/");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userid);
        query.setFileidArray(paths);
        query.setFileCategory(FileFolderTypeEnums.FOLDER.getType());
        String orderBy = "field(file_id,\"" + StringUtils.join(paths, "\",\"") + "\")";
        query.setOrderBy(orderBy);
        List<FileInfo> list = fileInfoService.findListByParam(query);
        return getSuccessResponseVO(CopyTools.copyList(list, FolderVO.class));

    }

}

