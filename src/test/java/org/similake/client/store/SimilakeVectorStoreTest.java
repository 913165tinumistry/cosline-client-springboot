package org.similake.client.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.similake.client.properties.SimilakeProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimilakeVectorStoreTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SimilakeProperties similakeProperties;

    private SimilakeVectorStore vectorStore;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(similakeProperties.getHost()).thenReturn("localhost");
        when(similakeProperties.getPort()).thenReturn(6767);
        when(similakeProperties.getCollectionName()).thenReturn("vector_store");

        vectorStore = new SimilakeVectorStore(embeddingModel, similakeProperties);
    }

    @Test
    void testAdd_SingleDocument() {
        // Arrange
        Document document = new Document("test-content");
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        when(embeddingModel.embed(document)).thenReturn(embedding);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));

        // Act
        List<Document> documents = Arrays.asList(document);
        vectorStore.add(documents);

        // Assert
        verify(embeddingModel).embed(document);
        verify(restTemplate).postForEntity(contains("/collections/vector_store/payload"), any(), eq(String.class));
        assertEquals(embedding, document.getEmbedding());
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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