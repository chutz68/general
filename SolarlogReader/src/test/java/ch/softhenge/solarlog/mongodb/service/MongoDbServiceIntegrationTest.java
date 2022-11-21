package ch.softhenge.solarlog.mongodb.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest

public class MongoDbServiceIntegrationTest {

    private MongodbService mongoDbService;

    @Test
    public void testMongoDbService() {
        mongoDbService = new MongodbService("dev");
    }
}
