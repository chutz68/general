package ch.softhenge.solarlog.mongodb.service;

import ch.softhenge.solarlog.mongodb.property.MongodbProperties;
import ch.softhenge.solarlog.mongodb.property.Mongodbdatabasis;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This service offers connection to the MongoDB and diverse Methods to connect to it
 */
public class MongodbService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String MONGODB_PROPERTIES_FILE_LOC = "/mongodb.json";

    private final MongodbProperties mongodbProperties;
    private final String mongodbUrl;
    private final String mongodbdatabase;

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
        mongodbUrl = sub.replace(mongodbProperties.getMongodburl());
        Mongodbdatabasis mongodbdatabasis = mongodbProperties.getMongodbdatabases().stream()
                .filter((myDatabase) -> myDatabase.getName().equals(databasename))
                .findFirst()
                .orElse(null);
        if (mongodbdatabasis == null) {
            throw new RuntimeException("The Database with the name " + databasename + " does not exist in the mongodb properties file");
        }
        mongodbdatabase = mongodbdatabasis.getDatabasename();
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

/**    ConnectionString connectionString = new ConnectionString("mongodb+srv://wern-user:<password>@wernch.6jpqs67.mongodb.net/?retryWrites=true&w=majority");
    MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .serverApi(ServerApi.builder()
                    .version(ServerApiVersion.V1)
                    .build())
            .build();
    MongoClient mongoClient = MongoClients.create(settings);
    MongoDatabase database = mongoClient.getDatabase("test");

 **/