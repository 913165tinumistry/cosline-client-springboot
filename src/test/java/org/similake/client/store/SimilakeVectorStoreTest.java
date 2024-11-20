package org.similake.client.store;

import com.squareup.okhttp.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.similake.client.model.Distance;
import org.similake.client.properties.SimilakeProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.ResponseEntity.ok;

@ExtendWith(MockitoExtension.class)
@Testcontainers
class SimilakeVectorStoreTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private RestTemplate restTemplate;

    private SimilakeProperties similakeProperties;

    private SimilakeVectorStore vectorStore;

    private static final int CONTAINER_PORT = 6767;

    @Mock
    private Environment environment;

    @Container
    static GenericContainer<?> similakemContainer = new GenericContainer<>(
            DockerImageName.parse("tinumistry/similake"))

            .withExposedPorts(CONTAINER_PORT);

    @BeforeEach
    void init() throws IOException {
        Integer mappedPort = similakemContainer.getMappedPort(CONTAINER_PORT);
        similakeProperties = new SimilakeProperties();
        similakeProperties.setHost("localhost");
        similakeProperties.setPort(mappedPort);
        similakeProperties.setCollectionName("vector_store");
        similakeProperties.setDistance(Distance.valueOf("Cosine"));
        similakeProperties.setInitializeSchema(true);
        similakeProperties.setApiKey("test-api-key");
        // Initialize your vectorStore with the mock and container URL
        vectorStore = new SimilakeVectorStore(embeddingModel, similakeProperties);
        ResponseEntity<String> vectorStore1 = createVectorStore(mappedPort);
        System.out.println(vectorStore1);
        System.out.println("Vector store created successfully");
    }

    public ResponseEntity<String> createVectorStore(Integer mappedPort) throws IOException {
        com.squareup.okhttp.MediaType mediattpe = com.squareup.okhttp.MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediattpe, "{\r\n    \"size\": 1536,\r\n    \"distance\": \"Cosine\",\r\n    \"persist\" : \"true\"\r\n  }");
        Request request = new Request.Builder()
                .url("http://localhost:"+mappedPort+"/collections/vector_store")
                .method("PUT", body)
                .addHeader("api-key", "1234")
                .addHeader("Content-Type", "application/json")
                .build();
        Response reponse = new OkHttpClient().newCall(request).execute();
        return ResponseEntity.ok(reponse.body().string());
    }

    @Test
    void testAdd_SingleDocument() {
        Document document = new Document(
                "2d8f1c4b-517d-46ec-924c-9f5fed79bf89",  // id
                "test-content",                           // content
                Collections.emptyMap()                    // metadata
        );
        List<Document> documents = new ArrayList<>();
        documents.add(document);

        // Mock embedding generation
        float[] embedding = new float[1536];
        Arrays.fill(embedding, 0.1f);
        doReturn(embedding).when(embeddingModel).embed(any(Document.class));
        vectorStore.add(documents);

        ResponseEntity<List<Document>> vectorStorePayloads = getVectorStorePayloads(similakeProperties.getHost(), similakeProperties.getPort(), similakeProperties.getCollectionName());
        assertEquals(1, vectorStorePayloads.getBody().size());
        System.out.println(vectorStorePayloads.getBody().get(0).getEmbedding().length);
    }


    public ResponseEntity<List<Document>> getVectorStorePayloads(String host, int port, String collectionName) {
        String baseUrl = String.format("http://%s:%d", host, port);
        String payloadsUrl = baseUrl + "/collections/" + collectionName + "/payloads";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> requestEntity = new HttpEntity<>("", headers);

        return restTemplate.exchange(
                payloadsUrl,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<List<Document>>() {}
        );
    }
    // @Test
    void testCosineSimilarity() {
        // Test valid vectors
        float[] vector1 = new float[]{1.0f, 0.0f};
        float[] vector2 = new float[]{1.0f, 0.0f};
        assertEquals(1.0, SimilakeVectorStore.cosineSimilarity(vector1, vector2), 0.0001);

        // Test orthogonal vectors
        float[] vector3 = new float[]{1.0f, 0.0f};
        float[] vector4 = new float[]{0.0f, 1.0f};
        assertEquals(0.0, SimilakeVectorStore.cosineSimilarity(vector3, vector4), 0.0001);

        // Test different length vectors
        float[] vector5 = new float[]{1.0f};
        float[] vector6 = new float[]{1.0f, 0.0f};
        assertThrows(IllegalArgumentException.class, () ->
                SimilakeVectorStore.cosineSimilarity(vector5, vector6)
        );

        // Test null vectors
        assertThrows(RuntimeException.class, () ->
                SimilakeVectorStore.cosineSimilarity(null, vector1)
        );
    }

    //@Test
    void testDoSimilaritySearch() {
        // Arrange
        String query = "test query";
        float[] queryEmbedding = new float[]{0.1f, 0.2f, 0.3f};
        when(embeddingModel.embed(query)).thenReturn(queryEmbedding);

        Document doc1 = new Document("content1");
        doc1.setEmbedding(new float[]{0.1f, 0.2f, 0.3f});
        Document doc2 = new Document("content2");
        doc2.setEmbedding(new float[]{0.4f, 0.5f, 0.6f});

        List<Document> mockResponse = Arrays.asList(doc1, doc2);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        // Act
        SearchRequest request = SearchRequest.defaults()
                .withQuery(query)
                .withTopK(2)
                .withSimilarityThreshold(0.5f);
        List<Document> results = vectorStore.doSimilaritySearch(request);

        // Assert
        assertNotNull(results);
        assertTrue(results.size() > 0);
        verify(embeddingModel).embed(query);
    }

    //@Test
    void testDoSimilaritySearchWithFilter() {
        // Arrange
        String query = "test query";
        float[] queryEmbedding = new float[]{0.1f, 0.2f, 0.3f};
        when(embeddingModel.embed(query)).thenReturn(queryEmbedding);

        Filter.Expression filterExpression = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("category"),
                new Filter.Value("test")
        );

        Document doc1 = new Document("content1");
        doc1.setEmbedding(new float[]{0.1f, 0.2f, 0.3f});

        List<Document> mockResponse = Arrays.asList(doc1);
        when(restTemplate.exchange(
                contains("metadata.category.eq=test"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        // Act
        SearchRequest request = SearchRequest.defaults()
                .withQuery(query)
                .withFilterExpression(filterExpression);
        List<Document> results = vectorStore.doSimilaritySearch(request);

        // Assert
        assertNotNull(results);
        verify(embeddingModel).embed(query);
        verify(restTemplate).exchange(
                contains("metadata.category.eq=test"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        );
    }

    //s @Test
    void testGetDocumentsFromApi() {
        // Arrange
        Document doc1 = new Document("content1");
        List<Document> mockResponse = Arrays.asList(doc1);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // Act
        SearchRequest request = SearchRequest.defaults();
        List<Document> results = vectorStore.getDocumentsFromApi(request);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(doc1.getId(), results.get(0).getId());
    }

    //s@Test
    void testDotProduct() {
        // Test valid vectors
        float[] vector1 = new float[]{1.0f, 2.0f, 3.0f};
        float[] vector2 = new float[]{4.0f, 5.0f, 6.0f};
        float expectedDotProduct = 1.0f * 4.0f + 2.0f * 5.0f + 3.0f * 6.0f;
        assertEquals(expectedDotProduct, SimilakeVectorStore.dotProduct(vector1, vector2), 0.0001);

        // Test different length vectors
        float[] vector3 = new float[]{1.0f, 2.0f};
        assertThrows(IllegalArgumentException.class, () ->
                SimilakeVectorStore.dotProduct(vector1, vector3)
        );
    }

    //@Test
    void testNorm() {
        // Test valid vector
        float[] vector = new float[]{3.0f, 4.0f};
        float expectedNorm = 25.0f; // 3^2 + 4^2 = 25
        assertEquals(expectedNorm, SimilakeVectorStore.norm(vector), 0.0001);

        // Test zero vector
        float[] zeroVector = new float[]{0.0f, 0.0f};
        assertEquals(0.0f, SimilakeVectorStore.norm(zeroVector), 0.0001);
    }
}