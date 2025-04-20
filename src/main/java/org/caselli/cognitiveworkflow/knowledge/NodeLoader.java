package org.caselli.cognitiveworkflow.knowledge;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.util.List;

@Component
public class NodeLoader {
    private final ObjectMapper mapper;

    public NodeLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<WorkflowNodeDescriptor> loadFromClasspath(String path) throws Exception {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(is, new TypeReference<>() {});
        }
    }
}
