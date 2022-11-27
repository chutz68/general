package ch.softhenge.solarlog.mongodb.service;

import ch.softhenge.solarlog.solarlog.pojo.SolarlogData5Min;
import com.google.gson.Gson;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import org.apache.commons.io.IOUtils;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static com.mongodb.client.model.Filters.lt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MongoDbServiceIntegrationTest {

    private static final MongodbService mongoDbService = new MongodbService("dev");

    @Test
    public void testMongoDbServiceInvalidDb() {
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> new MongodbService("invalid"));
        assertThat(runtimeException.getMessage(), startsWith("The Database with the name"));
    }

    @Test
    public void testInsertOneInto5MinTable() throws IOException {
        LocalDateTime ldtafter5Minutes = LocalDateTime.now().plusMinutes(2000);
        Bson query = lt("record_timestamp", ldtafter5Minutes);
        DeleteResult deleteResult1 = mongoDbService.getCollection5MinData().deleteMany(query);
        System.out.println("Deleted " + deleteResult1 + " records");

        String jsonFile = IOUtils.resourceToString("/record5min.json", StandardCharsets.UTF_8);
        SolarlogData5Min solarlogData5Min = new Gson().fromJson(jsonFile, SolarlogData5Min.class);
        InsertOneResult insertOneResult = mongoDbService.insertOneInto5MinData(solarlogData5Min);
        assertThat(insertOneResult.getInsertedId(), is(notNullValue()));

        DeleteResult deleteResult = mongoDbService.deleteOneFrom5MinData(solarlogData5Min.getRecordTimestampAsDate());
        assertThat(deleteResult.getDeletedCount(), is(greaterThanOrEqualTo(1L)));
    }
}