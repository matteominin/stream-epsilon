package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.knowledge.model.workflow.EdgeCondition;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Evaluates edge conditions to determine if workflow transitions should proceed.
 * This class has the sole responsibility of validating EdgeCondition objects
 * against the current execution context.
 *
 * @author niccolocaselli
 */
@Component
public class EdgeConditionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(EdgeConditionEvaluator.class);

    /**
     * Evaluates the condition on an edge to determine if execution should proceed.
     *
     * @param edge The workflow edge containing the condition to evaluate
     * @param context The current execution context containing variable values
     * @return true if the condition passes or there is no condition, false otherwise
     */
    public boolean evaluate(WorkflowEdge edge, ExecutionContext context) {
        EdgeCondition condition = edge.getCondition();

        // No condition means the edge is unconditionally valid
        if (condition == null) {
            logger.debug("Edge {} has no condition, allowing transition", edge.getId());
            return true;
        }

        return evaluateCondition(condition, context);
    }

    /**
     * Evaluates a specific EdgeCondition against the execution context.
     *
     * @param condition The condition to evaluate
     * @param context The current execution context
     * @return true if the condition is satisfied, false otherwise
     */
    public boolean evaluateCondition(EdgeCondition condition, ExecutionContext context) {
        if (condition == null) {
            return true;
        }

        List<EdgeCondition.Expression> expressions = condition.getExpressions();
        if (expressions == null) {
            logger.debug("Edge condition has no expressions, allowing transition");
            return true;
        }

        EdgeCondition.LogicalOperator operator = condition.getOperator();
        if (operator == null) {
            // Default to AND if no operator specified
            operator = EdgeCondition.LogicalOperator.AND;
        }

        boolean result = evaluateExpressions(expressions, operator, context);

        logger.debug("Edge condition evaluation result: {} (operator: {})", result, operator);
        return result;
    }

    /**
     * Evaluates a list of expressions using the specified logical operator.
     */
    private boolean evaluateExpressions(List<EdgeCondition.Expression> expressions,
                                        EdgeCondition.LogicalOperator operator,
                                        ExecutionContext context) {

        if (operator == EdgeCondition.LogicalOperator.OR) {
            // OR: return true if any expression is true
            for (EdgeCondition.Expression expr : expressions) {
                if (evaluateExpression(expr, context)) {
                    return true;
                }
            }
            return false;
        } else {
            // AND: return false if any expression is false
            for (EdgeCondition.Expression expr : expressions) {
                if (!evaluateExpression(expr, context)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Evaluates a single expression against the execution context.
     */
    private boolean evaluateExpression(EdgeCondition.Expression expression, ExecutionContext context) {
        String port = expression.getPort();
        Object contextValue = context.get(port);
        Object expectedValue = expression.getValue();
        EdgeCondition.Operation operation = expression.getOperation();

        boolean result = performOperation(contextValue, expectedValue, operation);

        logger.debug("Expression evaluation: port='{}' operation='{}' expected='{}' actual='{}' result={}", port, operation, expectedValue, contextValue, result);

        return result;
    }

    /**
     * Performs the actual operation comparison.
     */
    private boolean performOperation(Object contextValue, Object expectedValue, EdgeCondition.Operation operation) {
        return switch (operation) {
            case EQUALS -> Objects.equals(contextValue, expectedValue);
            case NOT_EQUALS -> !Objects.equals(contextValue, expectedValue);
            case GREATER_THAN -> compareNumbers(contextValue, expectedValue) > 0;
            case LESS_THAN -> compareNumbers(contextValue, expectedValue) < 0;
            case CONTAINS -> contextValue != null && contextValue.toString().contains(expectedValue.toString());
            case STARTS_WITH -> contextValue != null && contextValue.toString().startsWith(expectedValue.toString());
            case IN -> isValueInCollection(contextValue, expectedValue);
            case NOT_IN -> !isValueInCollection(contextValue, expectedValue);
            case IS_NULL -> contextValue == null;
            case IS_NOT_NULL -> contextValue != null;
            case IS_TRUE -> isTrueValue(contextValue);
            case IS_FALSE -> isFalseValue(contextValue);
            default -> {
                logger.warn("Unknown operation: {}", operation);
                yield false;
            }
        };
    }

    /**
     * Compares two values as numbers.
     */
    private int compareNumbers(Object contextValue, Object expectedValue) {
        if (contextValue == null || expectedValue == null) {
            throw new IllegalArgumentException("Cannot compare null values with GREATER_THAN/LESS_THAN operations");
        }

        try {
            double contextNum = Double.parseDouble(contextValue.toString());
            double expectedNum = Double.parseDouble(expectedValue.toString());
            return Double.compare(contextNum, expectedNum);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot compare non-numeric values with GREATER_THAN/LESS_THAN operations", e);
        }
    }

    /**
     * Checks if a value is contained in a collection or array.
     */
    private boolean isValueInCollection(Object contextValue, Object expectedValue) {
        if (expectedValue instanceof Collection) {
            return ((Collection<?>) expectedValue).contains(contextValue);
        } else if (expectedValue instanceof Object[] array) {

            for (Object item : array)
                if (Objects.equals(contextValue, item))
                    return true;


        }
        return false;
    }

    /**
     * Determines if a value represents true.
     */
    private boolean isTrueValue(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) {
            String str = ((String) value).toLowerCase();
            return "true".equals(str) || "yes".equals(str) || "1".equals(str);
        }
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;

        return false;
    }

    /**
     * Determines if a value represents false.
     */
    private boolean isFalseValue(Object value) {
        if (value == null) return true;
        if (value instanceof Boolean) return !(Boolean) value;
        if (value instanceof String) {
            String str = ((String) value).toLowerCase();
            return "false".equals(str) || "no".equals(str) || "0".equals(str) || str.isEmpty();
        }

        if (value instanceof Number) return ((Number) value).doubleValue() == 0;

        return false;
    }

    /**
     * Validates that an EdgeCondition is properly configured.
     *
     * @param condition The condition to validate
     * @return true if the condition is valid, false otherwise
     */
    public boolean isValidCondition(EdgeCondition condition) {
        if (condition == null) {
            return true; // null condition is valid (means unconditional)
        }

        List<EdgeCondition.Expression> expressions = condition.getExpressions();
        if (expressions == null || expressions.isEmpty()) {
            logger.warn("Invalid edge condition: no expressions defined");
            return false;
        }

        for (EdgeCondition.Expression expr : expressions) {
            if (!isValidExpression(expr)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validates that an Expression is properly configured.
     */
    private boolean isValidExpression(EdgeCondition.Expression expression) {
        if (expression.getPort() == null || expression.getPort().trim().isEmpty()) {
            logger.warn("Invalid expression: port is null or empty");
            return false;
        }

        if (expression.getOperation() == null) {
            logger.warn("Invalid expression: operation is null for port '{}'", expression.getPort());
            return false;
        }

        EdgeCondition.Operation operation = expression.getOperation();

        if (operation == EdgeCondition.Operation.IS_NULL ||
                operation == EdgeCondition.Operation.IS_NOT_NULL ||
                operation == EdgeCondition.Operation.IS_TRUE ||
                operation == EdgeCondition.Operation.IS_FALSE) {
            return true;
        }

        if (expression.getValue() == null) {
            logger.warn("Invalid expression: value is null for port '{}' with operation '{}'", expression.getPort(), operation);
            return false;
        }

        return true;
    }
}