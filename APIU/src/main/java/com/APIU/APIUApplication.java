package com.APIU;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@EnableAsync
@SpringBootApplication(scanBasePackages = {"com.APIU"})
@EnableTransactionManagement
@EnableScheduling
@MapperScan("com.APIU.mappers")
public class APIUApplication {
    public static void main(String[] args) {
        SpringApplication.run(APIUApplication.class,args);
    }
}

/*这是第一次修改*/
/*这是第二次修改*/
/*这是第三次修改*/