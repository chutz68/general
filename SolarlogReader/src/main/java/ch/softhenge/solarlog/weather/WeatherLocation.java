package ch.softhenge.solarlog.weather;

/**
 * Class represents a Weather Location that can be called using the weather API
 */
public class WeatherLocation {

    private final String apiKey;
    private final String unit;
    private final String location;
    private final String locationurl;

    public static final WeatherLocation DEFAULT_WEATHER_LOCATION
            = new WeatherLocation("c6409c4bc73119f3a3bba68f4eaebcb6", "metric", "Neuenhof", "neuenhof,ag,ch");

    /**
     * Constructor to build an object out of the attributes
     *
     * @param apiKey the apikey that we use from the weather api
     * @param unit the unit used, e.g. metric
     * @param location the location
     * @param locationurl the url of the location that is used to call the weather api
     */
    public WeatherLocation(String apiKey, String unit, String location, String locationurl) {
        this.apiKey = apiKey;
        this.unit = unit;
        this.location = location;
        this.locationurl = locationurl;
    }

    /**
     * Constructor to build an object out of a WeatherProperties Object
     *
     * @param weatherProperties the properties json file
     * @param location the location
     */
    public WeatherLocation(WeatherProperties weatherProperties, String location) {
        this.apiKey = weatherProperties.getWeatherapikey();
        this.unit = weatherProperties.getWeatherunit();
        this.location = location;
        this.locationurl = weatherProperties.getWetherLocationApiUrlByLocationname(location);
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getUnit() {
        return unit;
    }

    public String getLocation() {
        return location;
    }

    public String getLocationurl() {
        return locationurl;
    }
}
