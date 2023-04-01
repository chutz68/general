package ch.softhenge.solarlog.mongodb.service;

import ch.softhenge.solarlog.mongodb.property.MongodbProperties;
import ch.softhenge.solarlog.mongodb.property.Mongodbdatabasis;
import ch.softhenge.solarlog.solarlog.pojo.SolarlogData5Min;
import ch.softhenge.solarlog.solarlog.pojo.SolarlogData5MinV1;
import com.google.gson.Gson;
import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static com.mongodb.client.model.Filters.*;

/**
 * This service offers connection to the MongoDB and diverse Methods to connect to it
 */
public class MongodbService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String MONGODB_PROPERTIES_FILE_LOC = "/mongodb.json";

    private final MongodbProperties mongodbProperties;
    private final MongoDatabase mongoDB;


    /**
     * Constructor
     *
     * @param databasename name of the database from the mongodb property file
     */
    public MongodbService(String databasename) {
        mongodbProperties = readMongodbPropertiesFile();
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("user", mongodbProperties.getMongodbuser());
        byte[] pwd = Base64.decodeBase64(mongodbProperties.getMongodbpassword());
        valuesMap.put("password", new String(pwd, StandardCharsets.UTF_8));
        valuesMap.put("mongodbcluster", mongodbProperties.getMongodbcluster());
        StringSubstitutor sub = new StringSubstitutor(valuesMap);
        String mongodbUrl = sub.replace(mongodbProperties.getMongodburl());
        Mongodbdatabasis mongodbdatabasis = mongodbProperties.getMongodbdatabases().stream()
                .filter((myDatabase) -> myDatabase.getName().equals(databasename))
                .findFirst()
                .orElse(null);
        if (mongodbdatabasis == null) {
            throw new RuntimeException("The Database with the name '" + databasename + "' does not exist in the mongodb properties file");
        }
        ConnectionString connectionString = new ConnectionString(mongodbUrl);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .serverApi(ServerApi.builder()
                        .version(ServerApiVersion.V1)
                        .build())
                .build();
        MongoClient mongoClient = MongoClients.create(settings);
        mongoDB = mongoClient.getDatabase(mongodbdatabasis.getDatabasename());
    }

    /**
     * Get the Collection that stores the 1 minute data
     *
     * @return the Collection
     */
    public MongoCollection<Document>  getCollection1MinData() {
        return mongoDB.getCollection(mongodbProperties.getMongodbcollection1min());
    }

    /**
     * Get the Collection that stores the 5 minute data
     *
     * @return the Collection
     */
    public MongoCollection<Document> getCollection5MinData() {
        return mongoDB.getCollection(mongodbProperties.getMongodbcollection5min());
    }

    /**
     * Insert one SolarlogData5Minn Java Object into the 5-min collection
     *
     * @param SolarlogData5Minn the object that should be written
     * @return the result
     */
    public InsertOneResult insertOneInto5MinData(SolarlogData5Min SolarlogData5Minn) {
        String solDataAsJson = new Gson().toJson(SolarlogData5Minn);
        return insertOneInto5MinData(solDataAsJson);
    }

    /**
     * Insert one SolarlogData5Minn json String into the 5-min collection
     *
     * @param SolarlogData5MinnJson the Json as String that should be written
     * @return the result
     */
    public InsertOneResult insertOneInto5MinData(String SolarlogData5MinnJson) {
        Document document = Document.parse(SolarlogData5MinnJson);
        return getCollection5MinData().insertOne(document);
    }


    /**
     * Delete one record from the 5-min collection based on the createddate
     *
     * @param createdDateTime the instant object of the record
     * @return the result
     */
    public DeleteResult deleteOneFrom5MinData(Instant createdDateTime) {
        Bson query = eq("record_timestamp", SolarlogData5Min.getISOStringFromInstant(createdDateTime));
        return getCollection5MinData().deleteOne(query);
    }

    /**
     * Requests for 5 min data based on from and to date
     *
     * @param fromDate The Date from which the records should be read including fromDate (>= fromDate)
     * @param toDate The Date to which the records should be read, NOT including toDate (< toDate)
     * @return a FindIterable of SolarlogData5Minn
     */
    public FindIterable<SolarlogData5Min> readSolarlogData5MinByRecordDate(Instant fromDate, Instant toDate) {
        Bson querygte = gte("record_timestamp", SolarlogData5Min.getISOStringFromInstant(fromDate));
        Bson querylt = lt("record_timestamp", SolarlogData5Min.getISOStringFromInstant(toDate));
        return getCollection5MinData().find(and(querygte, querylt), SolarlogData5Min.class);
    }

    public FindIterable<SolarlogData5MinV1> readSolarlogData5MinByRecordDate2(Instant fromDate, Instant toDate) {
        Bson querygte = gte("record_timestamp", SolarlogData5Min.getISOStringFromInstant(fromDate));
        Bson querylt = lt("record_timestamp", SolarlogData5Min.getISOStringFromInstant(toDate));
        return getCollection5MinData().find(and(querygte, querylt), SolarlogData5MinV1.class);
    }

    /**
     * resturns a list of SolarlogData5Minn based of a FindIterable
     *
     * @param iterable the FindIterable
     * @return a list of SolarlogData5Minn
     */
    public List<SolarlogData5Min> getListFromIterable(FindIterable<SolarlogData5Min> iterable) {
        List<SolarlogData5Min> solarlogList = new ArrayList<>();
        MongoCursor<SolarlogData5Min> cursor = iterable.iterator();
        if (cursor.hasNext()) {
            solarlogList.add(cursor.next());
        }
        return solarlogList;
    }

    /**
     *
     * Read the MongoDb Properties File
     * @return the Properties File as Java Object
     */
    protected MongodbProperties readMongodbPropertiesFile() {
        try {
            String jsonFile = IOUtils.resourceToString(MONGODB_PROPERTIES_FILE_LOC, StandardCharsets.UTF_8);
            return new Gson().fromJson(jsonFile, MongodbProperties.class);
        } catch (IOException e) {
            logger.error("Reading the mongodb properties file {} went wrong: ", MONGODB_PROPERTIES_FILE_LOC + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }
    }

}
