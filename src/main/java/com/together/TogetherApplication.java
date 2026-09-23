package com.together;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.together.mapper")
public class TogetherApplication {

    public static void main(String[] args) {
        SpringApplication.run(TogetherApplication.class, args);
    }
}
