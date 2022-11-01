package ch.softhenge.solarlog.weather;

import java.io.Serializable;
import java.util.List;

/**
 *
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

