package org.similake.client.main;

import org.similake.client.filtercriteria.SimilakeFilterExpressionConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.Filter.ExpressionType;

import java.util.Arrays;

public class FilterExample {
    private static final Logger logger = LoggerFactory.getLogger(FilterExample.class);

    public static void main(String[] args) {
        SimilakeFilterExpressionConverter converter = new SimilakeFilterExpressionConverter();

        // Single condition
        Filter.Expression singleExp = new Filter.Expression(
                        ExpressionType.EQ,
                        new Filter.Key("brand"),
                        new Filter.Value("Apple")
                );

        SearchRequest searchRequest = SearchRequest.query("test")
                .withTopK(5)
                .withFilterExpression(singleExp);
        logger.info("Single condition - Filter expression: {}", searchRequest.getFilterExpression());
        String queryParams = converter.convertToQueryParams(searchRequest.getFilterExpression());
        logger.info("Single condition - Query parameters: {}", queryParams);

        // Multiple conditions (brand=Apple AND product_name=Laptop)
        Filter.Expression multipleExp = new Filter.Expression(
                ExpressionType.AND,
                new Filter.Expression(
                        ExpressionType.EQ,
                        new Filter.Key("brand"),
                        new Filter.Value("Apple")
                ),
                new Filter.Expression(
                        ExpressionType.EQ,
                        new Filter.Key("product_name"),
                        new Filter.Value("Laptop")
                )
        );

        searchRequest = SearchRequest.query("test")
                .withTopK(5)
                .withFilterExpression(multipleExp);

        queryParams = converter.convertToQueryParams(searchRequest.getFilterExpression());
        logger.info("Multiple conditions - Query parameters: {}", queryParams);

        // Complex conditions (brand=Apple AND price>1000 AND quantity<10)
        Filter.Expression complexExp = new Filter.Expression(
                ExpressionType.AND,
                new Filter.Expression(
                        ExpressionType.EQ,
                        new Filter.Key("brand"),
                        new Filter.Value("Apple")
                ),
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

        searchRequest = SearchRequest.query("test")
                .withTopK(5)
                .withFilterExpression(complexExp);

        queryParams = converter.convertToQueryParams(searchRequest.getFilterExpression());
        logger.info("Complex conditions - Query parameters: {}", queryParams);

        // Example with IN operator
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

        searchRequest = SearchRequest.query("test")
                .withTopK(5)
                .withFilterExpression(inExp);

        queryParams = converter.convertToQueryParams(searchRequest.getFilterExpression());
        logger.info("IN operator - Query parameters: {}", queryParams);

        // Example with OR conditions
        Filter.Expression orExp = new Filter.Expression(
                ExpressionType.OR,
                new Filter.Expression(
                        ExpressionType.EQ,
                        new Filter.Key("brand"),
                        new Filter.Value("Apple")
                ),
                new Filter.Expression(
                        ExpressionType.EQ,
                        new Filter.Key("brand"),
                        new Filter.Value("Samsung")
                )
        );

        searchRequest = SearchRequest.query("test")
                .withTopK(5)
                .withFilterExpression(orExp);

        queryParams = converter.convertToQueryParams(searchRequest.getFilterExpression());
        logger.info("OR conditions - Query parameters: {}", queryParams);
    }
}