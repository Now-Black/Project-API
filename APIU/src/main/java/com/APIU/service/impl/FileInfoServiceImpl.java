package com.APIU.service.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.APIU.component.RedisComponent;
import com.APIU.component.RedisUtils;
import com.APIU.entity.config.AppConfig;
import com.APIU.entity.constants.Constants;
import com.APIU.entity.dto.SessionWebUserDto;
import com.APIU.entity.dto.UploadResultDto;
import com.APIU.entity.dto.UserSpaceDto;
import com.APIU.entity.enums.*;
import com.APIU.entity.po.UserInfo;
import com.APIU.entity.query.UserInfoQuery;
import com.APIU.exception.BusinessException;
import com.APIU.mappers.UserInfoMapper;
import com.APIU.utils.DateUtil;
import com.APIU.utils.ProcessUtils;
import org.apache.commons.io.FileUtils;
import org.aspectj.util.FileUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.APIU.entity.query.FileInfoQuery;
import com.APIU.entity.po.FileInfo;
import com.APIU.entity.vo.PaginationResultVO;
import com.APIU.entity.query.SimplePage;
import com.APIU.mappers.FileInfoMapper;
import com.APIU.service.FileInfoService;
import com.APIU.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;


/**
 * 文件信息 业务接口实现
 */
