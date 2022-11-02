package ch.softhenge.solarlog.weather;

import java.io.Serializable;
import java.util.List;

/**
 * The Java Object that represents a weather api properties json file
 */
public class WeatherProperties implements Serializable {
    private String weatherapikey;

    private String weatherunit;

    private List<? extends Weatherlocations> weatherlocations;

    public String getWeatherapikey() {
        return this.weatherapikey;
    }

    public void setWeatherapikey(String weatherapikey) {
        this.weatherapikey = weatherapikey;
    }

    public String getWeatherunit() {
        return this.weatherunit;
    }

    public void setWeatherunit(String weatherunit) {
        this.weatherunit = weatherunit;
    }

    public List<? extends Weatherlocations> getWeatherlocations() {
        return this.weatherlocations;
    }

    public void setWeatherlocations(List<? extends Weatherlocations> weatherlocations) {
        this.weatherlocations = weatherlocations;
    }

    /**
     *
     * @param myLocation the location for which I want to get the URL
     * @return the location api url based on the location
     */
    public String getWetherLocationApiUrlByLocationname(String myLocation) {
        Weatherlocations searchLocation = this.weatherlocations.stream()
                .filter((weatherLocation) -> weatherLocation.getLocationname().equals(myLocation))
                .findFirst()
                .orElse(null);
        return searchLocation == null ? null: searchLocation.getLocationapiurl();
    }

    public static class Weatherlocations implements Serializable {
        private String locationname;

        private String locationapiurl;

        public String getLocationname() {
            return this.locationname;
        }

        public void setLocationname(String locationname) {
            this.locationname = locationname;
        }

        public String getLocationapiurl() {
            return this.locationapiurl;
        }

        public void setLocationapiurl(String locationapiurl) {
            this.locationapiurl = locationapiurl;
        }
    }

    @Override
    public String toString() {
        return "WeatherProperties{" +
                "weatherapikey='" + weatherapikey + '\'' +
                ", weatherunit='" + weatherunit + '\'' +
                ", weatherlocations=" + weatherlocations +
                '}';
    }
}

