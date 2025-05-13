package org.caselli.cognitiveworkflow.operational.LLM.services;

import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.StandardPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("it")
@ActiveProfiles("test")
public class PortAdapterServiceIT {

    @Autowired private PortAdapterService portAdapterService;


    /**
     * Test the port adapter with simple ports
     */
    @Test
    public void PortAdapterShouldWorkWithSimplePorts() {
        List<Port> sources = new ArrayList<>();
        // SOURCE
        Port source1 = StandardPort.builder()
                .withKey("source1")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        Port source2 = StandardPort.builder()
                .withKey("source2")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        sources.add(source1);
        sources.add(source2);

        // TARGET
        List<Port> targets = new ArrayList<>();

        Port target1 = StandardPort.builder()
                .withKey("target1")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        Port target2 = StandardPort.builder().withKey("target2")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();


        targets.add(target1);
        targets.add(target2);

        System.out.println("Sources: ");
        for (Port source : sources)
            System.out.println("Source port: " + source.getKey() + " -> " + source.getSchema().getType());

        System.out.println("Targets: ");
        for (Port target : targets)
            System.out.println("Target port: " + target.getKey() + " -> " + target.getSchema().getType());

        // ADAPTER
        var adapter = this.portAdapterService.adaptPorts(sources, targets);

        // List adapter ports
        Map<String,String> adapterPorts = adapter.getBindings();

        for (String key : adapterPorts.keySet())
            System.out.println("Adapter port: " + key + " -> " + adapterPorts.get(key));



        // Check if the adapter ports are correct
        assertEquals(adapterPorts.get("source1"), "target1");
        assertEquals(adapterPorts.get("source2"), "target2");
    }

    /**
     * Test the port adapter with nested ports
     */
    @Test
    public void PortAdapterShouldWorkWithSimpleNestedPorts() {
        // SOURCE
        PortSchema source1Schema = PortSchema.builder()
                .objectSchema(Map.of(
                        "userDetails", PortSchema.builder()
                                .objectSchema(Map.of(
                                        "email", PortSchema.builder().stringSchema().withRequired(true).build(),
                                        "phone_number", PortSchema.builder().stringSchema().withRequired(false).build()
                                ))
                                .withRequired(true)
                                .build(),
                        "orderId", PortSchema.builder().stringSchema().withRequired(true).build()
                ))
                .build();

        Port source1 = StandardPort.builder()
                .withKey("source")
                .withSchema(source1Schema)
                .build();

        List<Port> sources = List.of(source1);

        // TARGET
        Port target1 = StandardPort.builder()
                .withKey("email")
                .withSchema(PortSchema.builder()
                        .stringSchema()
                        .withRequired(true)
                        .build())
                .build();

        Port target2 = StandardPort.builder()
                .withKey("phone_number")
                .withSchema(PortSchema.builder()
                        .stringSchema()
                        .withRequired(true)
                        .build())
                .build();

        Port target3 = StandardPort.builder()
                .withKey("orderId")
                .withSchema(PortSchema.builder()
                        .stringSchema()
                        .withRequired(true)
                        .build())
                .build();

        List<Port> targets = List.of(target1, target2, target3);

        // ADAPTER
        var adapter = this.portAdapterService.adaptPorts(sources, targets);

        // List adapter ports
        Map<String,String> adapterPorts = adapter.getBindings();
        for (Map.Entry<String, String> entry : adapterPorts.entrySet()) {
            System.out.println("Adapter port: " + entry.getKey() + " -> " + entry.getValue());
        }

        // Check if the adapter ports are correct
        assertEquals(adapterPorts.get("source.userDetails.email"), "email");
        assertEquals(adapterPorts.get("source.userDetails.phone_number"), "phone_number");
        assertEquals(adapterPorts.get("source.orderId"), "orderId");
        assertEquals(adapterPorts.size(), 3);


        // Check the inversion of the adapter ports
        var invertedAdapter = this.portAdapterService.adaptPorts(targets,sources);
        assertEquals(invertedAdapter.getBindings().get("email"), "source.userDetails.email");
        assertEquals(invertedAdapter.getBindings().get("phone_number"), "source.userDetails.phone_number");
        assertEquals(invertedAdapter.getBindings().get("orderId"), "source.orderId");
        assertEquals(invertedAdapter.getBindings().size(), 3);
    }


