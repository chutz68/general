package ch.softhenge.solarlog.mongodb.service;

import ch.softhenge.solarlog.solarlog.pojo.SolarlogData5Min;
import com.google.gson.Gson;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest

public class MongoDbServiceIntegrationTest {

    @Test
    public void testMongoDbServiceInvalidDb() {
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> new MongodbService("invalid"));
        assertThat(runtimeException.getMessage(), startsWith("The Database with the name"));
    }

    @Test
    public void testInsertOneInto5MinTable() throws IOException {
        String jsonFile = IOUtils.resourceToString("/record5min.json", StandardCharsets.UTF_8);
        SolarlogData5Min solarlogData5Min = new Gson().fromJson(jsonFile, SolarlogData5Min.class);
        MongodbService mongoDbService = new MongodbService("dev");
        InsertOneResult insertOneResult = mongoDbService.insertOneInto5MinData(solarlogData5Min);
        assertThat(insertOneResult.getInsertedId(), is(notNullValue()));

        DeleteResult deleteResult = mongoDbService.deleteOneFrom5MinData(solarlogData5Min.getRecordTimestampAsDate());
        assertThat(deleteResult.getDeletedCount(), is(greaterThanOrEqualTo(1L)));
    }
}
