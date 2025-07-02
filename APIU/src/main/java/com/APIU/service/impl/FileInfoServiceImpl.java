package com.APIU.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.APIU.component.RedisComponent;
import com.APIU.component.RedisUtils;
import com.APIU.entity.constants.Constants;
import com.APIU.entity.dto.SessionWebUserDto;
import com.APIU.entity.dto.UploadResultDto;
import com.APIU.entity.dto.UserSpaceDto;
import com.APIU.entity.enums.*;
import com.APIU.entity.po.UserInfo;
import com.APIU.entity.query.UserInfoQuery;
import com.APIU.exception.BusinessException;
import com.APIU.mappers.UserInfoMapper;
import org.springframework.stereotype.Service;

import com.APIU.entity.query.FileInfoQuery;
import com.APIU.entity.po.FileInfo;
import com.APIU.entity.vo.PaginationResultVO;
import com.APIU.entity.query.SimplePage;
import com.APIU.mappers.FileInfoMapper;
import com.APIU.service.FileInfoService;
import com.APIU.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


/**
 * 文件信息 业务接口实现
 */
@Service("fileInfoService")
public class FileInfoServiceImpl implements FileInfoService {
	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
	@Resource
	private RedisComponent redisComponent;
	@Resource
	private RedisUtils redisUtils;
	@Resource
	private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<FileInfo> findListByParam(FileInfoQuery param) {
		return this.fileInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(FileInfoQuery param) {
		return this.fileInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<FileInfo> findListByPage(FileInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<FileInfo> list = this.findListByParam(param);
		PaginationResultVO<FileInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(FileInfo bean) {
		return this.fileInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<FileInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.fileInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<FileInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.fileInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(FileInfo bean, FileInfoQuery param) {
		StringTools.checkParam(param);
		return this.fileInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(FileInfoQuery param) {
		StringTools.checkParam(param);
		return this.fileInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据FileIdAndUserId获取对象
	 */
	@Override
	public FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId) {
		return this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
	}
	/**
	 * 根据FileIdAndUserId修改
	 */
	@Override
	public Integer updateFileInfoByFileIdAndUserId(FileInfo bean, String fileId, String userId) {
		return this.fileInfoMapper.updateByFileIdAndUserId(bean, fileId, userId);
	}
	/**
	 * 根据FileIdAndUserId删除
	 */
	@Override
	public Integer deleteFileInfoByFileIdAndUserId(String fileId, String userId) {
		return this.fileInfoMapper.deleteByFileIdAndUserId(fileId, userId);
	}
	@Override
	@Transactional(rollbackFor = Exception.class)
	public UploadResultDto uploadFile(SessionWebUserDto webUserDto, String fileId, String fileName,
							   MultipartFile file, String filePid, String fileMd5,
							   String chunkIndex, String chunks){
		UploadResultDto resultDto = new UploadResultDto();
		if(StringTools.isEmpty(fileId)){
			fileId = StringTools.getRandomNumber(10);
		}
		resultDto.setFileId(fileId);
		UserSpaceDto spaceDto = redisComponent.getUserSpaveDto(webUserDto.getUserId());
		Date date = new Date();
		if(chunkIndex.equals(Constants.ZERO_STR)){
			FileInfoQuery query = new FileInfoQuery();
			query.setFileMd5(fileMd5);
			query.setDelFlag(FileDelFlagEnums.USING.getFlag());
			query.setSimplePage(new SimplePage(0,1));
			List<FileInfo> list =  fileInfoMapper.selectList(query);
			if(!list.isEmpty()){
				FileInfo fileInfo = list.get(0);
				if(fileInfo.getFileSize() + spaceDto.getUseSpace() > spaceDto.getTotalSpace())
				{
					throw new BusinessException(ResponseCodeEnum.CODE_904);
				}
				fileInfo.setFileId(fileId);
				fileInfo.setFileMd5(fileMd5);
				fileInfo.setCreateTime(date);
				fileInfo.setLastUpdateTime(date);
				fileInfo.setFilePid(filePid);
				fileInfo.setStatus(FileStatusEnums.USING.getStatus());
				fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
				fileInfo.setUserId(webUserDto.getUserId());
				fileInfo.setFileName(Refilename(fileName,filePid,webUserDto.getUserId()));
				fileInfoMapper.insert(fileInfo);
				resultDto.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());
				updateUserSpace(webUserDto.getUserId(),fileInfo.getFileSize());
			}
		}


	}
	private void updateUserSpace(String userid,Long filesize){
		Integer count = userInfoMapper.updateUserSpace(userid,filesize,null);
		if(count==0){
			throw new BusinessException(ResponseCodeEnum.CODE_904);
		}
		UserSpaceDto userSpaceDto = redisComponent.getUserSpaveDto(userid);
		userSpaceDto.setUseSpace(userSpaceDto.getUseSpace()+filesize);
		redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE+userid,userSpaceDto,Constants.REDIS_KEY_EXPIRES_DAY);
	}
	private String Refilename(String filename,String filepid,String userid){
		FileInfoQuery query = new FileInfoQuery();
		query.setFilePid(filepid);
		query.setUserId(userid);
		query.setDelFlag(FileDelFlagEnums.USING.getFlag());
		query.setFileName(filename);
		Integer count = fileInfoMapper.selectCount(query);
		if(count > 0){
			filename = StringTools.rename(filename);
		}
		return filename;

	}
}