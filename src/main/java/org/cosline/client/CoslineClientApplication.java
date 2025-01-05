package org.cosline.client;

import org.cosline.client.properties.CoslineProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties(CoslineProperties.class)
public class CoslineClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoslineClientApplication.class, args);
    }

}
