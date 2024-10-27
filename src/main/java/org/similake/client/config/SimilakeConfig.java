package org.similake.client.config;

import org.similake.client.model.Distance;
import org.similake.client.properties.SimilakeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SimilakeConfig {

    private static final Logger logger = LoggerFactory.getLogger(SimilakeConfig.class);

    @Autowired
    SimilakeProperties similakeProperties;

    @Bean
    public String init() {
        System.out.println("********************Initializing SimilakeConfig");
        logger.info("********************Initializing SimilakeConfig");
        String returnMessage = "Properties not created";
        if (similakeProperties.isInitializeSchema()) {
            String collectionName = similakeProperties.getCollectionName();
            int dimension = similakeProperties.getDimension();
            String distanceType = String.valueOf(similakeProperties.getDistance());
            String apiKey = similakeProperties.getApiKey();
            String requestPayload = String.format("{\"size\": %d, \"distance\": \"%s\"}", dimension, distanceType);
            logger.info("Request payload: {}", requestPayload);
            logger.info("Collection name: {}", collectionName);
            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", apiKey);
            headers.set("Content-Type", "application/json");
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(requestPayload, headers);
            String url = String.format("http://%s:%d/collections/%s",
                    similakeProperties.getHost(),
                    similakeProperties.getPort(),
                    similakeProperties.getCollectionName());
            logger.info("URL: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    String.class);
            returnMessage = response.getBody();

        } else {
            returnMessage = "Properties not created";
        }
        logger.info("Similake: {}", returnMessage);
        return returnMessage;
    }
}
