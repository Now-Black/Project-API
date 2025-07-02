package com.APIU.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import com.APIU.component.RedisComponent;
import com.APIU.component.RedisUtils;
import com.APIU.entity.config.AppConfig;
import com.APIU.entity.constants.Constants;
import com.APIU.entity.dto.SessionWebUserDto;
import com.APIU.entity.dto.SysSettingsDto;
import com.APIU.entity.dto.UserSpaceDto;
import com.APIU.entity.enums.UserStatusEnum;
import com.APIU.entity.po.EmailCode;
import com.APIU.entity.po.FileInfo;
import com.APIU.entity.query.EmailCodeQuery;
import com.APIU.entity.query.FileInfoQuery;
import com.APIU.exception.BusinessException;
import com.APIU.mappers.EmailCodeMapper;
import com.APIU.mappers.FileInfoMapper;
import com.APIU.service.EmailCodeService;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;

import com.APIU.entity.enums.PageSize;
import com.APIU.entity.query.UserInfoQuery;
import com.APIU.entity.po.UserInfo;
import com.APIU.entity.vo.PaginationResultVO;
import com.APIU.entity.query.SimplePage;
import com.APIU.mappers.UserInfoMapper;
import com.APIU.service.UserInfoService;
import com.APIU.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;



/**
 * 用户信息 业务接口实现
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {
	@Resource
	private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;
	@Resource
	private RedisUtils redisUtils;

	@Resource
	private EmailCodeMapper<EmailCode, EmailCodeQuery> emailCodeMapper;
	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
	@Resource
	private EmailCodeService emailCodeService;
	@Resource
	private RedisComponent redisComponent;
	@Resource
	private AppConfig appConfig;
	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserInfo> findListByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserInfo> list = this.findListByParam(param);
		PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserInfo bean) {
		return this.userInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserInfo bean, UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据UserId获取对象
	 */
	@Override
	public UserInfo getUserInfoByUserId(String userId) {
		return this.userInfoMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId修改
	 */
	@Override
	public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
		return this.userInfoMapper.updateByUserId(bean, userId);
	}

	/**
	 * 根据UserId删除
	 */
	@Override
	public Integer deleteUserInfoByUserId(String userId) {
		return this.userInfoMapper.deleteByUserId(userId);
	}

	/**
	 * 根据Email获取对象
	 */
	@Override
	public UserInfo getUserInfoByEmail(String email) {
		return this.userInfoMapper.selectByEmail(email);
	}

	/**
	 * 根据Email修改
	 */
	@Override
	public Integer updateUserInfoByEmail(UserInfo bean, String email) {
		return this.userInfoMapper.updateByEmail(bean, email);
	}

	/**
	 * 根据Email删除
	 */
	@Override
	public Integer deleteUserInfoByEmail(String email) {
		return this.userInfoMapper.deleteByEmail(email);
	}

	/**
	 * 根据NickName获取对象
	 */
	@Override
	public UserInfo getUserInfoByNickName(String nickName) {
		return this.userInfoMapper.selectByNickName(nickName);
	}

	/**
	 * 根据NickName修改
	 */
	@Override
	public Integer updateUserInfoByNickName(UserInfo bean, String nickName) {
		return this.userInfoMapper.updateByNickName(bean, nickName);
	}

	/**
	 * 根据NickName删除
	 */
	@Override
	public Integer deleteUserInfoByNickName(String nickName) {
		return this.userInfoMapper.deleteByNickName(nickName);
	}

	/**
	 * 根据QqOpenId获取对象
	 */
	@Override
	public UserInfo getUserInfoByQqOpenId(String qqOpenId) {
		return this.userInfoMapper.selectByQqOpenId(qqOpenId);
	}

	/**
	 * 根据QqOpenId修改
	 */
	@Override
	public Integer updateUserInfoByQqOpenId(UserInfo bean, String qqOpenId) {
		return this.userInfoMapper.updateByQqOpenId(bean, qqOpenId);
	}

	/**
	 * 根据QqOpenId删除
	 */
	@Override
	public Integer deleteUserInfoByQqOpenId(String qqOpenId) {
		return this.userInfoMapper.deleteByQqOpenId(qqOpenId);
	}


	@Override
	@Transactional(rollbackFor = Exception.class)
	public void register(String email , String nickName , String password , String emailCode){
		UserInfo userInfo = userInfoMapper.selectByEmail(email);
		if(userInfo != null){
			throw new BusinessException("邮箱已存在");
		}
		userInfo =  userInfoMapper.selectByNickName(nickName);
		if(userInfo != null){
			throw new BusinessException("用户名已存在");
		}
		emailCodeService.checkEmailCode(email,emailCode);
		String userid = StringTools.getRandomNumber(Constants.LENGTH_10);
		userInfo = new UserInfo();
		userInfo.setEmail(email);
		userInfo.setNickName(nickName);
		userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
		userInfo.setJoinTime(new Date());
		userInfo.setPassword(StringTools.encodeByMD5(password));
		userInfo.setUserId(userid);
		SysSettingsDto sysSettingsDto = redisComponent.getSysSettingsDto();
		userInfo.setTotalSpace(sysSettingsDto.getUserInitUseSpace() * Constants.MB);
		userInfo.setUseSpace(0L);
		userInfoMapper.insert(userInfo);
	}
	@Override
	@Transactional(rollbackFor = Exception.class)
	public SessionWebUserDto login(String email, String password){
		UserInfo userInfo = userInfoMapper.selectByEmail(email);
		if(userInfo == null){
			throw new BusinessException("用户不存在");
		}
		if(!userInfo.getPassword().equals(StringTools.encodeByMD5(password))){
			throw new BusinessException("密码输入错误");
		}
		if(userInfo.getStatus().equals(UserStatusEnum.DISABLE.getStatus())){
			throw new BusinessException("账号已禁用");
		}
		userInfo.setLastLoginTime(new Date());
		userInfoMapper.updateByEmail(userInfo,email);
		SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
		sessionWebUserDto.setNickName(userInfo.getNickName());
		sessionWebUserDto.setUserId(userInfo.getUserId());
		sessionWebUserDto.setAdmin(ArrayUtils.contains(appConfig.getAdminEmails().split(","), email));
		UserSpaceDto userSpaceDto = new UserSpaceDto();
		userSpaceDto.setUseSpace(fileInfoMapper.selectUseSpace(userInfo.getUserId()));
		userSpaceDto.setTotalSpace(userInfo.getTotalSpace());
		redisUtils.set(Constants.REDIS_KEY_USER_SPACE_USE,userSpaceDto);
		return sessionWebUserDto;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void resetpassword(String email,String password,String emailCode){
		emailCodeService.checkEmailCode(email,emailCode);
		UserInfo userInfo = userInfoMapper.selectByEmail(email);
		if(userInfo==null || userInfo.getStatus().equals(UserStatusEnum.DISABLE.getStatus())){
			throw new BusinessException("账号不存在或已被禁用");
		}
		UserInfo updateInfo = new UserInfo();
		updateInfo.setPassword(StringTools.encodeByMD5(password));
		userInfoMapper.updateByEmail(updateInfo,email);
	}

}