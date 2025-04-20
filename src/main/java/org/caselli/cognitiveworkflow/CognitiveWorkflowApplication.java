package org.caselli.cognitiveworkflow;

import org.caselli.cognitiveworkflow.core.WorkflowEngine;
import org.caselli.cognitiveworkflow.core.WorkflowNode;
import org.caselli.cognitiveworkflow.core.WorkflowParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class CognitiveWorkflowApplication implements CommandLineRunner {

    @Autowired
    private WorkflowParser parser;

    public static void main(String[] args) {
        SpringApplication.run(CognitiveWorkflowApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        List<WorkflowNode> nodes = parser.loadFromClasspath("workflow.json");
        WorkflowEngine engine = new WorkflowEngine(nodes);


        Map<String, Object> input = new HashMap<>();
        input.put("userQuestion", "What is SBOM?");

        Map<String, Object> output = engine.execute(input);
        System.out.println("Workflow result: " + output);
    }

}
