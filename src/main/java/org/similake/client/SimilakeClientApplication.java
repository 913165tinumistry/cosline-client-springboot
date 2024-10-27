package org.similake.client;

import org.similake.client.properties.SimilakeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties(SimilakeProperties.class)
public class SimilakeClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimilakeClientApplication.class, args);
    }

}
