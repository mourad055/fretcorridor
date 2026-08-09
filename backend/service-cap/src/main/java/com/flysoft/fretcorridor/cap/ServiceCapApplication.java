package com.flysoft.fretcorridor.cap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServiceCapApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceCapApplication.class, args);
    }
}
