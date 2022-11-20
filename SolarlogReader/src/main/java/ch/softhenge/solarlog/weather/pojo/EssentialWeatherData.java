package ch.softhenge.solarlog.weather.pojo;

/**
 * Essetial Weahter Data that is used
 * <a href="https://openweathermap.org/current">...</a>
 */
public class EssentialWeatherData {

    /* e.g. 8.3167 */
    private final Double coordinatesLongitude;

    /* e.g. 47.45 */
    private final Double coordinatesLatitude;

    /* e.g. broken clouds */
    private final String weatherMain;

    /* e.g. Clouds */
    private final String weatherDescription;

    /* e.g. 04d */
    /* see <a href="https://openweathermap.org/weather-conditions">...</a> */
    private final String weatherIcon;

    /* e.g. 75 */
    /* Cloudiness in percentage */
    private final Integer cloudiness;

    /* e.g. 14.42 */
    private final Double tempReal;

    /* e.g. 14.16 */
    private final Double tempFeel;

    /* e.g. 1021*/
    /* mbar*/
    private final Integer pressure;

    /* e.g. 86*/
    /* percentage */
    private final Integer humidity;

    /* e.g. 2.5*/
    /* Unit Default: meter/sec */
    private final Double windSpeed;

    /* e.g. 0 */
    private final Integer windDegree;

    /* e.g. 100000 */
    /* Visibility, meter. The maximum value of the visibility is 10km */
    private final Integer visibility;

    /* e.g. 3600 */
    /* Shift in seconds from UTC */
    private final Integer timezone;

    /* e.g. 3.16 */
    /* Rain volume for the last 1 hour, mm */
    private final Double rainLastHour;

    /* e.g. 0 */
    /* Snow volume for the last 1 hour, mm */
    private final Double snowLastHour;


    public EssentialWeatherData(Double coordinatesLongitude, Double coordinatesLatitude, String weatherMain, String weatherDescription, String weatherIcon,
                                Integer cloudiness, Double tempReal, Double tempFeel, Integer pressure, Integer humidity, Double windSpeed, Integer windDegree,
                                Integer visibility, Integer timezone, Double rainLastHour, Double snowLastHour) {
        this.coordinatesLongitude = coordinatesLongitude;
        this.coordinatesLatitude = coordinatesLatitude;
        this.weatherMain = weatherMain;
        this.weatherDescription = weatherDescription;
        this.weatherIcon = weatherIcon;
        this.cloudiness = cloudiness;
        this.tempReal = tempReal;
        this.tempFeel = tempFeel;
        this.pressure = pressure;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.windDegree = windDegree;
        this.visibility = visibility;
        this.timezone = timezone;
        this.rainLastHour = rainLastHour;
        this.snowLastHour = snowLastHour;
    }

    public Double getCoordinatesLongitude() {
        return coordinatesLongitude;
    }

    public Double getCoordinatesLatitude() {
        return coordinatesLatitude;
    }

    public String getWeatherMain() {
        return weatherMain;
    }

    public String getWeatherDescription() {
        return weatherDescription;
    }

    public String getWeatherIcon() {
        return weatherIcon;
    }

    public Integer getCloudiness() {
        return cloudiness;
    }

    public Double getTempReal() {
        return tempReal;
    }

    public Double getTempFeel() {
        return tempFeel;
    }

    public Integer getPressure() {
        return pressure;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public Double getWindSpeed() {
        return windSpeed;
    }

    public Integer getWindDegree() {
        return windDegree;
    }

    public Integer getVisibility() {
        return visibility;
    }

    public Integer getTimezone() {
        return timezone;
    }

    public Double getRainLastHour() {
        return rainLastHour;
    }

    public Double getSnowLastHour() {
        return snowLastHour;
    }

    @Override
    public String toString() {
        return "EssentialWeatherData{" +
                "coordinatesLongitude=" + coordinatesLongitude +
                ", coordinatesLatitude=" + coordinatesLatitude +
                ", weatherMain='" + weatherMain + '\'' +
                ", weatherDescription='" + weatherDescription + '\'' +
                ", weatherIcon='" + weatherIcon + '\'' +
                ", cloudiness=" + cloudiness +
                ", tempReal=" + tempReal +
                ", tempFeel=" + tempFeel +
                ", pressure=" + pressure +
                ", humidity=" + humidity +
                ", windSpeed=" + windSpeed +
                ", windDegree=" + windDegree +
                ", visibility=" + visibility +
                ", timezone=" + timezone +
                ", rainLastHour=" + rainLastHour +
                ", snowLastHour=" + snowLastHour +
                '}';
    }
}

