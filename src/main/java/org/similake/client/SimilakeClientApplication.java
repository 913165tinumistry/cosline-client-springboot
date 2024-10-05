package org.similake.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@SpringBootApplication
public class SimilakeClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimilakeClientApplication.class, args);
    }

}
