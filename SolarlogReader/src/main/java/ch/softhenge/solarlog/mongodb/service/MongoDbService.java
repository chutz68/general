package ch.softhenge.solarlog.mongodb.service;

import ch.softhenge.solarlog.mongodb.property.MongodbProperties;
import ch.softhenge.solarlog.mongodb.property.Mongodbdatabasis;
import ch.softhenge.solarlog.solarlog.pojo.SolarlogData5Min;
import com.google.gson.Gson;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

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
     * Insert one SolarlogData5Min into the 5-min collection
     *
     * @param solarlogData5Min the object that should be written
     * @return the result
     */
    public InsertOneResult insertOneInto5MinData(SolarlogData5Min solarlogData5Min) {
        String solDataAsJson = new Gson().toJson(solarlogData5Min);
        return getCollection5MinData().insertOne(Document.parse(solDataAsJson));
    }

    /**
     * Delete one record from the 5-min collection based on the createddate
     *
     * @param createdDateTime the datetime object of the record
     * @return the result
     */
    public DeleteResult deleteOneFrom5MinData(LocalDateTime createdDateTime) {
        String createdDateTimeAsString = createdDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Bson query = eq("record_timestamp", createdDateTimeAsString);
        return getCollection5MinData().deleteOne(query);
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