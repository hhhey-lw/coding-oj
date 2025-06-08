package com.longcoding.top;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.longcoding.top")
public class LongojChatEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(LongojChatEndApplication.class, args);
    }

}
