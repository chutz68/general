package ch.softhenge.solarlog.solarlog.pojo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This is an object that is created if the solarlog 300 is read
 */
public class Solarlog300Data implements Serializable {

    /**
     * This enum represents the json fields in the solarlog return value
     */
    public enum SOLARLOG300_REGISTER {
        CREATEDDATE("createdDate", "100", LocalDateTime.class),
        PACWRALL("pacWrAll", "101", Integer.class);

        private final String fieldName;
        private final String register;
        private final Class<?> fieldClass;

        SOLARLOG300_REGISTER(String fieldName, String register, Class<?> fieldClass) {
            this.fieldName = fieldName;
            this.register = register;
            this.fieldClass = fieldClass;
        }

    }

    private final static String SOLARLOG300_JSON_OBJID_1 = "801";
    private final static String SOLARLOG300_JSON_OBJID_2 = "170";

    private final static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss");

    private LocalDateTime createdDate;
    private Integer pacWrAll;


    public Solarlog300Data(String solarlogLogJsonString) {
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(solarlogLogJsonString);
        JsonObject jsonObj = jsonElement.getAsJsonObject();
        jsonObj = jsonObj.getAsJsonObject(Solarlog300Data.SOLARLOG300_JSON_OBJID_1);
        jsonObj = jsonObj.getAsJsonObject(Solarlog300Data.SOLARLOG300_JSON_OBJID_2);

        Field[] fields = Solarlog300Data.class.getDeclaredFields();
        for (SOLARLOG300_REGISTER slr : SOLARLOG300_REGISTER.values()) {
            JsonPrimitive primitive = jsonObj.get(slr.register).getAsJsonPrimitive();
            try {
                Field f = Solarlog300Data.class.getDeclaredField(slr.fieldName);
                if (Integer.class.equals(slr.fieldClass)) {
                    f.set(this, primitive.getAsInt());
                }
                if (LocalDateTime.class.equals(slr.fieldClass)) {
                    String dateAsString = primitive.getAsString();
                    f.set(this, LocalDateTime.parse(dateAsString, dtf));
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Integer getPacWrAll() {
        return pacWrAll;
    }
}
