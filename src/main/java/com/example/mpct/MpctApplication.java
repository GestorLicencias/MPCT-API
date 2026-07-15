package com.example.mpct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@org.springframework.scheduling.annotation.EnableScheduling
public class MpctApplication {

    public static void main(String[] args) {
        SpringApplication.run(MpctApplication.class, args);
    }

}
