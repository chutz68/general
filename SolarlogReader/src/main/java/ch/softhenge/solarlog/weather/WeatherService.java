package ch.softhenge.solarlog.weather;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The Weather Service is responsible to get weather information from a chosen location
 * Check <a href="https://openweathermap.org/api">...</a>
 * We are using API Version 2.5 and are allowed to do 1000 API calls per day for free.
 */
public class WeatherService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String WEATHER_PROPERTIES_FILE_LOC = "/weatherapiproperties.json";
    private static final String WEATHER_URI_TEMPLATE = "http://api.openweathermap.org/data/2.5/weather?q=${locationapiurl}&APPID=${weatherapikey}&units=${weatherunit}";
    private final String WEATHER_URI;
    private final RestTemplate restTemplate;
    private final String location;

    /**
     * Creates a Weather Service for the chosen location
     *
     * @param location     Is defined by the weatherAPI. locations can be e.g. Neuenhof
     * @param restTemplate the restTemplate
     */
    public WeatherService(String location, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.location = location;
        WeatherProperties weatherProperties = readWeatherPropertiesFile();
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("locationapiurl", weatherProperties.getWetherLocationApiUrlByLocationname(location));
        valuesMap.put("weatherapikey", weatherProperties.getWeatherapikey());
        valuesMap.put("weatherunit", weatherProperties.getWeatherunit());
        StringSubstitutor sub = new StringSubstitutor(valuesMap);
        WEATHER_URI = sub.replace(WEATHER_URI_TEMPLATE);
    }

    /**
     *  read the API call into a simple String.
     * @return contains the json object as a String
     */
    public String getWeatherDataAsString() {
        try {
            return restTemplate.getForObject(WEATHER_URI, String.class);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Location " + this.location + " does not exist");
        }
    }

    /**
     *  read the API call into the WeatherData Object.
     * @return a new WeatherData Object
     */
    public WeatherData getWeatherDataAsObject() {
        try {
            return restTemplate.getForObject(WEATHER_URI, WeatherData.class);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Location " + this.location + " does not exist");
        }
    }

    public String getLocation() {
        return location;
    }

    /**
     * Reads the weather properties json file
     *
     * @return the weather properties object containing the content of the json file as a java object
     */
    protected WeatherProperties readWeatherPropertiesFile() {
        try {
            String jsonFile = IOUtils.resourceToString(WEATHER_PROPERTIES_FILE_LOC, StandardCharsets.UTF_8);
            return new Gson().fromJson(jsonFile, WeatherProperties.class);
        } catch (IOException e) {
            logger.error("Reading the properties file {} went wrong: ", WEATHER_PROPERTIES_FILE_LOC + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }
    }
}
