package org.caselli.cognitiveworkflow.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkflowParser {
    @Autowired private ObjectMapper mapper;
    @Autowired private WorkflowNodeFactory factory;

    public List<WorkflowNodeDescriptor> loadDescriptors(InputStream jsonStream) throws Exception {
        return mapper.readValue(jsonStream, new TypeReference<>() {});
    }

    public List<WorkflowNode> buildNodes(List<WorkflowNodeDescriptor> descriptors) throws Exception {
        List<WorkflowNode> nodes = new ArrayList<>();
        for (WorkflowNodeDescriptor desc : descriptors) {
            System.out.println(desc);
            nodes.add(factory.create(desc));
        }

        System.out.println("✓ Built nodes");

        return nodes;
    }

    public List<WorkflowNode> loadFromClasspath(String resourcePath) throws Exception {
        try (InputStream is = new ClassPathResource(resourcePath).getInputStream()) {

            List<WorkflowNodeDescriptor> descriptors = loadDescriptors(is);
            System.out.println("✓ Loaded descriptors");

            return buildNodes(descriptors);
        }
    }
}
