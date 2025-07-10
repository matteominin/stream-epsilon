package org.caselli.cognitiveworkflow.knowledge.model.workflow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author niccolocaselli
 */
@Data
public class EdgeCondition {

    private LogicalOperator operator;
    private List<Expression> expressions;

    @Data
    public static class Expression {
        @NotNull
        private String port;

        @NotNull
        private Operation operation;


        private Object value;
    }

    public enum LogicalOperator {
        AND, OR
    }

    public enum Operation {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN, LESS_THAN,
        CONTAINS, STARTS_WITH,
        IN, NOT_IN,
        IS_NULL, IS_NOT_NULL,
        IS_TRUE, IS_FALSE
    }
}



