package ch.softhenge.solarlog.mongodb.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest

public class MongoDbServiceIntegrationTest {

    @Test
    public void testMongoDbServiceConnection() {
        MongodbService mongoDbService = new MongodbService("dev");
    }

    @Test
    public void testMongoDbServiceInvalidDb() {
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> new MongodbService("invalid"));
        assertThat(runtimeException.getMessage(), startsWith("The Database with the name"));
    }
}
