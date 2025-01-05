package org.cosline.client.filtercriteria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoslineFilterExpressionConverterTest {

    private CoslineFilterExpressionConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CoslineFilterExpressionConverter();
    }

    @Test
    void testSimpleEqualityExpression() {
        Filter.Expression expression = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("name"),
                new Filter.Value("test")
        );

        String result = converter.convertToQueryParams(expression);
        assertEquals("metadata.name.eq=test", result);
    }

    @Test
    void testNumericComparisons() {
        Filter.Expression gtExpression = new Filter.Expression(
                Filter.ExpressionType.GT,
                new Filter.Key("age"),
                new Filter.Value(25)
        );
        assertEquals("metadata.age.gt=25", converter.convertToQueryParams(gtExpression));

        converter = new CoslineFilterExpressionConverter(); // Reset
        Filter.Expression lteExpression = new Filter.Expression(
                Filter.ExpressionType.LTE,
                new Filter.Key("score"),
                new Filter.Value(95.5)
        );
        assertEquals("metadata.score.lte=95.5", converter.convertToQueryParams(lteExpression));
    }

    @Test
    void testAndOperation() {
        Filter.Expression exp1 = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("department"),
                new Filter.Value("IT")
        );
        Filter.Expression exp2 = new Filter.Expression(
                Filter.ExpressionType.GT,
                new Filter.Key("salary"),
                new Filter.Value(50000)
        );
        Filter.Expression andExpression = new Filter.Expression(
                Filter.ExpressionType.AND,
                exp1,
                exp2
        );

        String result = converter.convertToQueryParams(andExpression);
        assertEquals("metadata.department.eq=IT&metadata.salary.gt=50000", result);
    }

    @Test
    void testOrOperation() {
        Filter.Expression exp1 = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("status"),
                new Filter.Value("active")
        );
        Filter.Expression exp2 = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("status"),
                new Filter.Value("pending")
        );
        Filter.Expression orExpression = new Filter.Expression(
                Filter.ExpressionType.OR,
                exp1,
                exp2
        );

        String result = converter.convertToQueryParams(orExpression);
        assertEquals("metadata.status.eq=active&metadata.status.eq=pending", result);
    }

    @Test
    void testInOperation() {
        List<String> values = Arrays.asList("admin", "user", "guest");
        Filter.Expression inExpression = new Filter.Expression(
                Filter.ExpressionType.IN,
                new Filter.Key("role"),
                new Filter.Value(values)
        );

        String result = converter.convertToQueryParams(inExpression);
        assertEquals("metadata.role.in=admin,user,guest", result);
    }

    @Test
    void testNotInOperation() {
        List<Integer> values = Arrays.asList(1, 2, 3);
        Filter.Expression ninExpression = new Filter.Expression(
                Filter.ExpressionType.NIN,
                new Filter.Key("category"),
                new Filter.Value(values)
        );

        String result = converter.convertToQueryParams(ninExpression);
        assertEquals("metadata.category.nin=1,2,3", result);
    }

    @Test
    void testNotOperation() {
        Filter.Expression equalExp = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("status"),
                new Filter.Value("inactive")
        );
        Filter.Expression notExpression = new Filter.Expression(
                Filter.ExpressionType.NOT,
                equalExp,
                null
        );

        String result = converter.convertToQueryParams(notExpression);
        assertEquals("metadata.metadata.status.eq=inactive.not=", result);
    }

    @Test
    void testComplexNestedExpression() {
        // (department = "IT" AND salary > 50000) OR (role IN ["admin", "manager"])
        Filter.Expression exp1 = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("department"),
                new Filter.Value("IT")
        );
        Filter.Expression exp2 = new Filter.Expression(
                Filter.ExpressionType.GT,
                new Filter.Key("salary"),
                new Filter.Value(50000)
        );
        Filter.Expression exp3 = new Filter.Expression(
                Filter.ExpressionType.IN,
                new Filter.Key("role"),
                new Filter.Value(Arrays.asList("admin", "manager"))
        );

        Filter.Expression andExp = new Filter.Expression(
                Filter.ExpressionType.AND,
                exp1,
                exp2
        );
        Filter.Expression finalExp = new Filter.Expression(
                Filter.ExpressionType.OR,
                andExp,
                exp3
        );

        String result = converter.convertToQueryParams(finalExp);
        assertEquals("metadata.department.eq=IT&metadata.salary.gt=50000&metadata.role.in=admin,manager", result);
    }

    @Test
    void testGroupExpression() {
        Filter.Expression innerExp = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("status"),
                new Filter.Value("active")
        );
        Filter.Group group = new Filter.Group(innerExp);

        String result = converter.convertToQueryParams(group.content());
        assertEquals("metadata.status.eq=active", result);
    }

    @Test
    void testInvalidInOperation() {
        Filter.Expression inExpression = new Filter.Expression(
                Filter.ExpressionType.IN,
                new Filter.Key("role"),
                new Filter.Value("not-a-list")
        );

        assertThrows(IllegalArgumentException.class, () ->
                converter.convertToQueryParams(inExpression)
        );
    }

    @Test
    void testAllComparisonOperators() {
        testOperator(Filter.ExpressionType.EQ, "eq", "name", "test");
        testOperator(Filter.ExpressionType.NE, "ne", "age", 25);
        testOperator(Filter.ExpressionType.LT, "lt", "score", 90);
        testOperator(Filter.ExpressionType.LTE, "lte", "price", 100.50);
        testOperator(Filter.ExpressionType.GT, "gt", "count", 1000);
        testOperator(Filter.ExpressionType.GTE, "gte", "rating", 4.5);
    }

    private void testOperator(Filter.ExpressionType type, String operator, String key, Object value) {
        converter = new CoslineFilterExpressionConverter(); // Reset state
        Filter.Expression expression = new Filter.Expression(
                type,
                new Filter.Key(key),
                new Filter.Value(value)
        );
        String expected = String.format("metadata.%s.%s=%s", key, operator, value);
        assertEquals(expected, converter.convertToQueryParams(expression));
    }
}