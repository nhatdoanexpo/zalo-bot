package com.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.bot")
@EnableScheduling
public class ZaloBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZaloBotApplication.class, args);
    }

}
