package ch.softhenge.solarlog.weather;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
class WaetherServiceTest {

    @Autowired
    private WaetherService weatherService;

    @Test
    public void testWeatherService() {
        String weatherData = weatherService.getWeatherDataAsString();
        System.out.println(weatherData);
    }

}