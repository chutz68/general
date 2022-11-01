package ch.softhenge.solarlog.weather;

/**
 * Class represents a Weather Location that can be callled using the weather API
 */
public class WeatherLocation {

    private final String apiKey;
    private final String unit;
    private final String location;
    private final String locationurl;

    public static final WeatherLocation DEFAULT_WEATHER_LOCATION
            = new WeatherLocation("c6409c4bc73119f3a3bba68f4eaebcb6", "metric", "Neuenhof", "neuenhof,ag,ch");

    /**
     * Constructor
     *
     * @param apiKey
     * @param unit
     * @param location
     * @param locationurl
     */
    public WeatherLocation(String apiKey, String unit, String location, String locationurl) {
        this.apiKey = apiKey;
        this.unit = unit;
        this.location = location;
        this.locationurl = locationurl;
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
