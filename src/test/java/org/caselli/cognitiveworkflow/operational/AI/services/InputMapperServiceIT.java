package org.caselli.cognitiveworkflow.operational.AI.services;

import org.caselli.cognitiveworkflow.knowledge.model.node.RestNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.RestPort;
import org.junit.jupiter.api.DisplayName;
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
     * Test the Input Mapper with simple mapping and only 1 starting node
     */
    @Test
    @DisplayName("Test with only one node")
    public void shouldWorkWithOnlyOneNode() {
        // NODE 1
        RestPort source1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source3 = RestPort.builder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestNodeMetamodel nodeA = new RestNodeMetamodel();
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

        var res = inputMapperService.mapInput(variables,List.of(nodeA)).result;

        System.out.println("Context result:"+res);

        assertNotNull(res);
        assertEquals(res.getContext().get("user_name"), variables.get("name"));
        assertEquals(res.getContext().get("user_phone"),variables.get("number"));
        assertEquals(res.getContext().get("user_email"),variables.get("email"));
    }


    /**
     * Tests that the Input Mapper correctly selects ignore the variables that are not required
     * The service should discard excess variables not required in input
     */
    @Test
    @DisplayName("Test with excess variables not required in input")
    public void shouldDiscardExcessVariables() {
        // NODE A
        RestPort source_A_1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_A_2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_A_3 = RestPort.builder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestNodeMetamodel nodeA = new RestNodeMetamodel();
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
        var res = inputMapperService.mapInput(variables,List.of(nodeA)).result;

        // ASSERTIONS
        assertNotNull(res);
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
     * Should not try to invent the content of the optional missing variable
     */
    @Test
    @DisplayName("Test with optional fields not satisfied")
    public void shouldWorkWithOnlyOnNodeEvenWithoutAllOptionalFieldsSatisfied() {
        // NODE 1
        RestPort source1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source3 = RestPort.builder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // OPTIONAL FIELDS:
        RestPort source4 = RestPort.builder().withKey("user_id").withSchema(PortSchema.builder().stringSchema().withRequired(false).build()).build();
        RestPort source5 = RestPort.builder().withKey("user_password").withSchema(PortSchema.builder().stringSchema().withRequired(false).build()).build();

        RestNodeMetamodel nodeA = new RestNodeMetamodel();
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

        var res = inputMapperService.mapInput(variables,List.of(nodeA)).result;

        // ASSERTIONS
        assertNotNull(res);
        Map<String, Object> mappedInputs = res.getContext();
        assertNotNull(mappedInputs);
        assertEquals(3, mappedInputs.size());
        assertTrue(mappedInputs.containsKey("user_name"));
        assertTrue(mappedInputs.containsKey("user_email"));
        assertTrue(mappedInputs.containsKey("user_phone"));
    }


    /**
     * Test the Input Mapper with simple mapping and only one unsatisfiable starting node
     * Should produce a null result
     */
    @Test
    @DisplayName("Test with only one unsatisfiable node")
    public void shouldFailWithOnlyOneWrongNode() {
        // NODE 1
        RestPort source1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source3 = RestPort.builder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source4 = RestPort.builder().withKey("user_id").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestNodeMetamodel nodeA = new RestNodeMetamodel();
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

        assertNull(res.result);
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
     *  Should return null as all nodes have unsatisfied required ports
     */
    @Test
    @DisplayName("Test with multiple unsatisfiable nodes")
    public void shouldFailWithMultipleWrongNodes() {

        // NODE A
        RestPort source1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source3 = RestPort.builder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // Unsatisfied port:
        RestPort source4 = RestPort.builder().withKey("user_id").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestNodeMetamodel nodeA = new RestNodeMetamodel();
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
        RestPort source_B_1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_B_2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_B_3 = RestPort.builder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // Unsatisfied port:
        RestPort source_B_4 = RestPort.builder().withKey("user_password").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestNodeMetamodel nodeB = new RestNodeMetamodel();
        nodeB.setId(String.valueOf(UUID.randomUUID()));
        nodeB.setName("CHECK_USER_B");
        nodeB.setDescription("Check if user is registered to our platform using V2 API");
        nodeB.setInputPorts(List.of(source_B_1, source_B_2, source_B_3, source_B_4));
        nodeB.setOutputPorts(List.of());

        // NODE C
        RestPort source_C_1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_C_2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_C_3 = RestPort.builder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // Unsatisfied port:
        RestPort source_C_4 = RestPort.builder().withKey("user_social_security_number").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();


        RestNodeMetamodel nodeC = new RestNodeMetamodel();
        nodeC.setId(String.valueOf(UUID.randomUUID()));
        nodeC.setName("CHECK_USER_C");
        nodeC.setDescription("Check if user is registered to our platform using V1 API");
        nodeC.setInputPorts(List.of(source_C_1, source_C_2,source_C_3,source_C_4));
        nodeC.setOutputPorts(List.of());

        var res = inputMapperService.mapInput(variables,List.of(nodeA, nodeB, nodeC)).result;

        if(res != null){
            // Debug why test is failing
            System.out.println("LLM returned a not-null context:");
            res.context.printContext();
        }

        assertNull(res);
    }


    /**
     * Test the Input Mapper with simple mapping and multiple satisfiable nodes except one.
     *
     * <ul>
     *   <li>Node A is satisfiable</li>
     *   <li>Node B is satisfiable</li>
     *   <li>Node C has a variable (user_social_security_number) that is not among the provided variables but is optional</li>
     *   <li>Node D has a variable (user_social_security_number) that is not among the provided variables and is required</li>
     * </ul>
     *
     * Service should return null as node D cannot be satisfied
     */
    @Test
    @DisplayName("Test with multiple satisfiable nodes and one unsatisfiable node")
    public void shouldFailWithMultipleSatisfiableAndOneUnsatisfiableNode() {
        // NODE 1
        RestPort source1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source3 = RestPort.builder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestNodeMetamodel nodeA = new RestNodeMetamodel();
        nodeA.setId(String.valueOf(UUID.randomUUID()));
        nodeA.setName("CHECK_USER");
        nodeA.setDescription("Check if user is registered to our platform");
        nodeA.setInputPorts(List.of(source1, source2, source3));
        nodeA.setOutputPorts(List.of());

        // NODE B
        RestPort source_B_1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_B_2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_B_3 = RestPort.builder().withKey("user_password").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestNodeMetamodel nodeB = new RestNodeMetamodel();
        nodeB.setId(String.valueOf(UUID.randomUUID()));
        nodeB.setName("CHECK_USER_B");
        nodeB.setDescription("Check if user is registered to our platform using V2 API");
        nodeB.setInputPorts(List.of(source_B_1, source_B_2, source_B_3));
        nodeB.setOutputPorts(List.of());

        // NODE C
        RestPort source_C_1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_C_2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_C_3 = RestPort.builder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // Unsatisfied port:
        RestPort source_C_4 = RestPort.builder().withKey("user_social_security_number").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestNodeMetamodel nodeC = new RestNodeMetamodel();
        nodeC.setId(String.valueOf(UUID.randomUUID()));
        nodeC.setName("CHECK_USER_C");
        nodeC.setDescription("Check if user is registered to our platform using V1 API");
        nodeC.setInputPorts(List.of(source_C_1, source_C_2,source_C_3,source_C_4));
        nodeC.setOutputPorts(List.of());


        // NODE D
        RestPort source_D_1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_D_2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_D_3 = RestPort.builder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // OPTIONAL Unsatisfied port:
        RestPort source_D_4 = RestPort.builder().withKey("user_social_security_number").withSchema(PortSchema.builder().stringSchema().withRequired(false).build()).build();

        RestNodeMetamodel nodeD = new RestNodeMetamodel();
        nodeD.setId(String.valueOf(UUID.randomUUID()));
        nodeD.setName("CHECK_USER_D");
        nodeD.setDescription("Check if user is registered to our platform using V1 API");
        nodeD.setInputPorts(List.of(source_D_1, source_D_2,source_D_3,source_D_4));
        nodeD.setOutputPorts(List.of());


        Map<String, Object> variables = Map.of(
                "name", "Niccolò",
                "number", "+393527624",
                "email", "caselli@gmail.com",
                "user_password", "password123"
        );

        var res = inputMapperService.mapInput(variables,List.of(nodeA, nodeB, nodeC, nodeD));

        // ASSERTIONS
        assertNull(res.result);
    }


    /**
     * Test the Input Mapper with simple mapping and multiple satisfiable nodes
     *
     * <ul>
     *   <li>Node A is satisfiable: it has all required variables satisfied</li>
     *   <li>Node B is satisfiable: it has all required variables satisfied</li>
     *   <li>Node C is satisfiable: it has a variable (user_social_security_number) that is not among the provided variables but is optional</li>
     *   <li>Node D satisfiable: it has a variable (user_social_security_number) that is not among the provided variables but is optional</li>
     * </ul>
     *
     * Service should work as all nodes have all their required ports satisfied
     */
    @Test
    @DisplayName("Test with multiple satisfiable nodes")
    public void shouldWorkWithMultipleSatisfiableNodes() {
        // NODE 1
        RestPort source1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source3 = RestPort.builder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestNodeMetamodel nodeA = new RestNodeMetamodel();
        nodeA.setId(String.valueOf(UUID.randomUUID()));
        nodeA.setName("CHECK_USER");
        nodeA.setDescription("Check if user is registered to our platform");
        nodeA.setInputPorts(List.of(source1, source2, source3));
        nodeA.setOutputPorts(List.of());

        // NODE B
        RestPort source_B_1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_B_2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_B_3 = RestPort.builder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestNodeMetamodel nodeB = new RestNodeMetamodel();
        nodeB.setId(String.valueOf(UUID.randomUUID()));
        nodeB.setName("CHECK_USER_B");
        nodeB.setDescription("Check if user is registered to our platform using V2 API");
        nodeB.setInputPorts(List.of(source_B_1, source_B_2, source_B_3));
        nodeB.setOutputPorts(List.of());

        // NODE C
        RestPort source_C_1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_C_2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_C_3 = RestPort.builder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // Unsatisfied OPTIONAL port:
        RestPort source_C_4 = RestPort.builder().withKey("user_password").withSchema(PortSchema.builder().stringSchema().withRequired(false).build()).build();

        RestNodeMetamodel nodeC = new RestNodeMetamodel();
        nodeC.setId(String.valueOf(UUID.randomUUID()));
        nodeC.setName("CHECK_USER_C");
        nodeC.setDescription("Check if user is registered to our platform using V1 API");
        nodeC.setInputPorts(List.of(source_C_1, source_C_2,source_C_3,source_C_4));
        nodeC.setOutputPorts(List.of());


        // NODE D
        RestPort source_D_1 = RestPort.builder().withKey("user_name").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_D_2 = RestPort.builder().withKey("user_email").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        RestPort source_D_3 = RestPort.builder().withKey("user_phone").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();
        // OPTIONAL Unsatisfied port:
        RestPort source_D_4 = RestPort.builder().withKey("user_social_security_number").withSchema(PortSchema.builder().stringSchema().withRequired(false).build()).build();

        RestNodeMetamodel nodeD = new RestNodeMetamodel();
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

        var res = inputMapperService.mapInput(variables,List.of(nodeA, nodeB, nodeC, nodeD)).result;

        // ASSERTIONS
        assertNotNull(res);
        Map<String, Object> mappedInputs = res.getContext();
        assertNotNull(mappedInputs);
        assertEquals(3, mappedInputs.size());
        assertTrue(mappedInputs.containsKey("user_name"));
        assertTrue(mappedInputs.containsKey("user_email"));
        assertTrue(mappedInputs.containsKey("user_phone"));
    }


    /**
     * Test the Input Mapper with a nested mapping and only 1 starting node
     * Should map successfully the nested structure
     */
    @Test
    @DisplayName("Test with only one node with nested structure")
    public void shouldWorkWithOnlyOneNode_nestedMap() {
        // NODE 1
        RestPort source1 = RestPort.builder()
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

        RestPort source2 = RestPort.builder().withKey("orderId").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();


        RestNodeMetamodel nodeA = new RestNodeMetamodel();
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

        var res = inputMapperService.mapInput(variables, List.of(nodeA)).result;

        assertNotNull(res);

        System.out.println("Context result: ");
        res.context.printContext();

        assertEquals(res.getContext().get("user.id"), variables.get("id_of_the_user"));
        assertEquals(res.getContext().get("user.userDetails.email"), variables.get("email"));
        assertEquals(res.getContext().get("user.userDetails.phone_number"),variables.get("number"));
        assertEquals(res.getContext().get("orderId"), variables.get("order_id"));
    }

    /**
     * Test the Input Mapper with a list of maps and only 1 starting node
     * Should map successfully the nested structure
     * For AI4NE & NE4AI
     */
    @Test
    @DisplayName("Test with only one node with a list mapping")
    public void shouldWorkWithOnlyOneNode_list() {

        RestPort source1 = RestPort.builder()
                .withKey("technical_requirements")
                .withSchema(PortSchema.builder()
                        .withRequired(true)
                        .arraySchema(
                                PortSchema.builder()
                                        .stringSchema()
                                        .withRequired(true)
                                        .build()
                        )
                        .build()).build();


        RestNodeMetamodel nodeA = new RestNodeMetamodel();
        nodeA.setId(String.valueOf(UUID.randomUUID()));
        nodeA.setName("");
        nodeA.setDescription("Start the AI4NE or NE4AI workflow with a list of requirements");
        nodeA.setInputPorts(List.of(source1));
        nodeA.setOutputPorts(List.of());

        Map<String, Object> variables = Map.of(
                "COMPLETE_USER_REQUEST", "I want to set a real-time connection with my friend Mario",
                "RESOLUTION", "4K",
                "LATENCY", "Low",
                "TARGET", "Mario"
        );

        var res = inputMapperService.mapInput(variables, List.of(nodeA)).result;

        assertNotNull(res);

        System.out.println("Context result: ");
        res.context.printContext();
        assertInstanceOf(List.class, res.getContext().get("technical_requirements"));

        @SuppressWarnings("unchecked")
        List<String> technical_requirements = (List<String>) res.getContext().get("technical_requirements");
        assertNotNull(technical_requirements);
        // check that among the requirements there is "4K" and "Low Latency" (case insensitive) or include them
        assertTrue(technical_requirements.stream().anyMatch(req -> req.toLowerCase().contains("4k")));
        assertTrue(technical_requirements.stream().anyMatch(req -> req.toLowerCase().contains("low")));


    }


    /**
     * Test the Input Mapper with a list of maps and only 1 starting node
     * Should map successfully the nested structure
     * For AI4NE & NE4AI
     */
    @Test
    @DisplayName("Test with only one node with nested structure")
    public void shouldWorkWithOnlyOneNode_listOfMaps() {
        // NODE 1
        RestPort source1 = RestPort.builder()
                .withKey("technical_requirements")
                .withSchema(PortSchema.builder()
                        .arraySchema(
                            PortSchema.builder()
                                .objectSchema(Map.of(
                                        "value", PortSchema.builder().stringSchema().withRequired(true).build(),
                                        "key", PortSchema.builder().stringSchema().withRequired(false).build()
                                ))
                                .withRequired(true)
                                .build()
                        )
                        .build()).build();

        RestPort source2 = RestPort.builder().withKey("service_type").withSchema(PortSchema.builder().stringSchema().withRequired(true).build()).build();

        RestNodeMetamodel nodeA = new RestNodeMetamodel();
        nodeA.setId(String.valueOf(UUID.randomUUID()));
        nodeA.setName("Starting gateway");
        nodeA.setDescription("Start the AI4NE or NE4AI workflow with a list of requirements");
        nodeA.setInputPorts(List.of(source1, source2));
        nodeA.setOutputPorts(List.of());

        Map<String, Object> variables = Map.of(
                "COMPLETE_USER_REQUEST", "I want to set a real-time connection with my friend Mario",
                "RESOLUTION", "4K",
                "LATENCY", "Low",
                "TARGET", "Mario"
        );

        var res = inputMapperService.mapInput(variables, List.of(nodeA)).result;

        assertNotNull(res);

        System.out.println("Context result: ");
        res.context.printContext();

        assertInstanceOf(List.class, res.getContext().get("technical_requirements"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> technical_requirements = (List<Map<String, String>>) res.getContext().get("technical_requirements");
        assertNotNull(technical_requirements);
        // check that among the requirements there is "4K" and "Low Latency" (case insensitive) or include them
        assertTrue(technical_requirements.stream().anyMatch(req -> req.get("value").toLowerCase().contains("4k")));
        assertTrue(technical_requirements.stream().anyMatch(req -> req.get("value").toLowerCase().contains("low")));
    }



    /**
     * Test the Input Mapper when no variables are provided but they can be extracted from the text input
     */
    @Test
    @DisplayName("Test with only one node with a list mapping")
    public void shouldWorkWithNoVariablesButTextRequest() {

        RestPort source1 = RestPort.builder()
                .withKey("technical_requirements")
                .withSchema(PortSchema.builder()
                        .withRequired(true)
                        .arraySchema(
                                PortSchema.builder()
                                        .stringSchema()
                                        .withRequired(true)
                                        .build()
                        )
                        .build()).build();


        RestNodeMetamodel nodeA = new RestNodeMetamodel();
        nodeA.setId(String.valueOf(UUID.randomUUID()));
        nodeA.setName("");
        nodeA.setDescription("Start the AI4NE or NE4AI workflow with a list of requirements");
        nodeA.setInputPorts(List.of(source1));
        nodeA.setOutputPorts(List.of());

        Map<String, Object> variables = Map.of(); // <---- empty

        String request = "I want to a 4k streaming with no more latency the 10ms";

        var res = inputMapperService.mapInput(variables, List.of(nodeA), request).result;

        assertNotNull(res);

        System.out.println("Context result: ");
        res.context.printContext();
        assertInstanceOf(List.class, res.getContext().get("technical_requirements"));

        @SuppressWarnings("unchecked")
        List<String> technical_requirements = (List<String>) res.getContext().get("technical_requirements");
        assertNotNull(technical_requirements);
        assertTrue(technical_requirements.stream().anyMatch(req -> req.toLowerCase().contains("4k")));
        assertTrue(technical_requirements.stream().anyMatch(req -> req.toLowerCase().contains("10ms")));
    }
}
