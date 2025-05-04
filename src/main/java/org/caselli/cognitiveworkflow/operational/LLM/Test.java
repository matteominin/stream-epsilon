package org.caselli.cognitiveworkflow.operational.LLM;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

// TODO REMOVE ALL CLASS
@Service
public class Test implements ApplicationListener<ApplicationReadyEvent> {

    private final IntentManager intentManager;

    public Test(IntentManager intentManager) {
        this.intentManager = intentManager;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent e) {
        System.out.println("Application is ready! - TESTING");

        var res = this.intentManager.determineIntent("Hello, how can I help you?"); // Example input
        System.out.println("Intent detected: " + res);
    }
}
