package ch.softhenge.solarlog.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The Weather Service is responsible to get weather information from a chosen location
 */
@Service
public class WeatherService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String WEATHER_PROPERTIES_FILE_LOC = "/weatherapiproperties.json";
    private static final String WEATHER_URI = "http://api.openweathermap.org/data/2.5/weather?q=neuenhof,ag,ch&APPID=c6409c4bc73119f3a3bba68f4eaebcb6&units=metric";
    @Autowired
    private RestTemplate restTemplate;

    public WeatherService() {
        readWeatherPropertiesFile();
    }

    public String getWeatherDataAsString() {
        return restTemplate.getForObject(WEATHER_URI, String.class);
    }

    /**
     *
     * @return
     */
    public WeatherProperties readWeatherPropertiesFile() {
        try {
            String jsonFile = IOUtils.resourceToString(WEATHER_PROPERTIES_FILE_LOC, StandardCharsets.UTF_8);
            return new Gson().fromJson(jsonFile, WeatherProperties.class);
        } catch (IOException e) {
            logger.error("Reading the properties file {} went wrong", WEATHER_PROPERTIES_FILE_LOC, e.getStackTrace());
            throw new RuntimeException(e);
        }
    }
}
