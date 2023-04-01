package ch.softhenge.solarlog.solarlog.pojo;

import java.util.ArrayList;
import java.util.Date;

public class SolarlogData5MinV1 {

    public Date record_timestamp;
    public Date update_timestamp;
    public int p_ac_usage;
    public int p_ac_inverter;
    public int e_ac_usage_day;
    public int e_ac_inverter_day;
    public int record_state;
    public int record_version;
    public ArrayList<Inverter> inverters;
    public ArrayList<Phase> phases;
    public Heatpump heatpump;
    public Weather weather;

    public class Heatpump{
        public int state;
    }

    public class Inverter{
        public int inverterNr;
        public int p_ac;
        public int u_ac;
        public int e_ac_day;
        public int state;
        public int error;
    }

    public class Phase{
        public int phase_nr;
        public int p_ac;
        public int u_ac;
    }


    public class Weather{
        public String weather_main;
        public String weather_description;
        public String weather_icon;
        public double temp_real;
        public double temp_feel;
        public int air_pressure;
        public int air_humidity;
        public int visibility;
        public double wind_speed;
        public int wind_degree;
        public int cloudiness;
        public double rain_last_hour;
        public int snow_last_hour;
        public int sunrise_datetime;
        public int sunset_datetime;
        public int timezone;
        public double coordinate_longitude;
        public double coordinate_latitude;
    }



}
