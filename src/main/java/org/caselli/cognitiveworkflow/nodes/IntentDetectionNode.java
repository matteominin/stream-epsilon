package org.caselli.cognitiveworkflow.nodes;

import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.core.WorkflowNode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;


@Setter
@Getter
@Component
@Scope("prototype") // to avoid singleton
public class IntentDetectionNode implements WorkflowNode {
    private String id;
    private List<String> inputKeys;
    private List<String> outputKeys;

    private String modelName;
    private double temperature;
    private int topK;


    @Override
    public void process(Map<String, Object> context) {


        System.out.println("STARTING INTENT DETECTION");
        System.out.println(inputKeys + ": " + outputKeys + ": " +  modelName + ": " + temperature + ": " + topK);



        String input = (String) context.get(inputKeys.get(0));

        String intent = "detected-intent-for('" + input + "')";
        context.put(outputKeys.get(0), intent);
    }

}
