package com.im.square;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication(scanBasePackages = {"com.im.square", "com.im.common"})
public class SquareApplication {
    public static void main(String[] args) {
        SpringApplication.run(SquareApplication.class, args);
    }
}
