package com.longcoding.top;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@MapperScan("com.longcoding.top.mapper")
@SpringBootApplication(scanBasePackages = "com.longcoding.top")
public class LongojChatEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(LongojChatEndApplication.class, args);
    }

}
