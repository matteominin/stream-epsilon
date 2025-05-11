package org.caselli.cognitiveworkflow.operational.LLM;

import org.caselli.cognitiveworkflow.knowledge.model.node.RestToolNodeMetamodel;
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
@ActiveProfiles("test")
public class InputMapperServiceIT {

    @Autowired
    private InputMapperService inputMapperService;



    /**
     * Test the Input Mapper with simple mapping and only 1 available node
     * Should select the only available node
     */
    @Test
    public void shouldWorkWithOnlyOneNode() {
        // NODE 1
        RestPort source1 = RestPort.resBuilder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source2 = RestPort.resBuilder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source3 = RestPort.resBuilder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestToolNodeMetamodel nodeA = new RestToolNodeMetamodel();
        nodeA.setId(String.valueOf(UUID.randomUUID()));
        nodeA.setName("CHECK_USER");
        nodeA.setDescription("Check if user is registered to our platform");
        nodeA.setInputPorts(List.of(source1, source2, source3));
        nodeA.setOutputPorts(List.of());

        Map<String, Object> variables = Map.of(
                "name", "Niccolò",
                "number", "+393527624",
                "email", "caselli@gmail.com"
        );

        var res = inputMapperService.mapInput(variables,List.of(nodeA));

        assertNotNull(res);
        assertNotNull(res.getStartingNode());
        assertEquals(res.getStartingNode().getId(), nodeA.getId());
        assertEquals(res.getContext().get("user_name"),variables.get("name"));
        assertEquals(res.getContext().get("user_phone"),variables.get("number"));
        assertEquals(res.getContext().get("user_email"),variables.get("email"));
    }


    /**
     * Tests that the Input Mapper correctly selects ignore the variables that are not required
     * The service should discard excess variables not required in input
     */
    @Test
    public void shouldDiscardExcessVariables() {
        // NODE A
        RestPort source_A_1 = RestPort.resBuilder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_A_2 = RestPort.resBuilder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_A_3 = RestPort.resBuilder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestToolNodeMetamodel nodeA = new RestToolNodeMetamodel();
        nodeA.setId(String.valueOf(UUID.randomUUID()));
        nodeA.setName("CHECK_USER_A");
        nodeA.setDescription("Check if user is registered to our platform using legacy API");
        nodeA.setInputPorts(List.of(source_A_1, source_A_2, source_A_3));
        nodeA.setOutputPorts(List.of());

        // VARIABLES
        Map<String, Object> variables = Map.of(
                "name", "Niccolò",
                "number", "+393527624",
                "email", "caselli@gmail.com",
                "IBAN", "IT60X0542811101000000123456",
                "creditCard", "4111111111111111",
                "ssn", "000-00-0000",
                "accountId", "987238363283",
                "orderId", "987238363281"
        );

        // MAPPER
        var res = inputMapperService.mapInput(variables,List.of(nodeA));

        // ASSERTIONS
        assertNotNull(res);
        assertNotNull(res.getStartingNode());
        assertEquals(nodeA.getId(), res.getStartingNode().getId());
        assertEquals(nodeA.getName(), res.getStartingNode().getName());

        // Verify mapped input variables
        Map<String, Object> mappedInputs = res.getContext();
        assertNotNull(mappedInputs);
        assertEquals(3, mappedInputs.size());
        assertTrue(mappedInputs.containsKey("user_name"));
        assertTrue(mappedInputs.containsKey("user_email"));
        assertTrue(mappedInputs.containsKey("user_phone"));
    }

    /**
     * Test the Input Mapper should work even if an option port field is missing in the variables
     * Should select the only available node without trying to invent the content of the optional missing variable
     */
    @Test
    public void shouldWorkWithOnlyOnNodeEvenWithoutAllOptionalFieldsSatisfied() {
        // NODE 1
        RestPort source1 = RestPort.resBuilder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source2 = RestPort.resBuilder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source3 = RestPort.resBuilder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // OPTIONAL FIELDS:
        RestPort source4 = RestPort.resBuilder().withKey("user_id").withSchema(PortSchema.builder().stringSchema().withRequired(false).build()).build();
        RestPort source5 = RestPort.resBuilder().withKey("user_password").withSchema(PortSchema.builder().stringSchema().withRequired(false).build()).build();

        RestToolNodeMetamodel nodeA = new RestToolNodeMetamodel();
        nodeA.setId(String.valueOf(UUID.randomUUID()));
        nodeA.setName("CHECK_USER");
        nodeA.setDescription("Check if user is registered to our platform");
        nodeA.setInputPorts(List.of(source1, source2, source3, source4, source5));
        nodeA.setOutputPorts(List.of());

        Map<String, Object> variables = Map.of(
                "name", "Niccolò",
                "number", "+393527624",
                "email", "caselli@gmail.com"
        );

        var res = inputMapperService.mapInput(variables,List.of(nodeA));

        // ASSERTIONS
        assertNotNull(res);
        assertNotNull(res.getStartingNode());
        assertEquals(nodeA.getId(), res.getStartingNode().getId());
        assertEquals(nodeA.getName(), res.getStartingNode().getName());

        Map<String, Object> mappedInputs = res.getContext();
        assertNotNull(mappedInputs);
        assertEquals(3, mappedInputs.size());
        assertTrue(mappedInputs.containsKey("user_name"));
        assertTrue(mappedInputs.containsKey("user_email"));
        assertTrue(mappedInputs.containsKey("user_phone"));
    }


