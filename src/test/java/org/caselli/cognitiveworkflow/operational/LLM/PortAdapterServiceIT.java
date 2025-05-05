package org.caselli.cognitiveworkflow.operational.LLM;

import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class PortAdapterServiceIT {

    @Autowired private PortAdapterService portAdapterService;

    @Test
    public void PortAdapterShouldWorkWithSimplePorts() {
        List<Port> sources = new ArrayList<>();
        // SOURCE
        Port source1 = Port.builder()
                .withKey("source1")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        Port source2 = Port.builder()
                .withKey("source2")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        sources.add(source1);
        sources.add(source2);

        // TARGET
        List<Port> targets = new ArrayList<>();

        Port target1 = Port.builder()
                .withKey("target1")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        Port target2 = Port.builder().withKey("target2")
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
        PortAdaptation adapter = this.portAdapterService.adaptPorts(sources, targets);

        // List adapter ports
        Map<String,String> adapterPorts = adapter.getBindings();

        for (String key : adapterPorts.keySet())
            System.out.println("Adapter port: " + key + " -> " + adapterPorts.get(key));



        // Check if the adapter ports are correct
        assertEquals(adapterPorts.get("source1"), "target1");
        assertEquals(adapterPorts.get("source2"), "target2");
    }


    @Test
    public void PortAdapterShouldWorkWithSimpleNestedPorts() {
        // SOURCE
        PortSchema source1Schema = PortSchema.builder()
                .objectSchema(Map.of(
                        "userDetails", PortSchema.builder()
                                .objectSchema(Map.of(
                                        "email", PortSchema.builder().stringSchema().withRequired(true).build(),
                                        "phone", PortSchema.builder().stringSchema().withRequired(false).build()
                                ))
                                .withRequired(true)
                                .build(),
                        "orderId", PortSchema.builder().stringSchema().withRequired(true).build()
                ))
                .build();

        Port source1 = Port.builder()
                .withKey("source")
                .withSchema(source1Schema)
                .build();

        List<Port> sources = List.of(source1);

        // TARGET
        Port target1 = Port.builder()
                .withKey("email")
                .withSchema(PortSchema.builder()
                        .stringSchema()
                        .withRequired(true)
                        .build())
                .build();

        Port target2 = Port.builder()
                .withKey("phone")
                .withSchema(PortSchema.builder()
                        .stringSchema()
                        .withRequired(true)
                        .build())
                .build();

        Port target3 = Port.builder()
                .withKey("orderId")
                .withSchema(PortSchema.builder()
                        .stringSchema()
                        .withRequired(true)
                        .build())
                .build();

        List<Port> targets = List.of(target1, target2, target3);

        // ADAPTER
        PortAdaptation adapter = this.portAdapterService.adaptPorts(sources, targets);

        // List adapter ports
        Map<String,String> adapterPorts = adapter.getBindings();
        for (Map.Entry<String, String> entry : adapterPorts.entrySet()) {
            System.out.println("Adapter port: " + entry.getKey() + " -> " + entry.getValue());
        }

        // Check if the adapter ports are correct
        assertEquals(adapterPorts.get("source.userDetails.email"), "email");
        assertEquals(adapterPorts.get("source.userDetails.phone"), "phone");
        assertEquals(adapterPorts.get("source.orderId"), "orderId");
        assertEquals(adapterPorts.size(), 3);


        // Check the inversion of the adapter ports
        PortAdaptation invertedAdapter = this.portAdapterService.adaptPorts(targets,sources);
        assertEquals(invertedAdapter.getBindings().get("email"), "source.userDetails.email");
        assertEquals(invertedAdapter.getBindings().get("phone"), "source.userDetails.phone");
        assertEquals(invertedAdapter.getBindings().get("orderId"), "source.orderId");
        assertEquals(invertedAdapter.getBindings().size(), 3);
    }
}
