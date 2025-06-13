package org.caselli.cognitiveworkflow.operational.instances;

import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Setter
@Getter
public abstract class NodeInstance {
    private String id;

    /** If the node is deprecated. If it is, when the last execution finishes it will be re-instanced **/
    private boolean isDeprecated;

    // Metamodel
    private NodeMetamodel metamodel;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public abstract void process(ExecutionContext context) throws Exception;

    /**
     * Method to handle the refresh of the node
     */
    public void handleRefreshNode(){
        // To override
    }
}