    /**
     * Test the Input Mapper with simple mapping and only one unsatisfiable node as input
     * Should should not select any nodes.
     */
    @Test
    public void shouldFailWithOnlyOneWrongNode() {
        // NODE 1
        RestPort source1 = RestPort.resBuilder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source2 = RestPort.resBuilder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source3 = RestPort.resBuilder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source4 = RestPort.resBuilder().withKey("user_id").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestToolNodeMetamodel nodeA = new RestToolNodeMetamodel();
        nodeA.setId(String.valueOf(UUID.randomUUID()));
        nodeA.setName("CHECK_USER");
        nodeA.setDescription("Check if user is registered to our platform");
        nodeA.setInputPorts(List.of(source1, source2, source3, source4));
        nodeA.setOutputPorts(List.of());

        Map<String, Object> variables = Map.of(
                "name", "Niccolò",
                "number", "+393527624",
                "email", "caselli@gmail.com"
        );

        var res = inputMapperService.mapInput(variables,List.of(nodeA));

        assertNull(res);
    }


    /**
     * Test the Input Mapper with simple mapping and multiple unsatisfiable nodes as input
     *
     * <ul>
     *   <li>Node A has a variable (user_id) that is not among the provided variables</li>
     *   <li>Node B has a variable (user_password) that is not among the provided variables</li>
     *   <li>Node C has a variable (user_social_security_number) that is not among the provided variables</li>
     * </ul>
     *
     * Service should select no nodes.
     */
    @Test
    public void shouldFailWithMultipleWrongNodes() {

        // NODE A
        RestPort source1 = RestPort.resBuilder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source2 = RestPort.resBuilder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source3 = RestPort.resBuilder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // Unsatisfied port:
        RestPort source4 = RestPort.resBuilder().withKey("user_id").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestToolNodeMetamodel nodeA = new RestToolNodeMetamodel();
        nodeA.setId(String.valueOf(UUID.randomUUID()));
        nodeA.setName("CHECK_USER");
        nodeA.setDescription("Check if user is registered to our platform");
        nodeA.setInputPorts(List.of(source1, source2, source3, source4));
        nodeA.setOutputPorts(List.of());

        Map<String, Object> variables = Map.of(
                "name", "Niccolò",
                "number", "+393527624",
                "email", "caselli@gmail.com"
        );

        // NODE B
        RestPort source_B_1 = RestPort.resBuilder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_B_2 = RestPort.resBuilder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_B_3 = RestPort.resBuilder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // Unsatisfied port:
        RestPort source_B_4 = RestPort.resBuilder().withKey("user_password").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestToolNodeMetamodel nodeB = new RestToolNodeMetamodel();
        nodeB.setId(String.valueOf(UUID.randomUUID()));
        nodeB.setName("CHECK_USER_B");
        nodeB.setDescription("Check if user is registered to our platform using V2 API");
        nodeB.setInputPorts(List.of(source_B_1, source_B_2, source_B_3, source_B_4));
        nodeB.setOutputPorts(List.of());

        // NODE C
        RestPort source_C_1 = RestPort.resBuilder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_C_2 = RestPort.resBuilder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_C_3 = RestPort.resBuilder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // Unsatisfied port:
        RestPort source_C_4 = RestPort.resBuilder().withKey("user_social_security_number").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();


        RestToolNodeMetamodel nodeC = new RestToolNodeMetamodel();
        nodeC.setId(String.valueOf(UUID.randomUUID()));
        nodeC.setName("CHECK_USER_C");
        nodeC.setDescription("Check if user is registered to our platform using V1 API");
        nodeC.setInputPorts(List.of(source_C_1, source_C_2,source_C_3,source_C_4));
        nodeC.setOutputPorts(List.of());

        var res = inputMapperService.mapInput(variables,List.of(nodeA, nodeB, nodeC));

        if(res != null){
            // Debug why test is failing
            System.out.println("LLM returned a not-null context:");
            res.context.printContext();
        }

        assertNull(res);
    }


