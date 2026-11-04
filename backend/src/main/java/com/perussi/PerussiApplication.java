package com.perussi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class PerussiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerussiApplication.class, args);
    }
}
