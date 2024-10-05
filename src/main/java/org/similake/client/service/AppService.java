package org.similake.client.service;

import org.similake.client.store.SimilakeVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AppService {

    private static final Logger logger = LoggerFactory.getLogger(AppService.class);

    @Autowired
    SimilakeVectorStore similakeVectorStore;

    ChatClient chatClient;

    public AppService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    List<Document> documents =
            List.of(
                    new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!", Map.of("country", "UK", "year", 2020)),
                    new Document("The World is Big and Salvation Lurks Around the Corner", Map.of("country", "BG", "year", 2018)),
                    new Document("You walk forward facing the past and you turn back toward the future.", Map.of("country", "NL", "year", 2023)),
                    new Document("Exploring the depths of the ocean is like diving into a new world of wonder.", Map.of("country", "USA", "year", 2019)),
                    new Document("Technology shapes the future but leaves our past behind.", Map.of("category", "Technology")),
                    new Document("The evolution of artificial intelligence is transforming industries.", Map.of("category", "Technology", "year", 2021)),
                    new Document("Mountains are the beginning and the end of all natural scenery.", Map.of("country", "CH", "year", 2022)),
                    new Document("Books are a uniquely portable magic.", Map.of("author", "Stephen King", "genre", "Literature")),
                    new Document("The stars are not afraid of the darkness; they only shine brighter.", Map.of("country", "AU", "year", 2021))
            );

    public String loadDocuments() {
        for (Document document : documents) {
            TextSplitter textSplitter = new TokenTextSplitter();
            List<Document> splitedDocuments = textSplitter.split(document);
            similakeVectorStore.add(splitedDocuments);
            logger.info("Added document: {}", document);
            if (document != documents.get(documents.size() - 1)) {
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return "documents loaded successfully";
    }

    public List<Document> search(String question) {
        return similakeVectorStore.doSimilaritySearch(SearchRequest.query(question).withTopK(1));
    }


}
