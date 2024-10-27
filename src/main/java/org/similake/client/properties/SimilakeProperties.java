package org.similake.client.properties;

import org.similake.client.model.Distance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ConfigurationProperties(prefix = "spring.ai.vectorstore.similake")
public class SimilakeProperties {
    @Autowired
    private Environment env;
    private String host;


    private int port;
    private String collectionName;
    private boolean initializeSchema;
    private int dimension;
    private Distance distance;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    private String apiKey;


    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    // Getters and setters
    public String getHost() {
        String envhost = env.getProperty("spring.ai.vectorstore.similake.host");
        host = envhost;
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public boolean isInitializeSchema() {
        return initializeSchema;
    }

    public void setInitializeSchema(boolean initializeSchema) {
        this.initializeSchema = initializeSchema;
    }

    public Distance getDistance() {
        return distance;
    }

    public void setDistance(Distance distance) {
        this.distance = distance;
    }

    @Override
    public String toString() {
        return "QdrantProperties{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", collectionName='" + collectionName + '\'' +
                ", initializeSchema=" + initializeSchema +
                '}';
    }
}
