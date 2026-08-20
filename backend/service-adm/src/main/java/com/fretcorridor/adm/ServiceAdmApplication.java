package com.fretcorridor.adm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServiceAdmApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAdmApplication.class, args);
    }
}
