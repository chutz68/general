package ch.softhenge.solarlog.solarlog.pojo;

import java.io.Serializable;
import java.lang.Double;
import java.lang.Integer;
import java.lang.String;
import java.util.List;

public class SolarData5MinV3 implements Serializable {
  private Integer e_ac_usage_day;

  private String update_timestamp;

  private Heatpump heatpump;

  private Integer record_state;

  private Integer p_ac_usage;

  private List<? extends Inverters> inverters;

  private Weather weather;

  private Integer record_version;

  private Integer p_ac_inverter;

  private Integer e_ac_inverter_day;

  private String record_timestamp;

  private List<? extends Phases> phases;

  public Integer getE_ac_usage_day() {
    return this.e_ac_usage_day;
  }

  public void setE_ac_usage_day(Integer e_ac_usage_day) {
    this.e_ac_usage_day = e_ac_usage_day;
  }

  public String getUpdate_timestamp() {
    return this.update_timestamp;
  }

  public void setUpdate_timestamp(String update_timestamp) {
    this.update_timestamp = update_timestamp;
  }

  public Heatpump getHeatpump() {
    return this.heatpump;
  }

  public void setHeatpump(Heatpump heatpump) {
    this.heatpump = heatpump;
  }

  public Integer getRecord_state() {
    return this.record_state;
  }

  public void setRecord_state(Integer record_state) {
    this.record_state = record_state;
  }

  public Integer getP_ac_usage() {
    return this.p_ac_usage;
  }

  public void setP_ac_usage(Integer p_ac_usage) {
    this.p_ac_usage = p_ac_usage;
  }

  public List<? extends Inverters> getInverters() {
    return this.inverters;
  }

  public void setInverters(List<? extends Inverters> inverters) {
    this.inverters = inverters;
  }

  public Weather getWeather() {
    return this.weather;
  }

  public void setWeather(Weather weather) {
    this.weather = weather;
  }

  public Integer getRecord_version() {
    return this.record_version;
  }

  public void setRecord_version(Integer record_version) {
    this.record_version = record_version;
  }

  public Integer getP_ac_inverter() {
    return this.p_ac_inverter;
  }

  public void setP_ac_inverter(Integer p_ac_inverter) {
    this.p_ac_inverter = p_ac_inverter;
  }

  public Integer getE_ac_inverter_day() {
    return this.e_ac_inverter_day;
  }

  public void setE_ac_inverter_day(Integer e_ac_inverter_day) {
    this.e_ac_inverter_day = e_ac_inverter_day;
  }

  public String getRecord_timestamp() {
    return this.record_timestamp;
  }

  public void setRecord_timestamp(String record_timestamp) {
    this.record_timestamp = record_timestamp;
  }

  public List<? extends Phases> getPhases() {
    return this.phases;
  }

  public void setPhases(List<? extends Phases> phases) {
    this.phases = phases;
  }

  public static class Heatpump implements Serializable {
    private Integer state;

    public Integer getState() {
      return this.state;
    }

    public void setState(Integer state) {
      this.state = state;
    }
  }

  public static class Inverters implements Serializable {
    private Integer p_ac;

    private Integer e_ac_day;

    private Integer inverterNr;

    private Integer u_ac;

    private Integer state;

    private Integer error;

    public Integer getP_ac() {
      return this.p_ac;
    }

    public void setP_ac(Integer p_ac) {
      this.p_ac = p_ac;
    }

    public Integer getE_ac_day() {
      return this.e_ac_day;
    }

    public void setE_ac_day(Integer e_ac_day) {
      this.e_ac_day = e_ac_day;
    }

    public Integer getInverterNr() {
      return this.inverterNr;
    }

    public void setInverterNr(Integer inverterNr) {
      this.inverterNr = inverterNr;
    }

    public Integer getU_ac() {
      return this.u_ac;
    }

    public void setU_ac(Integer u_ac) {
      this.u_ac = u_ac;
    }

    public Integer getState() {
      return this.state;
    }

    public void setState(Integer state) {
      this.state = state;
    }

    public Integer getError() {
      return this.error;
    }

    public void setError(Integer error) {
      this.error = error;
    }
  }

  public static class Weather implements Serializable {
    private Integer wind_degree;

    private String weather_description;

    private Integer visibility;

