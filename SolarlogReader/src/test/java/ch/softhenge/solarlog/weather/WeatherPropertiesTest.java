package ch.softhenge.solarlog.weather;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class WeatherPropertiesTest {

    private final WeatherProperties weatherPropertiesOk = new WeatherProperties();

    @BeforeEach
    void fillWeatherProperties() {
        weatherPropertiesOk.setWeatherapikey("4711");
        weatherPropertiesOk.setWeatherunit("metric");
        WeatherProperties.Weatherlocations weatherLocation = new WeatherProperties.Weatherlocations();
        weatherLocation.setLocationapiurl("myhome-url");
        weatherLocation.setLocationname("myhome");
        List<WeatherProperties.Weatherlocations> weatherLocations = Lists.newArrayList(weatherLocation);
        weatherPropertiesOk.setWeatherlocations(weatherLocations);
    }

    /**
     * Tests the behaviour when the Url was found based on the location
     */
    @Test
    void getWetherLocationApiUrlByLocationnameOk() {
        String location = weatherPropertiesOk.getWetherLocationApiUrlByLocationname("myhome");
        Assertions.assertEquals("myhome-url", location);
    }

    @Test
    void getWetherLocationApiUrlByLocationnameNotExists() {
        String location = weatherPropertiesOk.getWetherLocationApiUrlByLocationname("notexists");
        Assertions.assertNull(location);
    }

}