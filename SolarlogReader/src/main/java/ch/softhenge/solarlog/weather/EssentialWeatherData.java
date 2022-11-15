package ch.softhenge.solarlog.weather;

import java.io.Serializable;

/**
 * Essetial Weahter Data that is used
 * <a href="https://openweathermap.org/current">...</a>
 */
public class EssentialWeatherData implements Serializable {

    /** e.g. 8.3167 */
    private Double coordinatesLongitude;

    /** e.g. 47.45 */
    private Double coordinatesLatitude;

    /*** e.g. broken clouds */
    private String weatherMain;

    /*** e.g. Clouds */
    private String weatherDescription;

    /** e.g. 04d */
    /** see <a href="https://openweathermap.org/weather-conditions">...</a> */
    private String weatherIcon;

    /** e.g. 75 */
    /** Cloudiness in percentage */
    private Integer cloudiness;

    /** e.g. 14.42 */
    private Double tempReal;

    /** e.g. 14.16 */
    private Double tempFeel;

    /** e.g. 1021*/
    /** mbar*/
    private Integer pressure;

    /** e.g. 86*/
    /** percentage */
    private Integer humidity;

    /** e.g. 2.5*/
    /** Unit Default: meter/sec */
    private Double windSpeed;

    /** e.g. 0 */
    private Integer windDegree;

    /** e.g. 100000 */
    /** Visibility, meter. The maximum value of the visibility is 10km */
    private Integer visibility;

    /** e.g. 3600 */
    /** Shift in seconds from UTC */
    private Integer timezone;

    /** e.g. 3.16 */
    /** Rain volume for the last 1 hour, mm */
    private Double rainLastHour;

    /** e.g. 0 */
    /** Snow volume for the last 1 hour, mm */
    private Double snowLastHour;

    public EssentialWeatherData(Double coordinatesLongitude, Double coordinatesLatitude, String weatherMain, String weatherDescription, String weatherIcon,
                                Integer cloudiness, Double tempReal, Double tempFeel, Integer pressure, Integer humidity, Double windSpeed, Integer windDegree,
                                Integer visibility, Integer timezone, Double rainLastHour, Double snowLastHour) {

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

