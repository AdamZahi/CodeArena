package com.codearena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@SpringBootApplication
@EnableScheduling
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)

public class CodeArenaApplication {

    /**
     * Starts the Code Arena backend application.
     *
     * @param args startup arguments
     */
    public static void main(String[] args) {
        // TODO: Add startup hooks and bootstrap tasks.
        SpringApplication.run(CodeArenaApplication.class, args);
        System.out.println("Code Arena backend is running...");
    }
}
