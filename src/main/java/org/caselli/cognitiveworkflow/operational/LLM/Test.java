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

    private final IntentDetectionService intentDetectionService;
    private final EmbeddingService embeddingService;
    private final PortAdapterService portAdapterService;

    public Test(IntentDetectionService intentDetectionService, PortAdapterService portAdapterService, EmbeddingService embeddingService) {
        this.intentDetectionService = intentDetectionService;
        this.portAdapterService = portAdapterService;
        this.embeddingService = embeddingService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent e) {
        //testIntentDetector();
       testAdapter2();
       testAdapter1();
    }

    public void testIntentDetector() {
        //String userInput = "i like food";
        //String userInput = "qkjd";
        //String userInput = "I want to translate 'money' to french";
        String userInput = "I want to buy a new Iphone 16 pro for my wife and I need it to be delivered by tomorrow";

        IntentDetectorResult result = this.intentDetectionService.detect(userInput);

        System.out.println("Intent detection result: " + result);

        if(result != null)
        {


            System.out.println("Intent: " + result.getIntentName());
            System.out.println("Confidence: " + result.getConfidence());
            System.out.println("Is new: " + result.isNew());
            System.out.println("User variables: " + result.getUserVariables());
        }
    }

    public void testEmbeddings(){
        var res = this.embeddingService.generateEmbedding("Hello world");
        System.out.println("Embedding: " + res);
        System.out.println("Embedding size: " + res.size());
    }

    public void testAdapter1(){

        List<Port> sources = new ArrayList<>();

        // Using builder for source ports
        Port source1 = Port.builder()
                .withKey("source1")
                .withSchema(PortSchema.builder()
                        .intSchema()
                        .withRequired(true)
                        .build())
                .build();

        Port source2 = Port.builder()
                .withKey("source2")
                .withSchema(PortSchema.builder()
                        .stringSchema()
                        .withRequired(true)
                        .build())
                .build();

        sources.add(source1);
        sources.add(source2);

        // Using builder for target ports
        List<Port> targets = new ArrayList<>();

        Port target1 = Port.builder()
                .withKey("target1")
                .withSchema(PortSchema.builder()
                        .stringSchema()
                        .withRequired(true)
                        .build())
                .build();

        Port target2 = Port.builder()
                .withKey("target2")
                .withSchema(PortSchema.builder()
                        .stringSchema()
                        .withRequired(true)
                        .build())
                .build();

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


    public void testAdapter2() {
        // SOURCE
        PortSchema source1Schema = PortSchema.builder()
                .objectSchema(Map.of(
                        "userDetails", PortSchema.builder()
                                .objectSchema(Map.of(
                                        "email", PortSchema.builder()
                                                .stringSchema()
                                                .withRequired(true)
                                                .build(),
                                        "phone", PortSchema.builder()
                                                .stringSchema()
                                                .withRequired(false)
                                                .build()
                                ))
                                .withRequired(true)
                                .build(),
                        "orderId", PortSchema.builder()
                                .stringSchema()
                                .withRequired(true)
                                .build()
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
        PortAdaptation adapter = this.portAdapterService.adaptPorts(targets, sources);

        // List adapter ports
        Map<String,String> adapterPorts = adapter.getBindings();
        for (Map.Entry<String, String> entry : adapterPorts.entrySet()) {
            System.out.println("Adapter port: " + entry.getKey() + " -> " + entry.getValue());
        }
    }


}
