package org.caselli.cognitiveworkflow.operational.LLM;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntentDetectorResult {
    private String intentId;
    private String intentName;
    private double confidence;
    @Field("isNew")
    private boolean isNew;
    private Map<String,Object> userVariables;
}
