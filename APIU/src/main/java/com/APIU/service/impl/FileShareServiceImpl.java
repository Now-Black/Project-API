package com.APIU.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.APIU.entity.enums.PageSize;
import com.APIU.entity.query.FileShareQuery;
import com.APIU.entity.po.FileShare;
import com.APIU.entity.vo.PaginationResultVO;
import com.APIU.entity.query.SimplePage;
import com.APIU.mappers.FileShareMapper;
import com.APIU.service.FileShareService;
import com.APIU.utils.StringTools;


/**
 * 分享信息 业务接口实现
 */
@Service("fileShareService")
public class FileShareServiceImpl implements FileShareService {

	@Resource
	private FileShareMapper<FileShare, FileShareQuery> fileShareMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<FileShare> findListByParam(FileShareQuery param) {
		return this.fileShareMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(FileShareQuery param) {
		return this.fileShareMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<FileShare> findListByPage(FileShareQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<FileShare> list = this.findListByParam(param);
		PaginationResultVO<FileShare> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(FileShare bean) {
		return this.fileShareMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<FileShare> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.fileShareMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<FileShare> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.fileShareMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(FileShare bean, FileShareQuery param) {
		StringTools.checkParam(param);
		return this.fileShareMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(FileShareQuery param) {
		StringTools.checkParam(param);
		return this.fileShareMapper.deleteByParam(param);
	}

	/**
	 * 根据ShareId获取对象
	 */
	@Override
	public FileShare getFileShareByShareId(String shareId) {
		return this.fileShareMapper.selectByShareId(shareId);
	}

	/**
	 * 根据ShareId修改
	 */
	@Override
	public Integer updateFileShareByShareId(FileShare bean, String shareId) {
		return this.fileShareMapper.updateByShareId(bean, shareId);
	}

	/**
	 * 根据ShareId删除
	 */
	@Override
	public Integer deleteFileShareByShareId(String shareId) {
		return this.fileShareMapper.deleteByShareId(shareId);
	}
}