package ch.softhenge.solarlog.weather;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
class WeatherServiceTest {

    @Autowired
    private RestTemplate restTemplate;
    private WeatherService weatherService;

    @BeforeEach
    void beforeTest() {
        this.weatherService = new WeatherService("Neuenhof", restTemplate);
    }


    @Test
    public void testWeatherService() {
        String weatherData = weatherService.getWeatherDataAsString();
        Assertions.assertNotNull(weatherData);
    }

    @Test
    void readWeatherPropertiesFile() {
        WeatherProperties wp = weatherService.readWeatherPropertiesFile();
        Assertions.assertEquals("metric", wp.getWeatherunit());
    }
}