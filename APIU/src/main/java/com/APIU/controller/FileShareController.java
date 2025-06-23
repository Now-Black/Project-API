package com.APIU.controller;

import java.util.List;

import com.APIU.entity.query.FileShareQuery;
import com.APIU.entity.po.FileShare;
import com.APIU.entity.vo.ResponseVO;
import com.APIU.service.FileShareService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 分享信息 Controller
 */
@RestController("fileShareController")
@RequestMapping("/fileShare")
public class FileShareController extends ABaseController{

	@Resource
	private FileShareService fileShareService;
	/**
	 * 根据条件分页查询
	 */
	@RequestMapping("/loadDataList")
	public ResponseVO loadDataList(FileShareQuery query){
		return getSuccessResponseVO(fileShareService.findListByPage(query));
	}

	/**
	 * 新增
	 */
	@RequestMapping("/add")
	public ResponseVO add(FileShare bean) {
		fileShareService.add(bean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 批量新增
	 */
	@RequestMapping("/addBatch")
	public ResponseVO addBatch(@RequestBody List<FileShare> listBean) {
		fileShareService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 批量新增/修改
	 */
	@RequestMapping("/addOrUpdateBatch")
	public ResponseVO addOrUpdateBatch(@RequestBody List<FileShare> listBean) {
		fileShareService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 根据ShareId查询对象
	 */
	@RequestMapping("/getFileShareByShareId")
	public ResponseVO getFileShareByShareId(String shareId) {
		return getSuccessResponseVO(fileShareService.getFileShareByShareId(shareId));
	}

	/**
	 * 根据ShareId修改对象
	 */
	@RequestMapping("/updateFileShareByShareId")
	public ResponseVO updateFileShareByShareId(FileShare bean,String shareId) {
		fileShareService.updateFileShareByShareId(bean,shareId);
		return getSuccessResponseVO(null);
	}

	/**
	 * 根据ShareId删除
	 */
	@RequestMapping("/deleteFileShareByShareId")
	public ResponseVO deleteFileShareByShareId(String shareId) {
		fileShareService.deleteFileShareByShareId(shareId);
		return getSuccessResponseVO(null);
	}
}