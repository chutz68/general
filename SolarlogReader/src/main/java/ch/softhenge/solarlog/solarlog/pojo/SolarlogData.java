package ch.softhenge.solarlog.solarlog.pojo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * This is an object that contains the solarlog json data in HashMaps
 */
public class SolarlogData {

    /**
     * This enum represents the json fields in the solarlog return value
     */
    public enum SOLARLOG_REGISTER {
        CREATEDDATE("100", LocalDateTime.class),
        PACWRALL( "101", Integer.class),
        PDCWRALL("102", Integer.class),
        UACWRALL( "103", Integer.class),
        UDCWRALL( "104", Integer.class),
        EAC_DAYSUM_WRALL("105", Integer.class),
        EAC_DAYSUM_YESTERDAY_WRALL( "106", Integer.class),
        EAC_MONTHSUM_WRALL("107", Integer.class),
        EAC_YEARSUM_WRALL( "108", Integer.class),
        EAC_TOTAL_WRALL( "109", Integer.class),
        PAC_CNT( "110", Integer.class),
        EAC_DAYSUM_CNT( "111", Integer.class),
        EAC_DAYSUM_YESTERDAY_CNT( "112", Integer.class),
        EAC_MONTHSUM_CNT( "113", Integer.class),
        EAC_YEARSUM_CNT("114", Integer.class),
        EAC_TOTAL_CNT( "115", Integer.class);

        private final String register;
        private final Class<?> fieldClass;

        SOLARLOG_REGISTER(String register, Class<?> fieldClass) {
            this.register = register;
            this.fieldClass = fieldClass;
        }

    }

    private final static String SOLARLOG_JSON_OBJID_1 = "801";
    private final static String SOLARLOG_JSON_OBJID_2 = "170";
    private final static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss");

    private final Map<SOLARLOG_REGISTER, Integer> solarlogIntegerMap;
    private final Map<SOLARLOG_REGISTER, LocalDateTime> solarlogDateMap;

    private final ZoneId zoneIdCreatedDate;


    /**
     * The constructor creates a class containing all values of the Solarlog Json, storing them to HashMaps
     *
     * @param solarlogLogJsonString the JSON as String
     * @param zoneIdCreatedDate the zoneId of the createdDate
     */
    public SolarlogData(String solarlogLogJsonString, ZoneId zoneIdCreatedDate) {
        solarlogIntegerMap = new HashMap<>();
        solarlogDateMap = new HashMap<>();
        this.zoneIdCreatedDate = zoneIdCreatedDate;

        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(solarlogLogJsonString);
        JsonObject jsonObj = jsonElement.getAsJsonObject();
        jsonObj = jsonObj.getAsJsonObject(SolarlogData.SOLARLOG_JSON_OBJID_1);
        jsonObj = jsonObj.getAsJsonObject(SolarlogData.SOLARLOG_JSON_OBJID_2);

        for (SOLARLOG_REGISTER slr : SOLARLOG_REGISTER.values()) {
            JsonPrimitive jsonPrimitive = jsonObj.get(slr.register).getAsJsonPrimitive();
            if (Integer.class.equals(slr.fieldClass)) {
                solarlogIntegerMap.put(slr, jsonPrimitive.getAsInt());
            }
            if (LocalDateTime.class.equals(slr.fieldClass)) {
                String dateAsString = jsonPrimitive.getAsString();
                solarlogDateMap.put(slr, LocalDateTime.parse(dateAsString, dtf));
            }
        }
    }


    /**
     * returns the integer value of the requested key
     *
     * @param register the key that was requested
     * @return the integer belonging to the key
     */
    public Integer getSolarlogIntegerField(SOLARLOG_REGISTER register) {
        return solarlogIntegerMap.get(register);
    }

    /**
     * returns the Date value of the requested key
     *
     * @param register the key that was requested
     * @return the date belonging to the key
     */
    public LocalDateTime getSolarlogDateField(SOLARLOG_REGISTER register) {
        return solarlogDateMap.get(register);
    }

    /**
     * returns the Date value of the requested key as a UTC Date
     *
     * @param localDateTime the localDateTime
     * @return the ZonedDateTime in UTC
     */
    public ZonedDateTime getSolarlogDateFieldUTC(LocalDateTime localDateTime) {
        return localDateTime.atZone(zoneIdCreatedDate).withZoneSameInstant(ZoneOffset.UTC);
    }

}
