package org.similake.client.config;

import org.similake.client.model.Distance;
import org.similake.client.properties.SimilakeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SimilakeConfig {

    private static final Logger logger = LoggerFactory.getLogger(SimilakeConfig.class);

    //@Autowired
    SimilakeProperties similakeProperties;
    public SimilakeConfig(Environment env) {
        initprops(env);
    }
    public void initprops(Environment env) {
        String host = env.getProperty("spring.ai.vectorstore.similake.host");
        String port = env.getProperty("spring.ai.vectorstore.similake.port");
        String collectionName = env.getProperty("spring.ai.vectorstore.similake.collection-name");
        String initializeSchema = env.getProperty("spring.ai.vectorstore.similake.initialize-schema");
        String dimension = env.getProperty("spring.ai.vectorstore.similake.dimension");
        String distance = env.getProperty("spring.ai.vectorstore.similake.distance");

        logger.info("Host: {}", host);
        logger.info("Port: {}", port);
        logger.info("Collection Name: {}", collectionName);
        logger.info("Initialize Schema: {}", initializeSchema);
        logger.info("Dimension: {}", dimension);
        logger.info("Distance: {}", distance);
        similakeProperties = new SimilakeProperties();
        similakeProperties.setHost(env.getProperty("spring.ai.vectorstore.similake.host"));
        similakeProperties.setPort(Integer.parseInt(env.getProperty("spring.ai.vectorstore.similake.port")));
        similakeProperties.setCollectionName(env.getProperty("spring.ai.vectorstore.similake.collection-name"));
        similakeProperties.setInitializeSchema(Boolean.parseBoolean(env.getProperty("spring.ai.vectorstore.similake.initialize-schema")));
        similakeProperties.setDimension(Integer.parseInt(env.getProperty("spring.ai.vectorstore.similake.dimension")));
        similakeProperties.setDistance(Distance.valueOf(env.getProperty("spring.ai.vectorstore.similake.distance")));
        similakeProperties.setApiKey(env.getProperty("spring.ai.vectorstore.similake.api-key"));

        if(initializeSchema != null && initializeSchema.equalsIgnoreCase("true")) {
            init();
        } else {
            logger.info("Properties not initialized for initialization = true");
        }
    }

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
