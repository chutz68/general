package ch.softhenge.solarlog.weather;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration
class WaetherServiceTest {

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @Autowired
    WaetherService weatherService;

    @Test
    public void testWeatherService() {
        WaetherService weatherService = new WaetherService();
        String weatherData = weatherService.getWeatherData();
        System.out.println(weatherData);
    }

}