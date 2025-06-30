package com.APIU.aspect;

import com.APIU.annotation.GlobalInterceptor;
import com.APIU.annotation.VerifyParam;
import com.APIU.entity.config.AppConfig;
import com.APIU.entity.constants.Constants;
import com.APIU.entity.dto.SessionWebUserDto;
import com.APIU.entity.enums.ResponseCodeEnum;
import com.APIU.entity.po.EmailCode;
import com.APIU.exception.BusinessException;
import com.APIU.utils.StringTools;
import com.APIU.utils.VerifyUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component("globalOperationAspect")
public class GlobalOperationAspect {

    @Resource
    private AppConfig appConfig;

    @Pointcut("@annotation(com.APIU.annotation.GlobalInterceptor)")
    private void requestInterceptor(){


    }
    @Before("requestInterceptor()")
    public void interceptorDo(JoinPoint point)throws BusinessException {
        try{
            Object target = point.getTarget();
            Object[] argument = point.getArgs();
            String methodName = point.getSignature().getName();
            Class<?>[] parameterTypes = ((MethodSignature)point.getSignature()).getMethod().getParameterTypes();
            Method method = target.getClass().getMethod(methodName,parameterTypes);
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if(null == interceptor)return;

            if(interceptor.checklogin() || interceptor.checkAdmin()){
                checklogin(interceptor.checkAdmin());
            }
            if(interceptor.checkParams()){
                validateParams(method,argument);
            }
        }catch (NoSuchMethodException e){
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }

    }


    private void checklogin(Boolean checkAdmin){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session =  request.getSession();
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto)session.getAttribute(Constants.SESSION_KEY);
        /*if(sessionWebUserDto ==null && appConfig.getDev()!=null && !appConfig.getDev()){
            开发环境下自动登录
        }*/
        if(sessionWebUserDto == null){
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        if(!sessionWebUserDto.getAdmin() && checkAdmin){
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }

    }
    private static final String TYPE_STRING = "java.lang.String";
    private static final String TYPE_INTEGER = "java.lang.Integer";
    private static final String TYPE_LONG = "java.lang.Long";
    private void validateParams(Method m , Object[] arguement){
        Parameter[] parameters = m.getParameters();
        for(int i = 0 ; i < parameters.length ; i++){
            Parameter parameter = parameters[i];
            /*parameter包含参数名称、参数类型- 参数注解 -参数在方法中的位置等元数据*/
            Object value = arguement[i];
            /*value 变量代表方法调用时传入的实际参数值：*/
            VerifyParam verifyParam  = parameter.getAnnotation(VerifyParam.class);
            if(verifyParam == null)continue;
            if(parameter.getParameterizedType().getTypeName().equals(TYPE_STRING) || parameter.getParameterizedType().getTypeName().equals(TYPE_LONG)||
                    parameter.getParameterizedType().getTypeName().equals(TYPE_INTEGER))
            {
                checkvalue(value,verifyParam);
            }else {
                checkobjvalue(value,parameter);
            }


        }
    }
    private void checkvalue(Object value , VerifyParam verifyParam){
        boolean isempty = value ==null || StringTools.isEmpty(value.toString());
        int length = value == null ? 0 : value.toString().length();

        if(isempty && verifyParam.required()){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if(!isempty &&(verifyParam.max()!=-1 && length>verifyParam.max() || verifyParam.min()!=-1 && length<verifyParam.min())  ){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if(!isempty && !StringTools.isEmpty(verifyParam.regex().getRegex()) && !VerifyUtils.verify(verifyParam.regex(),String.valueOf(value))){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }
    private void checkobjvalue(Object value , Parameter parameter){
        try {
            String typename = parameter.getParameterizedType().getTypeName();
            Class classz = Class.forName(typename);
            Field[] fields = classz.getFields();
            for(Field field : fields){
                VerifyParam verifyParam = field.getAnnotation(VerifyParam.class);
                if(verifyParam == null){
                    continue;
                }
                field.setAccessible(true);
                Object resultvalue = field.get(value);
                checkvalue(resultvalue,verifyParam);
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

}