    /**
     * Test the Input Mapper with simple mapping and multiple unsatisfiable nodes except one.
     * The only satisfied node, however, have some optional fields that cannot be satisfied
     *
     * <ul>
     *   <li>Node A has a variable (user_id) that is not among the provided variables</li>
     *   <li>Node B has a variable (user_password) that is not among the provided variables</li>
     *   <li>Node C has a variable (user_social_security_number) that is not among the provided variables</li>
     *   <li>Node D has a variable (user_social_security_number) that is not among the provided variables but is optional</li>
     * </ul>
     *
     * Service should select Node D
     */
    @Test
    public void shouldWorkWithMultipleWrongNodesAndOneWithoutAllTheOptionalFields() {
        // NODE 1
        RestPort source1 = RestPort.resBuilder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source2 = RestPort.resBuilder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source3 = RestPort.resBuilder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // Unsatisfied port:
        RestPort source4 = RestPort.resBuilder().withKey("user_id").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestToolNodeMetamodel nodeA = new RestToolNodeMetamodel();
        nodeA.setId(String.valueOf(UUID.randomUUID()));
        nodeA.setName("CHECK_USER");
        nodeA.setDescription("Check if user is registered to our platform");
        nodeA.setInputPorts(List.of(source1, source2, source3, source4));
        nodeA.setOutputPorts(List.of());


        // NODE B
        RestPort source_B_1 = RestPort.resBuilder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_B_2 = RestPort.resBuilder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_B_3 = RestPort.resBuilder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // Unsatisfied port:
        RestPort source_B_4 = RestPort.resBuilder().withKey("user_password").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestToolNodeMetamodel nodeB = new RestToolNodeMetamodel();
        nodeB.setId(String.valueOf(UUID.randomUUID()));
        nodeB.setName("CHECK_USER_B");
        nodeB.setDescription("Check if user is registered to our platform using V2 API");
        nodeB.setInputPorts(List.of(source_B_1, source_B_2, source_B_3, source_B_4));
        nodeB.setOutputPorts(List.of());

        // NODE C

        RestPort source_C_1 = RestPort.resBuilder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_C_2 = RestPort.resBuilder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_C_3 = RestPort.resBuilder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // Unsatisfied port:
        RestPort source_C_4 = RestPort.resBuilder().withKey("user_social_security_number").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestToolNodeMetamodel nodeC = new RestToolNodeMetamodel();
        nodeC.setId(String.valueOf(UUID.randomUUID()));
        nodeC.setName("CHECK_USER_C");
        nodeC.setDescription("Check if user is registered to our platform using V1 API");
        nodeC.setInputPorts(List.of(source_C_1, source_C_2,source_C_3,source_C_4));
        nodeC.setOutputPorts(List.of());


        // NODE D

        RestPort source_D_1 = RestPort.resBuilder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_D_2 = RestPort.resBuilder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_D_3 = RestPort.resBuilder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // OPTIONAL Unsatisfied port:
        RestPort source_D_4 = RestPort.resBuilder().withKey("user_social_security_number").withSchema(PortSchema.builder().stringSchema().withRequired(false).build()).build();

        RestToolNodeMetamodel nodeD = new RestToolNodeMetamodel();
        nodeD.setId(String.valueOf(UUID.randomUUID()));
        nodeD.setName("CHECK_USER_D");
        nodeD.setDescription("Check if user is registered to our platform using V1 API");
        nodeD.setInputPorts(List.of(source_D_1, source_D_2,source_D_3,source_D_4));
        nodeD.setOutputPorts(List.of());




        Map<String, Object> variables = Map.of(
                "name", "Niccolò",
                "number", "+393527624",
                "email", "caselli@gmail.com"
        );

        var res = inputMapperService.mapInput(variables,List.of(nodeA, nodeB, nodeC, nodeD));

        // ASSERTIONS
        assertNotNull(res);
        assertNotNull(res.getStartingNode());
        assertEquals(nodeD.getId(), res.getStartingNode().getId());
        assertEquals(nodeD.getName(), res.getStartingNode().getName());

        Map<String, Object> mappedInputs = res.getContext();
        assertNotNull(mappedInputs);
        assertEquals(3, mappedInputs.size());
        assertTrue(mappedInputs.containsKey("user_name"));
        assertTrue(mappedInputs.containsKey("user_email"));
        assertTrue(mappedInputs.containsKey("user_phone"));
    }