    /**
     * Edge case: Test the port adapter with empty source and target ports
     */
    @Test
    public void testEmptySourcePorts() {
        List<Port> sources = Collections.emptyList();

        Port target = StandardPort.builder()
                .withKey("target")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        List<Port> targets = List.of(target);

        var adapter = portAdapterService.adaptPorts(sources, targets);

        assertNotNull(adapter);
        assertTrue(adapter.getBindings().isEmpty());
    }

    /**
     * Edge case: Test the port adapter with empty target ports
     */
    @Test
    public void testEmptyTargetPorts() {
        Port source = StandardPort.builder()
                .withKey("source")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        List<Port> sources = List.of(source);
        List<Port> targets = Collections.emptyList();

        var adapter = portAdapterService.adaptPorts(sources, targets);

        assertNotNull(adapter);
        assertTrue(adapter.getBindings().isEmpty());
    }

    /**
     * Edge case: Test the port adapter with empty source and target ports. Should return empty adapter (No matching)
     */
    @Test
    public void testTypeMismatchShouldNotBind() {
        Port source = StandardPort.builder()
                .withKey("stringValue")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        Port target = StandardPort.builder()
                .withKey("numericValue")
                .withSchema(PortSchema.builder().intSchema().withRequired(true).build())
                .build();

        List<Port> sources = List.of(source);
        List<Port> targets = List.of(target);

        var adapter = portAdapterService.adaptPorts(sources, targets);

        if (adapter.getBindings().containsKey("stringValue")) {
            assertNotEquals("numericValue", adapter.getBindings().get("stringValue"));
        }
    }

    /**
     * Test the port adapter with array types
     */
    @Test
    public void testArrayTypeHandling() {
        Port source = StandardPort.builder()
                .withKey("items")
                .withSchema(PortSchema.builder()
                        .arraySchema(PortSchema.builder().stringSchema().build())
                        .withRequired(true)
                        .build())
                .build();

        Port target = StandardPort.builder()
                .withKey("elements")
                .withSchema(PortSchema.builder()
                        .arraySchema(PortSchema.builder().stringSchema().build())
                        .withRequired(true)
                        .build())
                .build();

        List<Port> sources = List.of(source);
        List<Port> targets = List.of(target);

        var adapter = portAdapterService.adaptPorts(sources, targets);

        assertNotNull(adapter);
        assertEquals("elements", adapter.getBindings().get("items"));
    }

    /**
     * Test when types are not identical but compatible (e.g., int to string)
     */
    @Test
    public void testTypeCompatibilityMatching() {
        Port source = StandardPort.builder()
                .withKey("userId")
                .withSchema(PortSchema.builder().intSchema().withRequired(true).build())
                .build();

        Port target = StandardPort.builder()
                .withKey("userIdString")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        List<Port> sources = List.of(source);
        List<Port> targets = List.of(target);

        var adapter = portAdapterService.adaptPorts(sources, targets);

        assertNotNull(adapter);
        assertEquals("userIdString", adapter.getBindings().get("userId"));
    }

    /**
     * Test when there are multiple potential matches based on naming
     */
    @Test
    public void testMultiplePotentialMatches() {
        Port source = StandardPort.builder()
                .withKey("email")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        Port target1 = StandardPort.builder()
                .withKey("userEmail")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        Port target2 = StandardPort.builder()
                .withKey("contactEmail")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        List<Port> sources = List.of(source);
        List<Port> targets = List.of(target1, target2);

        var adapter = portAdapterService.adaptPorts(sources, targets);

        assertNotNull(adapter);
        assertTrue(adapter.getBindings().containsKey("email"));

        // Should match one of the targets (don't care which one specifically)
        assertTrue(adapter.getBindings().get("email").equals("userEmail") ||
                adapter.getBindings().get("email").equals("contactEmail"));
    }

    /**
     * Test with missing required fields
     */
    @Test
    public void testMissingRequiredFields() {
        Port source1 = StandardPort.builder()
                .withKey("requiredEmail")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        Port source2 = StandardPort.builder()
                .withKey("optionalPhone")
                .withSchema(PortSchema.builder().stringSchema().withRequired(false).build())
                .build();

        // Target only has one required field
        Port target = StandardPort.builder()
                .withKey("email")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        List<Port> sources = List.of(source1, source2);
        List<Port> targets = List.of(target);

        var adapter = portAdapterService.adaptPorts(sources, targets);

        assertNotNull(adapter);
        assertEquals("email", adapter.getBindings().get("requiredEmail"));
    }