@Service("fileInfoService")
public class FileInfoServiceImpl implements FileInfoService {
	@Resource
	@Lazy
	private FileInfoServiceImpl fileInfoService;
	@Resource
	private AppConfig appConfig;
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
							   Integer chunkIndex, Integer chunks){
		boolean successupload = true;
		File filetemp = null;
		try {

		UploadResultDto resultDto = new UploadResultDto();
		if(StringTools.isEmpty(fileId)){
			fileId = StringTools.getRandomNumber(10);
		}
		resultDto.setFileId(fileId);
		UserSpaceDto spaceDto = redisComponent.getUserSpaveDto(webUserDto.getUserId());
		Date date = new Date();
		if(chunkIndex.equals(Constants.ZERO)){
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
				return resultDto;
			}
		}
		String filetempfold = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
		String filetemptar = webUserDto.getUserId() + fileId;
		filetemp = new File(filetempfold + filetemptar);
		if(filetemp.exists()){
			filetemp.mkdirs();
		}
		Long filesizetemp = redisComponent.getFileTempSize(webUserDto.getUserId(),fileId);
		if(filesizetemp + file.getSize() + spaceDto.getUseSpace() > spaceDto.getTotalSpace()){
			throw new BusinessException(ResponseCodeEnum.CODE_904);
		}
		File newfile = new File(filetemp.getPath() + "/" + chunkIndex);
		 file.transferTo(newfile);

		redisComponent.saveFileTempSize(webUserDto.getUserId(),fileId,file.getSize());
		if(chunkIndex < chunks - 1){
			resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
			return resultDto;
		}
		String month = DateUtil.format(date,DateTimePatternEnum.YYYYMM.getPattern());
		String suffix = StringTools.getFileSuffix(fileName);

		String filerealname = filetemptar + suffix;
		FileTypeEnums fileTypeEnums = FileTypeEnums.getFileTypeBySuffix(suffix);
		String filename= Refilename(fileName,filePid,webUserDto.getUserId());
		FileInfo fileInfo = new FileInfo();
		fileInfo.setFileId(fileId);
		fileInfo.setUserId(webUserDto.getUserId());
		fileInfo.setFileMd5(fileMd5);
		fileInfo.setFileName(filename);
		fileInfo.setFilePath(month + "/" + filerealname);
		fileInfo.setFilePid(filePid);
		fileInfo.setCreateTime(date);
		fileInfo.setLastUpdateTime(date);
		fileInfo.setFileCategory(fileTypeEnums.getCategory().getCategory());
		fileInfo.setFileType(fileTypeEnums.getType());
		fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
		fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
		fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
		fileInfoMapper.insert(fileInfo);
		Long spacenew = redisComponent.getFileTempSize(webUserDto.getUserId(),fileId);
		updateUserSpace(webUserDto.getUserId(), spacenew);
		resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				fileInfoService.transferfile(fileInfo.getFileId(),webUserDto.getUserId());
			}
		});
		return resultDto;
		}catch (IOException e) {
			successupload = false;
			throw new BusinessException("文件上传失败");
		}finally {
			if(!successupload && filetemp!=null){
				try {
					FileUtils.deleteDirectory(filetemp);
				} catch (IOException e) {
					throw new BusinessException("临时目录删除失败");
				}
			}


		}

	}
	@Async
	public void transferfile(String fileid , String userid){
		FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileid,userid);
		if(fileInfo == null || !fileInfo.getStatus().equals(FileStatusEnums.TRANSFER.getStatus())){
			return;
		}
		String filetemp = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
		String tarfile = userid + fileid;

		File filetempo = new File(filetemp+tarfile);
		if(!filetempo.exists()){
			filetempo.mkdirs();
		}
		String suffix = StringTools.getFileSuffix(fileInfo.getFileName());
		String month = DateUtil.format(fileInfo.getCreateTime(),DateTimePatternEnum.YYYYMM.getPattern());

		String realfile = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
		File tarfileser = new File(realfile + "/" +month);
		if(!tarfileser.exists()){
			tarfileser.mkdirs();
		}
		String tarfilename = tarfile + suffix;
		String tarfilepath = tarfileser.getPath() +"/" + tarfilename;
		union(filetempo.getPath(),tarfilepath,fileInfo.getFileName(),true);
		FileTypeEnums fileTypeEnums = FileTypeEnums.getFileTypeBySuffix(suffix);
		if(fileTypeEnums == FileTypeEnums.VIDEO){
			cutFile4Video(userid,tarfilepath);
			//生成视频缩略图，使用ffmpeg
		}else if(fileTypeEnums == FileTypeEnums.IMAGE){
			//生成图片缩略图，使用ffmpeg
		}
		FileInfo updatefileInfo = new FileInfo();
		updatefileInfo.setFileSize(new File(tarfilepath).length());
		updatefileInfo.setStatus(FileStatusEnums.USING.getStatus());
		fileInfoMapper.updateFileStatusWithOldStatus(fileid,userid,updatefileInfo,FileStatusEnums.TRANSFER.getStatus());

	}
	private void cutFile4Video(String fileId, String videoFilePath) {
		//创建同名切片目录
		File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
		if (!tsFolder.exists()) {
			tsFolder.mkdirs();
		}

		final String CMD_GET_CODE = "ffprobe -v error -select_streams v:0 -show_entries stream=codec_name %s";
		String cmd = String.format(CMD_GET_CODE, videoFilePath);
		String result = ProcessUtils.executeCommand(cmd, false);
		result = result.replace("\n", "");
		result = result.substring(result.indexOf("=") + 1);
		String codec = result.substring(0, result.indexOf("["));

		//转码
		if ("hevc".equals(codec)) {
			String newFileName = videoFilePath.substring(0, videoFilePath.lastIndexOf(".")) + "_" + videoFilePath.substring(videoFilePath.lastIndexOf("."));
			new File(videoFilePath).renameTo(new File(newFileName));
			String CMD_HEVC_264 = "ffmpeg -i %s -c:v libx264 -crf 20 %s";
			cmd = String.format(CMD_HEVC_264, newFileName, videoFilePath);
			ProcessUtils.executeCommand(cmd, false);
			new File(newFileName).delete();
		}

		final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s  -vcodec copy -acodec copy -bsf:v h264_mp4toannexb %s";
		final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";

		String tsPath = tsFolder + "/" + Constants.TS_NAME;
		//生成.ts
		cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
		ProcessUtils.executeCommand(cmd, false);
		//生成索引文件.m3u8 和切片.ts
		cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
		ProcessUtils.executeCommand(cmd, false);
		//删除index.ts
		new File(tsPath).delete();
	}
	private void union(String fileori , String filepath , String filename , boolean delsource){
		File fileoi = new File(fileori);
		if(!fileoi.exists()){
			throw new BusinessException("目录不存在");
		}
		File filelist[] = fileoi.listFiles();
		File tar = new File(filepath);
		try {
			RandomAccessFile write = new RandomAccessFile(tar,"rw");
			byte[] b = new byte[1024 * 10];
			for(int i = 0 ; i < filelist.length ; i++) {
				try {
					int len = -1;
					File filechunk = new File(fileori + File.separator + i);
					RandomAccessFile read = new RandomAccessFile(filechunk, "r");
					while ((len = read.read(b)) != -1) {
						write.write(b, 0, len);
					}
				} finally {
					write.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessException("合并文件失败");
		}finally {
			if(delsource){
				if(fileoi.exists()){
					try {
						FileUtils.deleteDirectory(fileoi);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
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

	@Override
	@Transactional(rollbackFor = Exception.class)
	public FileInfo newfolder(String userid , String foldername , String filepid){
		checkfilename(userid,foldername,filepid,FileFolderTypeEnums.FOLDER.getType());
		Date date = new Date();

		FileInfo fileInfo = new FileInfo();
		fileInfo.setCreateTime(date);
		fileInfo.setLastUpdateTime(date);
		fileInfo.setStatus(FileStatusEnums.USING.getStatus());
		fileInfo.setFileId(StringTools.getRandomNumber(5));
		fileInfo.setFilePid(filepid);
		fileInfo.setUserId(userid);
		fileInfo.setFileName(foldername);
		fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
		fileInfoMapper.insert(fileInfo);

		return fileInfo;

	}
	private void checkfilename(String userid , String foldername , String filepid,Integer folderType ){
		FileInfoQuery query = new FileInfoQuery();
		query.setUserId(userid);
		query.setFilePid(filepid);
		query.setFileName(foldername);
		query.setFolderType(folderType);
		query.setDelFlag(FileDelFlagEnums.USING.getFlag());
		Integer count =  fileInfoService.findCountByParam(query);
		if(count > 0)throw new BusinessException("存在同名文件");
	}
	@Override
	@Transactional(rollbackFor = Exception.class)
	public FileInfo rename(String userid,String filename,String fileId){
		FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId,userid);
		if(fileInfo == null){
			throw new BusinessException(ResponseCodeEnum.CODE_404);
		}
		if(fileInfo.getFileName().equals(filename)){
			return fileInfo;
		}
		checkfilename(userid,filename,fileInfo.getFilePid(),fileInfo.getFolderType());
		filename = filename + StringTools.getFileNameNoSuffix(fileInfo.getFileName());

		FileInfo dbinsert = new FileInfo();
		Date date = new Date();
		dbinsert.setLastUpdateTime(date);
		dbinsert.setFileName(filename);
		fileInfoMapper.updateByFileIdAndUserId(dbinsert,fileId,userid);
		fileInfo.setFileName(filename);
		fileInfo.setLastUpdateTime(date);
		return fileInfo;
	}
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void changeFileFolder(String fileids , String filepid, String userid){
		if(fileids.equals(filepid)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		FileInfoQuery query = new FileInfoQuery();
		if(!filepid.equals(Constants.ZERO_STR)){
			query.setFilePid(filepid);
			query.setUserId(userid);
			Integer count = fileInfoService.findCountByParam(query);
			if(count==0)throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		String[] filelist = fileids.split(",");
		query = new FileInfoQuery();
		query.setUserId(userid);
		query.setFilePid(filepid);
		query.setDelFlag(FileDelFlagEnums.USING.getFlag());
		List<FileInfo> fileInfos = fileInfoService.findListByParam(query);
		Map<String , FileInfo> map = fileInfos.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));

		query = new FileInfoQuery();
		query.setFileidArray(filelist);
		query.setUserId(userid);
		List<FileInfo> filetar= fileInfoService.findListByParam(query);
		for(FileInfo cur : filetar){
			FileInfo fileInfo = new FileInfo();
			fileInfo.setFilePid(filepid);
			if(map.containsKey(cur.getFileName())){
				cur.setFileName(StringTools.rename(cur.getFileName()));
				fileInfo.setFileName(cur.getFileName());
			}
			fileInfoMapper.updateByFileIdAndUserId(fileInfo,cur.getFileId(),userid);
		}
	}

}