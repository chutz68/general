package ch.softhenge.solarlog.weather;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This test class tests API calls to locations that do not exist or are not supported
 */
@SpringBootTest
class WeatherServiceNegativeTest {

    private static final String LOCATION = "NOT_EXISTING_LOCATION";

    @Autowired
    private WeatherService weatherService;


    @Test()
    public void testWeatherServiceWeatherDataAsString() {
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> weatherService.getWeatherDataAsString(LOCATION));
        assertThat(runtimeException.getMessage(), startsWith("Location " + LOCATION + " does not"));
    }

    @Test
    public void testWeatherServiceWeatherDataAsObject() {
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> weatherService.getWeatherDataAsObject(LOCATION));
        assertThat(runtimeException.getMessage(), startsWith("Location " + LOCATION + " does not"));
    }

    @Test
    void readWeatherPropertiesFile() {
        WeatherProperties wp = weatherService.readWeatherPropertiesFile();
        Assertions.assertEquals("metric", wp.getWeatherunit());
    }
}