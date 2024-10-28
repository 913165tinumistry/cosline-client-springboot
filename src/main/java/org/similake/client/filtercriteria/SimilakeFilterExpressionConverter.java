package org.similake.client.filtercriteria;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.converter.AbstractFilterExpressionConverter;

import java.util.List;

public class SimilakeFilterExpressionConverter extends AbstractFilterExpressionConverter {

    private StringBuilder queryParams = new StringBuilder();
    private boolean isFirstParam = true;

    public String convertToQueryParams(Filter.Expression expression) {
        convert(expression, queryParams);
        return queryParams.toString();
    }

    protected void convert(Filter.Expression expression, StringBuilder context) {
        if (expression.type() == Filter.ExpressionType.AND ||
                expression.type() == Filter.ExpressionType.OR) {
            // Handle nested expressions
            Filter.Expression left = (Filter.Expression) expression.left();
            Filter.Expression right = (Filter.Expression) expression.right();

            convert(left, context);
            convert(right, context);
        } else {
            // Handle leaf expressions
            doExpression(expression, context);
        }
    }

    @Override
    protected void doExpression(Filter.Expression expression, StringBuilder context) {
        if (expression.type() == Filter.ExpressionType.IN) {
            handleIn(expression);
        } else if (expression.type() == Filter.ExpressionType.NIN) {
            handleNotIn(expression);
        } else {
            if (!isFirstParam) {
                queryParams.append("&");
            }
            queryParams.append("metadata.");
            convertOperand(expression.left(), queryParams);
            queryParams.append(getOperationSymbol(expression));
            convertOperand(expression.right(), queryParams);
            isFirstParam = false;
        }
    }

    private void handleIn(Filter.Expression expression) {
        if (!isFirstParam) {
            queryParams.append("&");
        }
        Filter.Key key = (Filter.Key) expression.left();
        Filter.Value right = (Filter.Value) expression.right();
        Object value = right.value();

        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("Expected a List, but got: " + value.getClass().getSimpleName());
        }

        queryParams.append("metadata.")
                .append(key.key())
                .append(".in=")
                .append(String.join(",", values.stream().map(String::valueOf).toList()));
        isFirstParam = false;
    }

    private void handleNotIn(Filter.Expression expression) {
        if (!isFirstParam) {
            queryParams.append("&");
        }
        Filter.Key key = (Filter.Key) expression.left();
        Filter.Value right = (Filter.Value) expression.right();
        Object value = right.value();

        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("Expected a List, but got: " + value.getClass().getSimpleName());
        }

        queryParams.append("metadata.")
                .append(key.key())
                .append(".nin=")
                .append(String.join(",", values.stream().map(String::valueOf).toList()));
        isFirstParam = false;
    }

    private String getOperationSymbol(Filter.Expression exp) {
        return switch (exp.type()) {
            case EQ -> ".eq=";
            case NE -> ".ne=";
            case LT -> ".lt=";
            case LTE -> ".lte=";
            case GT -> ".gt=";
            case GTE -> ".gte=";
            case NOT -> ".not=";
            default -> throw new RuntimeException("Not supported expression type: " + exp.type());
        };
    }

    @Override
    protected void doKey(Filter.Key key, StringBuilder context) {
        context.append(key.key());
    }

    @Override
    protected void doStartGroup(Filter.Group group, StringBuilder context) {
        // Not needed for query params
    }

    @Override
    protected void doEndGroup(Filter.Group group, StringBuilder context) {
        // Not needed for query params
    }

    @Override
    protected void doValue(Filter.Value value, StringBuilder context) {
        Object val = value.value();
        if (val instanceof String) {
            context.append(val);
        } else if (val instanceof Number) {
            context.append(val);
        } else if (val instanceof Boolean) {
            context.append(val);
        } else if (val instanceof List<?>) {
            context.append(String.join(",", ((List<?>) val).stream().map(String::valueOf).toList()));
        } else {
            context.append(val);
        }
    }
}