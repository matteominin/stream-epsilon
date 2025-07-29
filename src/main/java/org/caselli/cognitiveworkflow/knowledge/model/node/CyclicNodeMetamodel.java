package org.caselli.cognitiveworkflow.knowledge.model.node;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.List;

import org.caselli.cognitiveworkflow.knowledge.model.node.port.StandardPort;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowNode;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public class CyclicNodeMetamodel extends FlowNodeMetamodel {
    public CyclicNodeMetamodel() {
        super();
        this.setControlType(ControlType.CYCLIC);
    }

    private List<StandardPort> inputPorts;

    private List<StandardPort> outputPorts;

    @NotNull
    private Integer start;

    @NotNull
    private Integer end;

    @NotNull
    private Integer step = 1;

    @NotNull
    private List<WorkflowNode> nodes;

    @NotNull
    private List<WorkflowEdge> edges;

    @Override
    public List<StandardPort> getInputPorts() {
        return this.inputPorts;
    }

    public void setInputPorts(List<StandardPort> inputPorts) {
        this.inputPorts = inputPorts != null ? List.copyOf(inputPorts) : Collections.emptyList();
    }

    @Override
    public List<StandardPort> getOutputPorts() {
        return this.outputPorts;
    }

    public void setOutputPorts(List<StandardPort> outputPorts) {
        this.outputPorts = outputPorts != null ? List.copyOf(outputPorts) : Collections.emptyList();
    }
}
