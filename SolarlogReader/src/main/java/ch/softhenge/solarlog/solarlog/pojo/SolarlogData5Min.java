
package ch.softhenge.solarlog.solarlog.pojo;

import java.time.*;
import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class SolarlogData5Min {

    @SerializedName("record_timestamp")
    private RecordTimestamp mRecordTimestamp;
    @SerializedName("update_timestamp")
    private UpdateTimestamp mUpdateTimestamp;
    @SerializedName("e_ac_inverter_day")
    private Integer mEAcInverterDay;
    @SerializedName("e_ac_usage_day")
    private Integer mEAcUsageDay;
    @SerializedName("heatpump")
    private Heatpump mHeatpump;
    @SerializedName("inverters")
    private List<Inverter> mInverters;
    @SerializedName("p_ac_inverter")
    private Integer mPAcInverter;
    @SerializedName("p_ac_usage")
    private Integer mPAcUsage;
    @SerializedName("phases")
    private List<Phase> mPhases;
    @SerializedName("record_state")
    private Integer mRecordState;
    @SerializedName("record_version")
    private Integer mRecordVersion;
    @SerializedName("weather")
    private Weather mWeather;

    public RecordTimestamp getRecordTimestamp() {
        return mRecordTimestamp;
    }

    public LocalDateTime getRecordTimestampAsLocalDateTime(ZoneId zone) {
        Instant instant = Instant.parse(mRecordTimestamp.m$date);
        return LocalDateTime.ofInstant(instant, zone);
    }

    public void setRecordTimestamp(RecordTimestamp recordTimestamp) {
        mRecordTimestamp = recordTimestamp;
    }

    public UpdateTimestamp getUpdateTimestamp() {
        return mUpdateTimestamp;
    }

    public LocalDateTime getUpdateTimestampAsLocalDateTime(ZoneId zone) {
        Instant instant = Instant.parse(mUpdateTimestamp.m$date);
        return LocalDateTime.ofInstant(instant, zone);
    }

    public void setUpdateTimestamp(UpdateTimestamp updateTimestamp) {
        mUpdateTimestamp = updateTimestamp;
    }

    public Integer getEAcInverterDay() {
        return mEAcInverterDay;
    }

    public void setEAcInverterDay(Integer eAcInverterDay) {
        mEAcInverterDay = eAcInverterDay;
    }

    public Integer getEAcUsageDay() {
        return mEAcUsageDay;
    }

    public void setEAcUsageDay(Integer eAcUsageDay) {
        mEAcUsageDay = eAcUsageDay;
    }

    public Heatpump getHeatpump() {
        return mHeatpump;
    }

    public void setHeatpump(Heatpump heatpump) {
        mHeatpump = heatpump;
    }

    public List<Inverter> getInverters() {
        return mInverters;
    }

    public void setInverters(List<Inverter> inverters) {
        mInverters = inverters;
    }

    public Integer getPAcInverter() {
        return mPAcInverter;
    }

    public void setPAcInverter(Integer pAcInverter) {
        mPAcInverter = pAcInverter;
    }

    public Integer getPAcUsage() {
        return mPAcUsage;
    }

    public void setPAcUsage(Integer pAcUsage) {
        mPAcUsage = pAcUsage;
    }

    public List<Phase> getPhases() {
        return mPhases;
    }

    public void setPhases(List<Phase> phases) {
        mPhases = phases;
    }

    public Integer getRecordState() {
        return mRecordState;
    }

    public void setRecordState(Integer recordState) {
        mRecordState = recordState;
    }

    public Integer getRecordVersion() {
        return mRecordVersion;
    }

    public void setRecordVersion(Integer recordVersion) {
        mRecordVersion = recordVersion;
    }

    public Weather getWeather() {
        return mWeather;
    }

    public void setWeather(Weather weather) {
        mWeather = weather;
    }


    public static class RecordTimestamp {

        @SerializedName("$date")
        private String m$date;

        public String get$date() {
            return m$date;
        }

        public void set$date(String $date) {
            m$date = $date;
        }
    }

    public static class UpdateTimestamp {

        @SerializedName("$date")
        private String m$date;

        public String get$date() {
            return m$date;
        }

        public void set$date(String $date) {
            m$date = $date;
        }
    }

    public static class Heatpump {

        @SerializedName("state")
        private Integer mState;

        public Integer getState() {
            return mState;
        }

        public void setState(Integer state) {
            mState = state;
        }

    }

    public static class Inverter {

        @SerializedName("e_ac_day")
        private Integer mEAcDay;
        @SerializedName("error")
        private Integer mError;
        @SerializedName("inverterNr")
        private Integer mInverterNr;
        @SerializedName("p_ac")
        private Integer mPAc;
        @SerializedName("state")
        private Integer mState;
        @SerializedName("u_ac")
        private Integer mUAc;

        public Integer getEAcDay() {
            return mEAcDay;
        }

        public void setEAcDay(Integer eAcDay) {
            mEAcDay = eAcDay;
        }

        public Integer getError() {
            return mError;
        }

        public void setError(Integer error) {
            mError = error;
        }

        public Integer getInverterNr() {
            return mInverterNr;
        }

        public void setInverterNr(Integer inverterNr) {
            mInverterNr = inverterNr;
        }

        public Integer getPAc() {
            return mPAc;
        }

        public void setPAc(Integer pAc) {
            mPAc = pAc;
        }

        public Integer getState() {
            return mState;
        }

        public void setState(Integer state) {
            mState = state;
        }

        public Integer getUAc() {
            return mUAc;
        }

        public void setUAc(Integer uAc) {
            mUAc = uAc;
        }

    }

    public static class Phase {

        @SerializedName("p_ac")
        private Integer mPAc;
        @SerializedName("phase_nr")
        private Integer mPhaseNr;
        @SerializedName("u_ac")
        private Integer mUAc;

        public Integer getPAc() {
            return mPAc;
        }

        public void setPAc(Integer pAc) {
            mPAc = pAc;
        }

        public Integer getPhaseNr() {
            return mPhaseNr;
        }

        public void setPhaseNr(Integer phaseNr) {
            mPhaseNr = phaseNr;
        }

        public Integer getUAc() {
            return mUAc;
        }

        public void setUAc(Integer uAc) {
            mUAc = uAc;
        }

    }

    public static class Weather {

        @SerializedName("air_humidity")
        private Integer mAirHumidity;
        @SerializedName("air_pressure")
        private Integer mAirPressure;
        @SerializedName("cloudiness")
        private Integer mCloudiness;
        @SerializedName("coordinate_latitude")
        private Double mCoordinateLatitude;
        @SerializedName("coordinate_longitude")
        private Double mCoordinateLongitude;
        @SerializedName("rain_last_hour")
        private Double mRainLastHour;
        @SerializedName("snow_last_hour")
        private Integer mSnowLastHour;
        @SerializedName("sunrise_datetime")
        private Integer mSunriseDatetime;
        @SerializedName("sunset_datetime")
        private Integer mSunsetDatetime;
        @SerializedName("temp_feel")
        private Double mTempFeel;
        @SerializedName("temp_real")
        private Double mTempReal;
        @SerializedName("timezone")
        private Integer mTimezone;
        @SerializedName("visibility")
        private Integer mVisibility;
        @SerializedName("weather_description")
        private String mWeatherDescription;
        @SerializedName("weather_icon")
        private String mWeatherIcon;
        @SerializedName("weather_main")
        private String mWeatherMain;
        @SerializedName("wind_degree")
        private Integer mWindDegree;
        @SerializedName("wind_speed")
        private Double mWindSpeed;

        public Integer getAirHumidity() {
            return mAirHumidity;
        }

        public void setAirHumidity(Integer airHumidity) {
            mAirHumidity = airHumidity;
        }

        public Integer getAirPressure() {
            return mAirPressure;
        }

        public void setAirPressure(Integer airPressure) {
            mAirPressure = airPressure;
        }

        public Integer getCloudiness() {
            return mCloudiness;
        }

        public void setCloudiness(Integer cloudiness) {
            mCloudiness = cloudiness;
        }

        public Double getCoordinateLatitude() {
            return mCoordinateLatitude;
        }

        public void setCoordinateLatitude(Double coordinateLatitude) {
            mCoordinateLatitude = coordinateLatitude;
        }

        public Double getCoordinateLongitude() {
            return mCoordinateLongitude;
        }

        public void setCoordinateLongitude(Double coordinateLongitude) {
            mCoordinateLongitude = coordinateLongitude;
        }

        public Double getRainLastHour() {
            return mRainLastHour;
        }

        public void setRainLastHour(Double rainLastHour) {
            mRainLastHour = rainLastHour;
        }

        public Integer getSnowLastHour() {
            return mSnowLastHour;
        }

        public void setSnowLastHour(Integer snowLastHour) {
            mSnowLastHour = snowLastHour;
        }

        public Integer getSunriseDatetime() {
            return mSunriseDatetime;
        }

        public void setSunriseDatetime(Integer sunriseDatetime) {
            mSunriseDatetime = sunriseDatetime;
        }

        public Integer getSunsetDatetime() {
            return mSunsetDatetime;
        }

        public void setSunsetDatetime(Integer sunsetDatetime) {
            mSunsetDatetime = sunsetDatetime;
        }

        public Double getTempFeel() {
            return mTempFeel;
        }

        public void setTempFeel(Double tempFeel) {
            mTempFeel = tempFeel;
        }

        public Double getTempReal() {
            return mTempReal;
        }

        public void setTempReal(Double tempReal) {
            mTempReal = tempReal;
        }

        public Integer getTimezone() {
            return mTimezone;
        }

        public void setTimezone(Integer timezone) {
            mTimezone = timezone;
        }

        public Integer getVisibility() {
            return mVisibility;
        }

        public void setVisibility(Integer visibility) {
            mVisibility = visibility;
        }

        public String getWeatherDescription() {
            return mWeatherDescription;
        }

        public void setWeatherDescription(String weatherDescription) {
            mWeatherDescription = weatherDescription;
        }

        public String getWeatherIcon() {
            return mWeatherIcon;
        }

        public void setWeatherIcon(String weatherIcon) {
            mWeatherIcon = weatherIcon;
        }

        public String getWeatherMain() {
            return mWeatherMain;
        }

        public void setWeatherMain(String weatherMain) {
            mWeatherMain = weatherMain;
        }

        public Integer getWindDegree() {
            return mWindDegree;
        }

        public void setWindDegree(Integer windDegree) {
            mWindDegree = windDegree;
        }

        public Double getWindSpeed() {
            return mWindSpeed;
        }

        public void setWindSpeed(Double windSpeed) {
            mWindSpeed = windSpeed;
        }

    }

}
