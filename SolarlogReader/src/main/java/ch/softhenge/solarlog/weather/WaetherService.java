package ch.softhenge.solarlog.weather;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WaetherService {

    private static final String WEATHER_URI = "http://api.openweathermap.org/data/2.5/weather?q=neuenhof,ag,ch&APPID=c6409c4bc73119f3a3bba68f4eaebcb6&units=metric";
    @Autowired
    private RestTemplate restTemplate;

    public String getWeatherDataAsString() {
        return restTemplate.getForObject(WEATHER_URI, String.class);
    }
}
