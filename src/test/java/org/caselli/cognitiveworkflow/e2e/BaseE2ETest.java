package org.caselli.cognitiveworkflow.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("e2e")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseE2ETest {


    @Autowired
    protected MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        // Before each test all the documents of the test database are deleted
        // We delete the documents instead of dropping the tables because we want to
        // preserve collection indexes
        cleanDocuments();
    }

    public void cleanDocuments() {
        mongoTemplate.getCollectionNames()
                .stream()
                .filter(name -> !name.startsWith("system."))
                .forEach(collectionName -> mongoTemplate.remove(new Query(), collectionName));
    }
}