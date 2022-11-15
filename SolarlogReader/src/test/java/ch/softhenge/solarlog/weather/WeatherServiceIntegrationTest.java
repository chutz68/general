package ch.softhenge.solarlog.weather;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * This test class tests API calls to locations that do exist
 */
@SpringBootTest
class WeatherServiceIntegrationTest {

    private static final String LOCATION = "Neuenhof";

    @Autowired
    private WeatherService weatherService;


    /**
     * Expect something like:
     * {"coord":{"lon":8.3167,"lat":47.45},"weather":[{"id":803,"main":"Clouds","description":"broken clouds","icon":"04n"}],"base":"stations","main":{"temp":6.94,"feels_like":5.14,"temp_min":5.83,"temp_max":8.28,"pressure":1013,"humidity":91},"visibility":10000,"wind":{"speed":2.57,"deg":230},"clouds":{"all":75},"dt":1667598796,"sys":{"type":2,"id":19109,"country":"CH","sunrise":1667542472,"sunset":1667577961},"timezone":3600,"id":2659490,"name":"Neuenhof","cod":200}
     */
    @Test
    public void testWeatherServiceWeatherDataAsString() {
        String weatherData = weatherService.getWeatherDataAsString(LOCATION);
        assertThat(weatherData, startsWith("{\"coord\":{"));
    }

    @Test
    public void testWeatherServiceWeatherDataAsObject() {
        WeatherData weatherData = weatherService.getWeatherDataAsObject(LOCATION);
        assertThat(weatherData.getName(), equalTo(LOCATION));
        assertThat(weatherData.getMain().getTemp(), notNullValue());
        assertThat(weatherData.getMain().getFeels_like(), notNullValue());
        assertThat(weatherData.getMain().getPressure(), is(greaterThan(800)));
        assertThat(weatherData.getSys().getSunrise(), is(greaterThan(1000000)));
        assertThat(weatherData.getSys().getSunset(), is(greaterThan(weatherData.getSys().getSunrise())));
        assertThat(weatherData.getWeather().get(0).getIcon(), notNullValue());
        assertThat(weatherData.getWeather().get(0).getId(), notNullValue());
        assertThat(weatherData.getWeather().get(0).getMain(), notNullValue());
        assertThat(weatherData.getWeather().get(0).getDescription(), notNullValue());
        EssentialWeatherData essentialWeatherData = weatherData.createEssentialWeatherData();
        //FIXME: All fields are null!
        System.out.println(essentialWeatherData);
    }

    @Test
    void readWeatherPropertiesFile() {
        WeatherProperties wp = weatherService.readWeatherPropertiesFile();
        Assertions.assertEquals("metric", wp.getWeatherunit());
    }
}