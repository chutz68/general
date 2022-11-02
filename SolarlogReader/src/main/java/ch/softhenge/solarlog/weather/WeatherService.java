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
 */
public class WeatherService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String WEATHER_PROPERTIES_FILE_LOC = "/weatherapiproperties.json";
    private static final String WEATHER_URI_TEMPLATE = "http://api.openweathermap.org/data/2.5/weather?q=${locationapiurl}&APPID=${weatherapikey}&units=${weatherunit}";
    private final String WEATHER_URI;
    private final RestTemplate restTemplate;

    /**
     * Creates a Weather Service for the chosen location
     *
     * @param location     Is defined by the weatherAPI. locations can be e.g. Neuenhof
     * @param restTemplate the restTemplate
     */
    public WeatherService(String location, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        WeatherProperties weatherProperties = readWeatherPropertiesFile();
        WeatherLocation weatherLocation = new WeatherLocation(weatherProperties, location);
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("locationapiurl", weatherLocation.getLocationurl());
        valuesMap.put("weatherapikey", weatherLocation.getApiKey());
        valuesMap.put("weatherunit", weatherLocation.getUnit());
        StringSubstitutor sub = new StringSubstitutor(valuesMap);
        WEATHER_URI = sub.replace(WEATHER_URI_TEMPLATE);
    }

    public String getWeatherDataAsString() {
        return restTemplate.getForObject(WEATHER_URI, String.class);
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