    /**
     * Tests that the Input Mapper correctly selects the node with all required inputs satisfied.
     *
     * <ul>
     *   <li>Node A has all variables satisfied</li>
     *   <li>Node B has a variable (password) that is not among the provided variables</li>
     *   <li>Node C could also be selected as all its variables are satisfied, but it has an unused
     *       provided variable (phone_number). The mapper should prefer Node A in this case.</li>
     * </ul>
     *
     * The mapper should select either Node A or C as they are the only ones with all inputs satisfied,
     * with preference given to Node A since it doesn't have unused input variables.
     */
    @Test
    public void shouldWorkWithMultipleNodeAndOnlyOneValid() {
        // NODE A

        RestPort source_A_1 = RestPort.resBuilder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_A_2 = RestPort.resBuilder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_A_3 = RestPort.resBuilder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestToolNodeMetamodel nodeA = new RestToolNodeMetamodel();
        nodeA.setId(String.valueOf(UUID.randomUUID()));
        nodeA.setName("CHECK_USER_A");
        nodeA.setDescription("Check if user is registered to our platform using legacy API");
        nodeA.setInputPorts(List.of(source_A_1, source_A_2, source_A_3));
        nodeA.setOutputPorts(List.of());

        // NODE B

        RestPort source_B_1 = RestPort.resBuilder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_B_2 = RestPort.resBuilder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_B_3 = RestPort.resBuilder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_B_4 = RestPort.resBuilder().withKey("user_password").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestToolNodeMetamodel nodeB = new RestToolNodeMetamodel();
        nodeB.setId(String.valueOf(UUID.randomUUID()));
        nodeB.setName("CHECK_USER_B");
        nodeB.setDescription("Check if user is registered to our platform using V2 API");
        nodeB.setInputPorts(List.of(source_B_1, source_B_2, source_B_3, source_B_4));
        nodeB.setOutputPorts(List.of());

        // NODE C

        RestPort source_C_1 = RestPort.resBuilder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_C_2 = RestPort.resBuilder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestToolNodeMetamodel nodeC = new RestToolNodeMetamodel();
        nodeC.setId(String.valueOf(UUID.randomUUID()));
        nodeC.setName("CHECK_USER_C");
        nodeC.setDescription("Check if user is registered to our platform using V1 API");
        nodeC.setInputPorts(List.of(source_C_1, source_C_2));
        nodeC.setOutputPorts(List.of());

        // VARIABLES

        Map<String, Object> variables = Map.of(
                "name", "Niccolò",
                "number", "+393527624",
                "email", "caselli@gmail.com"
        );


        // MAPPER
        var res = inputMapperService.mapInput(variables,List.of(nodeB, nodeA, nodeC));

        // ASSERTIONS
        assertNotNull(res);
        assertNotNull(res.getStartingNode());
        assertEquals(nodeA.getId(), res.getStartingNode().getId());
        assertEquals(nodeA.getName(), res.getStartingNode().getName());

        // Verify mapped input variables
        Map<String, Object> mappedInputs = res.getContext();
        assertNotNull(mappedInputs);
        assertEquals(3, mappedInputs.size());
        assertTrue(mappedInputs.containsKey("user_name"));
        assertTrue(mappedInputs.containsKey("user_email"));
        assertTrue(mappedInputs.containsKey("user_phone"));
    }

    /**
     * Test the Input Mapper with a nested mapping and only 1 available node
     * Should select the only available node
     */
    @Test
    public void shouldWorkWithOnlyOneNestedNode() {
        // NODE 1
        RestPort source1 = RestPort.resBuilder()
                    .withKey("user")
                    .withSchema(PortSchema.builder()
                    .objectSchema(Map.of(
                        "userDetails", PortSchema.builder()
                                .objectSchema(Map.of(
                                        "email", PortSchema.builder().stringSchema().withRequired(true).build(),
                                        "phone_number", PortSchema.builder().stringSchema().withRequired(false).build()
                                ))
                                .withRequired(true)
                                .build(),
                        "id", PortSchema.builder().stringSchema().withRequired(true).build()
                ))
                .build()).build();

        RestPort source2 = RestPort.resBuilder().withKey("orderId").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();


        RestToolNodeMetamodel nodeA = new RestToolNodeMetamodel();
        nodeA.setId(String.valueOf(UUID.randomUUID()));
        nodeA.setName("SEE_SHIPPING_INFO");
        nodeA.setDescription("Get user shipping order");
        nodeA.setInputPorts(List.of(source1, source2));
        nodeA.setOutputPorts(List.of());

        Map<String, Object> variables = Map.of(
                "id_of_the_user", "123",
                "number", "+393527624",
                "email", "caselli@gmail.com",
                "order_id", "ORDER_ID"
        );

        var res = inputMapperService.mapInput(variables, List.of(nodeA));

        assertNotNull(res);

        System.out.println("Context result: ");
        res.context.printContext();

        assertNotNull(res.getStartingNode());
        assertEquals(res.getStartingNode().getId(), nodeA.getId());
        assertEquals(res.getContext().get("user.id"), variables.get("id_of_the_user"));
        assertEquals(res.getContext().get("user.userDetails.email"), variables.get("email"));
        assertEquals(res.getContext().get("user.userDetails.phone_number"),variables.get("number"));
        assertEquals(res.getContext().get("orderId"), variables.get("order_id"));
    }

}
