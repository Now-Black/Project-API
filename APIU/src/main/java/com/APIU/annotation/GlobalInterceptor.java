package com.APIU.annotation;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface GlobalInterceptor {
    boolean checkParams() default false;
    boolean checklogin() default false;
    boolean checkAdmin() default false;

}
