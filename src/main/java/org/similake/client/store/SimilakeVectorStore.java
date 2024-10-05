package org.similake.client.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SimilakeVectorStore {

    private static final Logger logger = LoggerFactory.getLogger(SimilakeVectorStore.class);
    protected EmbeddingModel embeddingModel;
    protected Map<String, Document> store = new ConcurrentHashMap<>();
    private double[] coordinates;
    public SimilakeVectorStore(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public void add(List<Document> documents) {
        for (Document document : documents) {
            logger.info("Calling EmbeddingModel for document id = {}", document.getId());
            float[] embedding = this.embeddingModel.embed(document);
            document.setEmbedding(embedding);
           // this.store.put(document.getId(), document);
            sendDocumentToApi(document);
        }
    }

    public List<Document> doSimilaritySearchlocal(SearchRequest request) {
        if (request.getFilterExpression() != null) {
            throw new UnsupportedOperationException(
                    "The [" + this.getClass() + "] doesn't support metadata filtering!");
        }

        float[] userQueryEmbedding = getUserQueryEmbedding(request.getQuery());
        return this.store.values()
                .stream()
                .map(entry -> new Similarity(entry.getId(),
                        cosineSimilarity(userQueryEmbedding, entry.getEmbedding())))
                .filter(s -> s.score >= request.getSimilarityThreshold())
                .sorted(Comparator.<Similarity>comparingDouble(s -> s.score).reversed())
                .limit(request.getTopK())
                .map(s -> this.store.get(s.key))
                .toList();
    }


    private float[] getUserQueryEmbedding(String query) {
        return this.embeddingModel.embed(query);
    }

    public static double cosineSimilarity(float[] vectorX, float[] vectorY) {
        if (vectorX == null || vectorY == null) {
            throw new RuntimeException("Vectors must not be null");
        }
        if (vectorX.length != vectorY.length) {
            throw new IllegalArgumentException("Vectors lengths must be equal");
        }
        float dotProduct = dotProduct(vectorX, vectorY);
        float normX = norm(vectorX);
        float normY = norm(vectorY);
        if (normX == 0 || normY == 0) {
            throw new IllegalArgumentException("Vectors cannot have zero norm");
        }
        return dotProduct / (Math.sqrt(normX) * Math.sqrt(normY));
    }

    public static float norm(float[] vector) {
        return dotProduct(vector, vector);
    }

    public static float dotProduct(float[] vectorX, float[] vectorY) {
        if (vectorX.length != vectorY.length) {
            throw new IllegalArgumentException("Vectors lengths must be equal");
        }
        float result = 0;
        for (int i = 0; i < vectorX.length; ++i) {
            result += vectorX[i] * vectorY[i];
        }
        return result;
    }

    public static class Similarity {
        private String key;
        private double score;

        public Similarity(String key, double score) {
            this.key = key;
            this.score = score;
        }

    }

    private void sendDocumentToApi(Document document) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:6767/collections/vector_store/payload";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Document> request = new HttpEntity<>(document, headers);
        // Log the request
        logger.info("Sending request to API: {}", request.getBody());
        restTemplate.postForEntity(url, request, String.class);
    }

    public List<Document> doSimilaritySearch(SearchRequest request) {
        Collection<Document> documents = getDocumentsFromApi();

        Map<String, Document> docuemntMap = documents.stream().collect(Collectors.toMap(Document::getId, Function.identity()));

        logger.info("Received {} documents from API", documents);
        float[] userQueryEmbedding = getUserQueryEmbedding(request.getQuery());
        List<Document> list = documents
                .stream()
                .map(entry -> new Similarity(entry.getId(),
                        cosineSimilarity(userQueryEmbedding, entry.getEmbedding())))
                .filter(s -> {
                    logger.info("Document ID: {}, Score: {}", s.key, s.score);
                    return s.score >= request.getSimilarityThreshold();
                })


                .sorted(Comparator.<Similarity>comparingDouble(s -> s.score).reversed())
                .limit(1)
                .map(s -> docuemntMap.get(s.key))
                .toList();

        logger.info("Returning {} documents from similarity search ", list);
        return list;

    }

    public List<Document> getDocumentsFromApi() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:6767/collections/vector_store/payloads";

        ResponseEntity<List<Document>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Document>>() {}
        );

        return response.getBody();
    }
}