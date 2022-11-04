package ch.softhenge.solarlog.weather;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

/**
 * This test class tests API calls to locations that do exist
 */
@SpringBootTest
class WeatherServiceTestPositive {

    public static final String LOCATION = "Neuenhof";
    @Autowired
    private RestTemplate restTemplate;
    private WeatherService weatherService;

    @BeforeEach
    void beforeTest() {
        this.weatherService = new WeatherService(LOCATION, restTemplate);
    }


    /**
     * Expect something like:
     * {"coord":{"lon":8.3167,"lat":47.45},"weather":[{"id":803,"main":"Clouds","description":"broken clouds","icon":"04n"}],"base":"stations","main":{"temp":6.94,"feels_like":5.14,"temp_min":5.83,"temp_max":8.28,"pressure":1013,"humidity":91},"visibility":10000,"wind":{"speed":2.57,"deg":230},"clouds":{"all":75},"dt":1667598796,"sys":{"type":2,"id":19109,"country":"CH","sunrise":1667542472,"sunset":1667577961},"timezone":3600,"id":2659490,"name":"Neuenhof","cod":200}
     */
    @Test
    public void testWeatherServiceWeatherDataAsString() {
        String weatherData = weatherService.getWeatherDataAsString();
        assertThat(weatherData, startsWith("{\"coord\":{"));
    }

    @Test
    public void testWeatherServiceWeatherDataAsObject() {
        WeatherData weatherData = weatherService.getWeatherDataAsObject();
        assertThat(weatherData.getName(), equalTo(LOCATION));
    }

    @Test
    void readWeatherPropertiesFile() {
        WeatherProperties wp = weatherService.readWeatherPropertiesFile();
        Assertions.assertEquals("metric", wp.getWeatherunit());
    }
}