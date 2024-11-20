package org.similake.client.store;

import org.similake.client.filtercriteria.SimilakeFilterExpressionConverter;
import org.similake.client.properties.SimilakeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

//@Configuration
public class SimilakeVectorStore implements InitializingBean {

   // @Autowired
    SimilakeProperties similakeProperties;

    private static final Logger logger = LoggerFactory.getLogger(SimilakeVectorStore.class);
    protected EmbeddingModel embeddingModel;
    protected Map<String, Document> store = new ConcurrentHashMap<>();
    private double[] coordinates;
    public SimilakeVectorStore(EmbeddingModel embeddingModel,SimilakeProperties similakeProperties) {
        this.embeddingModel = embeddingModel;
        this.similakeProperties = similakeProperties;
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
        logger.info("VectorX length: {}, VectorY length: {}", vectorX.length, vectorY.length);
        logger.info("VectorX: {}", Arrays.toString(vectorX));
        logger.info("VectorY: {}", Arrays.toString(vectorY));
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

    @Override
    public void afterPropertiesSet() throws Exception {
           logger.info("SimilakeVectorStore initialized");
        String property = System.getProperty("spring.ai.vectorstore.similake.host");
        logger.info("SimilakeVectorStore initialized with property: {}", property);
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
        logger.info("similakeProperties.getHost(): {}", similakeProperties.getHost());
        String payloadUrl = "http://" +similakeProperties.getHost() + ":" + similakeProperties.getPort() + "/collections/" + similakeProperties.getCollectionName() + "/payload";
        logger.info("Sending request to URL: {}", payloadUrl);
        //String url = "http://localhost:6767/collections/vector_store/payload";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Document> request = new HttpEntity<>(document, headers);
        // Log the request
        logger.info("Sending request to API: {}", request.getBody());
        restTemplate.postForEntity(payloadUrl, request, String.class);
    }

    public List<Document> doSimilaritySearch(SearchRequest request) {
        Collection<Document> documents = getDocumentsFromApi(request);

        Map<String, Document> docuemntMap = documents.stream().collect(Collectors.toMap(Document::getId, Function.identity()));

        logger.info("Received {} documents from API", documents);
        float[] userQueryEmbedding = getUserQueryEmbedding(request.getQuery());
        logger.info("User query embedding: {}", userQueryEmbedding);
        List<Document> list = documents
                .stream()
                .map(entry -> new Similarity(entry.getId(),
                        cosineSimilarity(userQueryEmbedding, entry.getEmbedding())))
                .filter(s -> {
                    logger.info("Document ID: {}, Score: {}", s.key, s.score);
                    return s.score >= request.getSimilarityThreshold();
                })


                .sorted(Comparator.<Similarity>comparingDouble(s -> s.score).reversed())
                .limit(request.getTopK())
                .map(s -> docuemntMap.get(s.key))
                .toList();

        logger.info("Returning {} documents from similarity search ", list);
        return list;

    }

    public List<Document> getDocumentsFromApi(SearchRequest request) {
        SimilakeFilterExpressionConverter filterExpressionConverter = new SimilakeFilterExpressionConverter();
        RestTemplate restTemplate = new RestTemplate();
        String payloadUrl = "http://" + similakeProperties.getHost() + ":" + similakeProperties.getPort() + "/collections/" + similakeProperties.getCollectionName() + "/payloads";
        //String baseUrl = "http://localhost:6767/collections/vector_store/payloads";

        // Convert filter expression to query parameters
        String queryParams = "";
        if (request.getFilterExpression() != null) {
            queryParams = filterExpressionConverter.convertToQueryParams(request.getFilterExpression());
        }

        // Build full URL with query parameters
        String fullUrl = payloadUrl;
        if (!queryParams.isEmpty()) {
            fullUrl = payloadUrl + "?" + queryParams;
        }

        logger.info("Sending request to URL: {}", fullUrl);

        ResponseEntity<List<Document>> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Document>>() {}
        );

        return response.getBody();
    }


}
