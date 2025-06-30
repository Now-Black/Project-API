package com.APIU.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.APIU.component.RedisComponent;
import com.APIU.entity.config.AppConfig;
import com.APIU.entity.constants.Constants;
import com.APIU.entity.dto.SysSettingsDto;
import com.APIU.entity.po.UserInfo;
import com.APIU.entity.query.UserInfoQuery;
import com.APIU.exception.BusinessException;
import com.APIU.mappers.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.APIU.entity.enums.PageSize;
import com.APIU.entity.query.EmailCodeQuery;
import com.APIU.entity.po.EmailCode;
import com.APIU.entity.vo.PaginationResultVO;
import com.APIU.entity.query.SimplePage;
import com.APIU.mappers.EmailCodeMapper;
import com.APIU.service.EmailCodeService;
import com.APIU.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 邮箱验证码 业务接口实现
 */
@Service("emailCodeService")
public class EmailCodeServiceImpl implements EmailCodeService {
	@Resource
	private AppConfig appConfig;
	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
	@Resource
	private RedisComponent redisComponent;
	@Resource
	private EmailCodeMapper<EmailCode, EmailCodeQuery> emailCodeMapper;

	@Resource
	private JavaMailSender javaMailSender;
	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<EmailCode> findListByParam(EmailCodeQuery param) {
		return this.emailCodeMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(EmailCodeQuery param) {
		return this.emailCodeMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<EmailCode> findListByPage(EmailCodeQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<EmailCode> list = this.findListByParam(param);
		PaginationResultVO<EmailCode> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(EmailCode bean) {
		return this.emailCodeMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<EmailCode> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.emailCodeMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<EmailCode> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.emailCodeMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(EmailCode bean, EmailCodeQuery param) {
		StringTools.checkParam(param);
		return this.emailCodeMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(EmailCodeQuery param) {
		StringTools.checkParam(param);
		return this.emailCodeMapper.deleteByParam(param);
	}

	/**
	 * 根据EmailAndCode获取对象
	 */
	@Override
	public EmailCode getEmailCodeByEmailAndCode(String email, String code) {
		return this.emailCodeMapper.selectByEmailAndCode(email, code);
	}

	/**
	 * 根据EmailAndCode修改
	 */
	@Override
	public Integer updateEmailCodeByEmailAndCode(EmailCode bean, String email, String code) {
		return this.emailCodeMapper.updateByEmailAndCode(bean, email, code);
	}

	/**
	 * 根据EmailAndCode删除
	 */
	@Override
	public Integer deleteEmailCodeByEmailAndCode(String email, String code) {
		return this.emailCodeMapper.deleteByEmailAndCode(email, code);
	}

	private void sendEmailCode(String email, String code){
		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,true);
			helper.setFrom(appConfig.getSendUserName());
			helper.setTo(email);
			SysSettingsDto sysSettingsDto = redisComponent.getSysSettingsDto();
			helper.setSubject(sysSettingsDto.getRegisterEmailTitle());
			helper.setText(sysSettingsDto.getRegisterEmailContent(),code);
			helper.setSentDate(new Date());
			javaMailSender.send(mimeMessage);
		} catch (MessagingException e) {
			e.printStackTrace();
			throw new BusinessException("邮箱发生失败");
		}

	}
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void sendEmailCode(String email , Integer type){
		if(type.equals(Constants.ZERO)){
			UserInfo userInfo =   userInfoMapper.selectByEmail(email);
			if(userInfo != null){
				throw new BusinessException("邮箱已存在");
			}
		}
		String code = StringTools.getRandomNumber(5);

		sendEmailCode(email,code);
		emailCodeMapper.disableEmailCode(email);
		EmailCode emailCode = new EmailCode();
		emailCode.setEmail(email);
		emailCode.setCode(code);
		emailCode.setCreateTime(new Date());
		emailCode.setStatus(Constants.ZERO);

		emailCodeMapper.insert(emailCode);
	}
}