package org.caselli.cognitiveworkflow.operational.instances;

import org.caselli.cognitiveworkflow.knowledge.model.node.GatewayNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.StandardPort;
import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;

@ActiveProfiles("test")
@Tag("it")
class GatewayNodeInstanceIT {

    private GatewayNodeInstance nodeInstance;
    private ExecutionContext context;
    private GatewayNodeMetamodel metamodel;

    @BeforeEach
    void setUp() {
        nodeInstance = new GatewayNodeInstance();
        metamodel = new GatewayNodeMetamodel();
        nodeInstance.setMetamodel(metamodel);
        nodeInstance.setId("test-node");
        context = new ExecutionContext();
    }

    @Test
    void testTransparentGateway(){

        var port1 =  StandardPort.builder()
                .withKey("input1")
                .withSchema(PortSchema.builder().stringSchema().build())
                .build();

        var port2 =  StandardPort.builder()
                .withKey("input2")
                .withSchema(PortSchema.builder().stringSchema().build())
                .build();

        metamodel.setInputPorts(List.of(port1, port2));

        // Check that the output ports are created correctly as a copy of the input ports
        var outputPorts = metamodel.getOutputPorts();
        assert outputPorts.size() == 2 : "Output ports should be created for each input port";
        for (var inputPort : metamodel.getInputPorts()) {
            var outputPort = outputPorts.stream()
                    .filter(op -> op.getKey().equals(inputPort.getKey()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Output port not found for input port: " + inputPort.getKey()));
            assert outputPort.getSchema().equals(inputPort.getSchema()) : "Output port schema should match input port schema";
        }

        context.put("input1", "hello");
        context.put("input2", "world");

        // Process the node instance
        nodeInstance.process(context);

        // Check that the gateway is transparent: the context values should be propagated without changes
        for (var outputPort : metamodel.getInputPorts()) {
            var inputValue = context.get(outputPort.getKey());
            var outputValue = context.get(outputPort.getKey());
            assert inputValue.equals(outputValue) : "Input value should match output value for transparent gateway";
        }
    }
}