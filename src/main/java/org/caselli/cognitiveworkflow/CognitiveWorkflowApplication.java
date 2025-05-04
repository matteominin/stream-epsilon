package org.caselli.cognitiveworkflow;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class CognitiveWorkflowApplication  {

    public static void main(String[] args) {
        SpringApplication.run(CognitiveWorkflowApplication.class, args);
    }
}
