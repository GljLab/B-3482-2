package com.cliphub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cliphub.mapper")
public class ClipHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClipHubApplication.class, args);
    }
}
