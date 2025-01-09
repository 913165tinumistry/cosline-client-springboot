package org.cosline.client.config;

import org.cosline.client.model.Distance;
import org.cosline.client.properties.CoslineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CoslineConfig {

    private static final Logger logger = LoggerFactory.getLogger(CoslineConfig.class);

    //@Autowired
    CoslineProperties coslineProperties;
    public CoslineConfig(Environment env) {
        String isApp = System.getenv("isApp");
        initprops(env);
    }
    public void initprops(Environment env) {
        String host = env.getProperty("spring.ai.vectorstore.cosline.host");
        String port = env.getProperty("spring.ai.vectorstore.cosline.port");
        String collectionName = env.getProperty("spring.ai.vectorstore.cosline.collection-name");
        String initializeSchema = env.getProperty("spring.ai.vectorstore.cosline.initialize-schema");
        String dimension = env.getProperty("spring.ai.vectorstore.cosline.dimension");
        String distance = env.getProperty("spring.ai.vectorstore.cosline.distance");

        logger.info("Host: {}", host);
        logger.info("Port: {}", port);
        logger.info("Collection Name: {}", collectionName);
        logger.info("Initialize Schema: {}", initializeSchema);
        logger.info("Dimension: {}", dimension);
        logger.info("Distance: {}", distance);
        coslineProperties = new CoslineProperties();
        coslineProperties.setHost(env.getProperty("spring.ai.vectorstore.cosline.host"));
        coslineProperties.setPort(Integer.parseInt(env.getProperty("spring.ai.vectorstore.cosline.port")));
        coslineProperties.setCollectionName(env.getProperty("spring.ai.vectorstore.cosline.collection-name"));
        coslineProperties.setInitializeSchema(Boolean.parseBoolean(env.getProperty("spring.ai.vectorstore.cosline.initialize-schema")));
        coslineProperties.setDimension(Integer.parseInt(env.getProperty("spring.ai.vectorstore.cosline.dimension")));
        coslineProperties.setDistance(Distance.valueOf(env.getProperty("spring.ai.vectorstore.cosline.distance")));
        coslineProperties.setApiKey(env.getProperty("spring.ai.vectorstore.cosline.api-key"));

        if(initializeSchema != null && initializeSchema.equalsIgnoreCase("true")) {
            init();
        } else {
            logger.info("Properties not initialized for initialization = true");
        }
    }

    @Bean
    public String init() {
        System.out.println("********************Initializing coslineConfig");
        logger.info("********************Initializing coslineConfig");
        String returnMessage = "Properties not created";
        if (coslineProperties.isInitializeSchema()) {
            String collectionName = coslineProperties.getCollectionName();
            int dimension = coslineProperties.getDimension();
            String distanceType = String.valueOf(coslineProperties.getDistance());
            String apiKey = coslineProperties.getApiKey();
            String requestPayload = String.format("{\"size\": %d, \"distance\": \"%s\"}", dimension, distanceType);
            logger.info("Request payload: {}", requestPayload);
            logger.info("Collection name: {}", collectionName);
            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", apiKey);
            headers.set("Content-Type", "application/json");
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(requestPayload, headers);
            String url = String.format("http://%s:%d/api/v1/collections/%s",
                    coslineProperties.getHost(),
                    coslineProperties.getPort(),
                    coslineProperties.getCollectionName());
            logger.info("URL: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class);
            returnMessage = response.getBody();

        } else {
            returnMessage = "Properties not created";
        }
        logger.info("cosline: {}", returnMessage);
        return returnMessage;
    }
}