    /**
     * Test with different nesting levels
     * (when source and target have different nesting levels)
     */
    @Test
    public void testDifferentNestingLevels() {
        PortSchema sourceSchema = PortSchema.builder()
                .objectSchema(Map.of(
                        "user", PortSchema.builder()
                                .objectSchema(Map.of(
                                        "address", PortSchema.builder()
                                                .objectSchema(Map.of(
                                                        "street", PortSchema.builder().stringSchema().withRequired(true).build(),
                                                        "city", PortSchema.builder().stringSchema().withRequired(true).build(),
                                                        "zip", PortSchema.builder().stringSchema().withRequired(true).build()
                                                ))
                                                .withRequired(true)
                                                .build()
                                ))
                                .withRequired(true)
                                .build()
                ))
                .build();

        Port source = StandardPort.builder()
                .withKey("userData")
                .withSchema(sourceSchema)
                .build();

        // Flat target structure
        Port targetStreet = StandardPort.builder()
                .withKey("street")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        Port targetCity = StandardPort.builder()
                .withKey("city")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        Port targetZip = StandardPort.builder()
                .withKey("postalCode")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        List<Port> sources = List.of(source);
        List<Port> targets = List.of(targetStreet, targetCity, targetZip);

        var adapter = portAdapterService.adaptPorts(sources, targets);

        assertNotNull(adapter);
        assertEquals("street", adapter.getBindings().get("userData.user.address.street"));
        assertEquals("city", adapter.getBindings().get("userData.user.address.city"));
        assertEquals("postalCode", adapter.getBindings().get("userData.user.address.zip"));
    }

    /**
     * Test with boolean types
     */
    @Test
    public void testBooleanTypeHandling() {
        // Test with boolean types
        Port source = StandardPort.builder()
                .withKey("isActive")
                .withSchema(PortSchema.builder().booleanSchema().withRequired(true).build())
                .build();

        Port target = StandardPort.builder()
                .withKey("active")
                .withSchema(PortSchema.builder().booleanSchema().withRequired(true).build())
                .build();

        List<Port> sources = List.of(source);
        List<Port> targets = List.of(target);

        var adapter = portAdapterService.adaptPorts(sources, targets);

        assertNotNull(adapter);
        assertEquals("active", adapter.getBindings().get("isActive"));
    }

    /**
     * Tests if the adapter can map fields that have identical structure but different names
     */
    @Test
    public void testIdenticalStructuresButDifferentFieldNames() {
        PortSchema sourceSchema = PortSchema.builder()
                .objectSchema(Map.of(
                        "person", PortSchema.builder()
                                .objectSchema(Map.of(
                                        "firstName", PortSchema.builder().stringSchema().withRequired(true).build(),
                                        "lastName", PortSchema.builder().stringSchema().withRequired(true).build(),
                                        "age", PortSchema.builder().intSchema().withRequired(true).build()
                                ))
                                .withRequired(true)
                                .build()
                ))
                .build();

        Port source = StandardPort.builder()
                .withKey("sourceData")
                .withSchema(sourceSchema)
                .build();

        PortSchema targetSchema = PortSchema.builder()
                .objectSchema(Map.of(
                        "user", PortSchema.builder()
                                .objectSchema(Map.of(
                                        "givenName", PortSchema.builder().stringSchema().withRequired(true).build(),
                                        "familyName", PortSchema.builder().stringSchema().withRequired(true).build(),
                                        "years", PortSchema.builder().intSchema().withRequired(true).build()
                                ))
                                .withRequired(true)
                                .build()
                ))
                .build();

        Port target = StandardPort.builder()
                .withKey("targetData")
                .withSchema(targetSchema)
                .build();

        List<Port> sources = List.of(source);
        List<Port> targets = List.of(target);

        var adapter = portAdapterService.adaptPorts(sources, targets);

        assertNotNull(adapter);
        // Check that semantically similar fields are mapped correctly
        assertEquals("targetData.user.givenName", adapter.getBindings().get("sourceData.person.firstName"));
        assertEquals("targetData.user.familyName", adapter.getBindings().get("sourceData.person.lastName"));
        assertEquals("targetData.user.years", adapter.getBindings().get("sourceData.person.age"));
    }
}