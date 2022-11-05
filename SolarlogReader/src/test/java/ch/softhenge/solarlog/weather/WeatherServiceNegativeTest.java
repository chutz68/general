package ch.softhenge.solarlog.weather;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This test class tests API calls to locations that do not exist or are not supported
 */
@SpringBootTest
class WeatherServiceNegativeTest {

    public static final String LOCATION = "NOT_EXISTING_LOCATION";
    @Autowired
    private RestTemplate restTemplate;
    private WeatherService weatherService;

    @BeforeEach
    void beforeTest() {
        this.weatherService = new WeatherService(LOCATION, restTemplate);
    }


    @Test()
    public void testWeatherServiceWeatherDataAsString() {
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> weatherService.getWeatherDataAsString());
        assertThat(runtimeException.getMessage(), startsWith("Location " + LOCATION + " does not"));
        assertThat(LOCATION, is(equalTo(weatherService.getLocation())));
    }

    @Test
    public void testWeatherServiceWeatherDataAsObject() {
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> weatherService.getWeatherDataAsObject());
        assertThat(runtimeException.getMessage(), startsWith("Location " + LOCATION + " does not"));
        assertThat(LOCATION, is(equalTo(weatherService.getLocation())));
    }

    @Test
    void readWeatherPropertiesFile() {
        WeatherProperties wp = weatherService.readWeatherPropertiesFile();
        Assertions.assertEquals("metric", wp.getWeatherunit());
        assertThat(LOCATION, is(equalTo(weatherService.getLocation())));
    }
}