    private Double rain_last_hour;

    private Double coordinate_latitude;

    private Double temp_real;

    private Integer timezone;

    private Integer air_humidity;

    private Integer cloudiness;

    private String weather_icon;

    private String weather_main;

    private Integer sunrise_datetime;

    private Integer snow_last_hour;

    private Double temp_feel;

    private Double wind_speed;

    private Double coordinate_longitude;

    private Integer sunset_datetime;

    private Integer air_pressure;

    public Integer getWind_degree() {
      return this.wind_degree;
    }

    public void setWind_degree(Integer wind_degree) {
      this.wind_degree = wind_degree;
    }

    public String getWeather_description() {
      return this.weather_description;
    }

    public void setWeather_description(String weather_description) {
      this.weather_description = weather_description;
    }

    public Integer getVisibility() {
      return this.visibility;
    }

    public void setVisibility(Integer visibility) {
      this.visibility = visibility;
    }

    public Double getRain_last_hour() {
      return this.rain_last_hour;
    }

    public void setRain_last_hour(Double rain_last_hour) {
      this.rain_last_hour = rain_last_hour;
    }

    public Double getCoordinate_latitude() {
      return this.coordinate_latitude;
    }

    public void setCoordinate_latitude(Double coordinate_latitude) {
      this.coordinate_latitude = coordinate_latitude;
    }

    public Double getTemp_real() {
      return this.temp_real;
    }

    public void setTemp_real(Double temp_real) {
      this.temp_real = temp_real;
    }

    public Integer getTimezone() {
      return this.timezone;
    }

    public void setTimezone(Integer timezone) {
      this.timezone = timezone;
    }

    public Integer getAir_humidity() {
      return this.air_humidity;
    }

    public void setAir_humidity(Integer air_humidity) {
      this.air_humidity = air_humidity;
    }

    public Integer getCloudiness() {
      return this.cloudiness;
    }

    public void setCloudiness(Integer cloudiness) {
      this.cloudiness = cloudiness;
    }

    public String getWeather_icon() {
      return this.weather_icon;
    }

    public void setWeather_icon(String weather_icon) {
      this.weather_icon = weather_icon;
    }

    public String getWeather_main() {
      return this.weather_main;
    }

    public void setWeather_main(String weather_main) {
      this.weather_main = weather_main;
    }

    public Integer getSunrise_datetime() {
      return this.sunrise_datetime;
    }

    public void setSunrise_datetime(Integer sunrise_datetime) {
      this.sunrise_datetime = sunrise_datetime;
    }

    public Integer getSnow_last_hour() {
      return this.snow_last_hour;
    }

    public void setSnow_last_hour(Integer snow_last_hour) {
      this.snow_last_hour = snow_last_hour;
    }

    public Double getTemp_feel() {
      return this.temp_feel;
    }

    public void setTemp_feel(Double temp_feel) {
      this.temp_feel = temp_feel;
    }

    public Double getWind_speed() {
      return this.wind_speed;
    }

    public void setWind_speed(Double wind_speed) {
      this.wind_speed = wind_speed;
    }

    public Double getCoordinate_longitude() {
      return this.coordinate_longitude;
    }

    public void setCoordinate_longitude(Double coordinate_longitude) {
      this.coordinate_longitude = coordinate_longitude;
    }

    public Integer getSunset_datetime() {
      return this.sunset_datetime;
    }

    public void setSunset_datetime(Integer sunset_datetime) {
      this.sunset_datetime = sunset_datetime;
    }

    public Integer getAir_pressure() {
      return this.air_pressure;
    }

    public void setAir_pressure(Integer air_pressure) {
      this.air_pressure = air_pressure;
    }
  }

  public static class Phases implements Serializable {
    private Integer p_ac;

    private Integer u_ac;

    private Integer phase_nr;

    public Integer getP_ac() {
      return this.p_ac;
    }

    public void setP_ac(Integer p_ac) {
      this.p_ac = p_ac;
    }

    public Integer getU_ac() {
      return this.u_ac;
    }

    public void setU_ac(Integer u_ac) {
      this.u_ac = u_ac;
    }

    public Integer getPhase_nr() {
      return this.phase_nr;
    }

    public void setPhase_nr(Integer phase_nr) {
      this.phase_nr = phase_nr;
    }
  }
}
