package org.caselli.cognitiveworkflow.operational.LLM;

import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// TODO REMOVE ALL CLASS
@Service
public class Test implements ApplicationListener<ApplicationReadyEvent> {

    private final IntentManager intentManager;
    private final PortAdapterService portAdapterService;

    public Test(IntentManager intentManager, PortAdapterService portAdapterService) {
        this.intentManager = intentManager;
        this.portAdapterService = portAdapterService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent e) {



        testAdapter2();


    }
    public void testAdapter1(){

        List<Port> sources = new ArrayList<>();

        Port source1 = new Port();
        PortSchema sourceSchema1 = new PortSchema(PortType.INT, true);
        source1.setKey("source1");
        source1.setSchema(sourceSchema1);

        Port source2 = new Port();
        PortSchema sourceSchema2 = new PortSchema(PortType.STRING, true);
        source2.setKey("source2");
        source2.setSchema(sourceSchema2);

        sources.add(source1);
        sources.add(source2);



        // TARGET
        List<Port> targets = new ArrayList<>();
        Port target1 = new Port();
        PortSchema targetSchema1 = new PortSchema(PortType.STRING, true);
        target1.setKey("target1");
        target1.setSchema(targetSchema1);

        Port target2 = new Port();
        PortSchema targetSchema2 = new PortSchema(PortType.STRING, true);
        target2.setKey("target2");
        target2.setSchema(targetSchema2);

        targets.add(target1);
        targets.add(target2);

        // ADAPTER
        PortAdaptation adapter = this.portAdapterService.adaptPorts(sources, targets);

        // List adapter ports
        Map<String,String> adapterPorts = adapter.getBindings();
        for (String key : adapterPorts.keySet()) {
            System.out.println("Adapter port: " + key + " -> " + adapterPorts.get(key));
        }

    }



    public void testAdapter2(){

        // SOURCE
        PortSchema source1Schema = new PortSchema();
        source1Schema.setType(PortType.OBJECT);
        source1Schema.setProperties(Map.of(
                "userDetails", new PortSchema(PortType.OBJECT, Map.of(
                        "email", new PortSchema(PortType.STRING, true),
                        "phone", new PortSchema(PortType.STRING, false)
                ), true),
                "orderId", new PortSchema(PortType.STRING, true)
        ));

        Port source1 = new Port();
        source1.setKey("source");
        source1.setSchema(source1Schema);

        List<Port> sources = new ArrayList<>();
        sources.add(source1);

         // TARGET
        PortSchema target1Schema = new PortSchema(PortType.STRING, true);
        Port target1 = new Port();
        target1.setKey("email");
        target1.setSchema(target1Schema);

        PortSchema target2Schema = new PortSchema(PortType.STRING, true);
        Port target2 = new Port();
        target2.setKey("phone");
        target2.setSchema(target2Schema);

        PortSchema target3Schema = new PortSchema(PortType.STRING, true);
        Port target3 = new Port();
        target3.setKey("orderId");
        target3.setSchema(target3Schema);

        List<Port> targets = new ArrayList<>();
        targets.add(target1);
        targets.add(target2);
        targets.add(target3);

        // ADAPTER
        PortAdaptation adapter = this.portAdapterService.adaptPorts(targets,sources);
        // List adapter ports
        Map<String,String> adapterPorts = adapter.getBindings();
        for (String key : adapterPorts.keySet()) {
            System.out.println("Adapter port: " + key + " -> " + adapterPorts.get(key));
        }

    }
}
