package ch.softhenge.solarlog.mongodb.service;

import ch.softhenge.solarlog.solarlog.pojo.SolarlogData5Min;
import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        Bson query = new Document("record_timestamp", ldtafter5Minutes);
        long docs = mongoDbService.getCollection5MinData().countDocuments();
        System.out.println("has " + docs + " records");

        DeleteResult deleteResult1 = mongoDbService.getCollection5MinData().deleteMany(query);
        System.out.println("Deleted " + deleteResult1 + " records");

        String jsonFile = IOUtils.resourceToString("/record5min.json", StandardCharsets.UTF_8);
        InsertOneResult insertOneResult = mongoDbService.insertOneInto5MinData(jsonFile);
        assertThat(insertOneResult.getInsertedId(), is(notNullValue()));

        SolarlogData5Min solarlogData5Min = new Gson().fromJson(jsonFile, SolarlogData5Min.class);
        DeleteResult deleteResult = mongoDbService.deleteOneFrom5MinData(solarlogData5Min.getRecordTimestampAsInstant());
        assertThat(deleteResult.getDeletedCount(), is(greaterThanOrEqualTo(1L)));
    }

    @Test
    public void testReadRecordFromDB() {
        LocalDate fromDate = LocalDate.of(2022, 10, 11);
        LocalDate toDate = LocalDate.of(2022, 10, 12);
        FindIterable<SolarlogData5Min> solarlogData5MinsIterable = mongoDbService.readSolarlogData5MinByRecordDate(fromDate, toDate);
        List<SolarlogData5Min> solarlogDataList = mongoDbService.getListFromIterable(solarlogData5MinsIterable);
        assertThat(solarlogDataList.size(), is(equalTo(1)));
    }
}
