package org.caselli.cognitiveworkflow.operational.LLM;

import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestToolNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.RestPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Tag("it")
@Tag("focus")
@ActiveProfiles("test")
public class InputMapperServiceIT {

    @Autowired
    private InputMapperService inputMapperService;



    /**
     * Test the Input Mapper with simple mapping
     */
    @Test
    public void InputMapperShouldWorkWithSimpleMappingAndOnlyOneNode() {
        // NODE 1
        RestPort source1 = RestPort.resBuilder()
                .withKey("user_name")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();


        RestPort source2 = RestPort.resBuilder()
                .withKey("user_email")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        RestPort source3 = RestPort.resBuilder()
                .withKey("user_phone")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();


        RestToolNodeMetamodel nodeA = new RestToolNodeMetamodel();
        nodeA.setId(String.valueOf(UUID.randomUUID()));
        nodeA.setName("CHECK_USER");
        nodeA.setDescription("Check if user is registered to our platform");
        nodeA.setInputPorts(List.of(source1, source2, source3));
        nodeA.setOutputPorts(List.of());

        Map<String, Object> variables = Map.of(
                "name", "ciao",
                "number", "+393527624",
                "email", "caselli@gmail.com"
        );

        var res = inputMapperService.mapInput(variables,List.of(nodeA));

    }
}
