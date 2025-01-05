package org.cosline.client;

import org.cosline.client.filtercriteria.CoslineFilterExpressionConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.Filter.ExpressionType;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilterExpressionTest {

    private CoslineFilterExpressionConverter converter;
    private Filter.Expression leftExpression;

    @BeforeEach
    void setUp() {
        converter = new CoslineFilterExpressionConverter();
        leftExpression = new Filter.Expression(
                ExpressionType.EQ,
                new Filter.Key("brand"),
                new Filter.Value("Apple")
        );
    }

    @Test
    void testSingleCondition() {
        // Given
        SearchRequest searchRequest = SearchRequest.query("test")
                .withTopK(5)
                .withFilterExpression(leftExpression);

        // When
        String queryParams = converter.convertToQueryParams(searchRequest.getFilterExpression());

        // Then
        assertEquals("metadata.brand.eq=Apple", queryParams);
    }

    @Test
    void testMultipleConditions() {
        // Given
        Filter.Expression multipleExp = new Filter.Expression(
                ExpressionType.AND,
                leftExpression,
                new Filter.Expression(
                        ExpressionType.EQ,
                        new Filter.Key("product_name"),
                        new Filter.Value("Laptop")
                )
        );

        SearchRequest searchRequest = SearchRequest.query("test")
                .withTopK(5)
                .withFilterExpression(multipleExp);

        // When
        String queryParams = converter.convertToQueryParams(searchRequest.getFilterExpression());

        // Then
        assertEquals("metadata.brand.eq=Apple&metadata.product_name.eq=Laptop", queryParams);
    }

    @Test
    void testComplexConditions() {
        // Given
        Filter.Expression complexExp = new Filter.Expression(
                ExpressionType.AND,
                leftExpression,
                new Filter.Expression(
                        ExpressionType.AND,
                        new Filter.Expression(
                                ExpressionType.GT,
                                new Filter.Key("price"),
                                new Filter.Value(1000)
                        ),
                        new Filter.Expression(
                                ExpressionType.LT,
                                new Filter.Key("quantity"),
                                new Filter.Value(10)
                        )
                )
        );

        SearchRequest searchRequest = SearchRequest.query("test")
                .withTopK(5)
                .withFilterExpression(complexExp);

        // When
        String queryParams = converter.convertToQueryParams(searchRequest.getFilterExpression());

        // Then
        assertEquals("metadata.brand.eq=Apple&metadata.price.gt=1000&metadata.quantity.lt=10", queryParams);
    }

    @Test
    void testInOperator() {
        // Given
        Filter.Expression inExp = new Filter.Expression(
                ExpressionType.AND,
                new Filter.Expression(
                        ExpressionType.IN,
                        new Filter.Key("brand"),
                        new Filter.Value(Arrays.asList("Apple", "Samsung"))
                ),
                new Filter.Expression(
                        ExpressionType.GT,
                        new Filter.Key("price"),
                        new Filter.Value(500)
                )
        );

        SearchRequest searchRequest = SearchRequest.query("test")
                .withTopK(5)
                .withFilterExpression(inExp);

        // When
        String queryParams = converter.convertToQueryParams(searchRequest.getFilterExpression());

        // Then
        assertEquals("metadata.brand.in=Apple,Samsung&metadata.price.gt=500", queryParams);
    }

    @Test
    void testOrConditions() {
        // Given
        Filter.Expression orExp = new Filter.Expression(
                ExpressionType.OR,
                leftExpression,
                new Filter.Expression(
                        ExpressionType.EQ,
                        new Filter.Key("brand"),
                        new Filter.Value("Samsung")
                )
        );

        SearchRequest searchRequest = SearchRequest.query("test")
                .withTopK(5)
                .withFilterExpression(orExp);

        // When
        String queryParams = converter.convertToQueryParams(searchRequest.getFilterExpression());

        // Then
        assertEquals("metadata.brand.eq=Apple&metadata.brand.eq=Samsung", queryParams);
    }
}