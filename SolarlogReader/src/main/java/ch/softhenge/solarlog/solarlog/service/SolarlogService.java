package ch.softhenge.solarlog.solarlog.service;

import ch.softhenge.solarlog.solarlog.pojo.Solarlog300Data;
import ch.softhenge.solarlog.solarlog.property.SolarlogProperties;
import ch.softhenge.solarlog.solarlog.property.Solarlogproperty;
import com.google.gson.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This Service is able to read the Solarlog REST Interface as well as the solarlog csv exports
 */
@Service
public class SolarlogService implements ISolarlogService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String SOLARLOG_PROPERTIES_FILE_LOC = "/solarlogproperties.json";
    private static final String SOLARLOG_URI_TEMPLATE = "${baseURL}/getjp";

    private static final String SOLARLOG_300_POST = "{\"801\":{\"170\":null}}";

    private final RestTemplate restTemplate;
    private final SolarlogProperties solarlogProperties;

    /**
     * Constructor
     *
     * @param restTemplate the rest Template to call API's
     */
    @Autowired
    public SolarlogService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.solarlogProperties = readSolarlogPropertiesFile();
    }

    /**
     * Reads the solarlog from API as a String
     *
     * @param solarlogName the name of the solarlog according to the solarlog properties file
     * @return the JSON return as a String
     */
    @Override
    public String getSolarlog300DataFromAPIAsString(String solarlogName) {
        try {
            return restTemplate.postForObject(enrichSolarlogURL(solarlogName), SOLARLOG_300_POST, String.class);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Solarlog 300 with the name " + solarlogName + " does not exist " + e);
        }
    }

    /**
     * Reads the Solarlog 300 and returns the Object that was created out of the json
     *
     * @param solarlogName the name of the solarlog according to the solarlog properties file
     * @return the Solarlog300Object
     */
    @Override
    public Solarlog300Data getSolarog300DataFromAPI(String solarlogName) {
        return new Solarlog300Data(getSolarlog300DataFromAPIAsString(solarlogName));
    }

    /**
     * read the solarlog properties file from classpath
     *
     * @return the solarlogProperties file
     */
    protected SolarlogProperties readSolarlogPropertiesFile() {
        try {
            String jsonFile = IOUtils.resourceToString(SOLARLOG_PROPERTIES_FILE_LOC, StandardCharsets.UTF_8);
            return new Gson().fromJson(jsonFile, SolarlogProperties.class);
        } catch (IOException e) {
            logger.error("Reading the solarlog properties file {} went wrong: ", SOLARLOG_PROPERTIES_FILE_LOC + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param solarlogName the name of the solarlog according to the solarlog properties file
     * @return the solarlogProperty object
     */
    protected Solarlogproperty getSolarlogproperty(String solarlogName) {
        return solarlogProperties.getSolarlogPropertyBySolarlogName(solarlogName);
    }

    /**
     * returns the base URL for the solarlog
     *
     * @param solarlogName the name of the solarlog according to the solarlog properties file
     * @return the complete URL
     */
    private String enrichSolarlogURL(String solarlogName) {
        Map<String, String> valuesMap = new HashMap<>();
        Solarlogproperty solarlogProperty = getSolarlogproperty(solarlogName);
        if (solarlogProperty == null) {
            throw new RuntimeException("The Solarlog Property File " + SOLARLOG_PROPERTIES_FILE_LOC + " doesn't contain the solarlogger with the name '" + solarlogName + "'");
        }
        valuesMap.put("baseURL", solarlogProperty.getSolarlogbaseurl());
        StringSubstitutor sub = new StringSubstitutor(valuesMap);
        return sub.replace(SOLARLOG_URI_TEMPLATE);
    }

}


