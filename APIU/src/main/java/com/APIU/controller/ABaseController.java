package com.APIU.controller;
import com.APIU.component.RedisUtils;
import com.APIU.entity.enums.ResponseCodeEnum;
import com.APIU.entity.po.FileInfo;
import com.APIU.entity.vo.FileInfoVO;
import com.APIU.entity.vo.PaginationResultVO;
import com.APIU.entity.vo.ResponseVO;
import com.APIU.exception.BusinessException;
import com.APIU.utils.CopyTools;
import com.APIU.utils.StringTools;

import javax.servlet.http.HttpServletResponse;
import java.io.*;


public class ABaseController{


    protected static final String STATUC_SUCCESS = "success";

    protected static final String STATUC_ERROR = "error";

    protected <T> ResponseVO getSuccessResponseVO(T t) {


        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUC_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    protected <T> ResponseVO getBusinessErrorResponseVO(BusinessException e, T t) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUC_ERROR);
        if (e.getCode() == null) {
            vo.setCode(ResponseCodeEnum.CODE_600.getCode());
        } else {
            vo.setCode(e.getCode());
        }
        vo.setInfo(e.getMessage());
        vo.setData(t);
        return vo;
    }

    protected <T> ResponseVO getServerErrorResponseVO(T t) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUC_ERROR);
        vo.setCode(ResponseCodeEnum.CODE_500.getCode());
        vo.setInfo(ResponseCodeEnum.CODE_500.getMsg());
        vo.setData(t);
        return vo;
    }
    protected void readFile(HttpServletResponse response,String filePath){
        if(!StringTools.pathIsOk(filePath)){
            return;
        }
        OutputStream outputStream = null;
        FileInputStream inputStream = null;
        File file = new File(filePath);
        if(!file.exists()){return;}
        try {
            inputStream = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            outputStream = response.getOutputStream();
            int len = 0;
            /*文件流的读写循环，从输入流读取文件到输出流*/
            while((len = inputStream.read(bytes) )!= -1){
                outputStream.write(bytes,0,len);
            }
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(outputStream != null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }
    protected <S,T> PaginationResultVO<T> convertoresultvo(PaginationResultVO<S> paginationResultVO,
                                                           Class<T> classz){
        PaginationResultVO<T> resultVO = new PaginationResultVO();
        resultVO.setList(CopyTools.copyList(paginationResultVO.getList(), classz));
        resultVO.setPageNo(paginationResultVO.getPageNo());
        resultVO.setPageSize(paginationResultVO.getPageSize());
        resultVO.setPageTotal(paginationResultVO.getPageTotal());
        resultVO.setTotalCount(paginationResultVO.getTotalCount());
        return resultVO;
    }

}
