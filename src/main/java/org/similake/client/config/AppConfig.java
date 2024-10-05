package org.similake.client.config;


import org.similake.client.store.SimilakeVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public SimilakeVectorStore cosDBVectorStore(EmbeddingModel embeddingModel) {
        return new SimilakeVectorStore(embeddingModel);
    }


}
