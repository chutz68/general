
package ch.softhenge.solarlog.solarlog.property;

import java.util.List;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class SolarlogProperties {

    @SerializedName("solarlogproperties")
    private List<Solarlogproperty> mSolarlogproperties;

    public List<Solarlogproperty> getSolarlogproperties() {
        return mSolarlogproperties;
    }

    public void setSolarlogproperties(List<Solarlogproperty> solarlogproperties) {
        mSolarlogproperties = solarlogproperties;
    }

    /**
     * returns the solarlog property file by solarlogname out of the json properties file
     *
     * @param solarlogName
     * @return the solarlogProperty or null if none was found for the solarlogName
     */
    public Solarlogproperty getSolarlogPropertyBySolarlogName(String solarlogName) {
        return this.getSolarlogproperties().stream()
                .filter((myProperty) -> myProperty.getSolarlogname().equals(solarlogName))
                .findFirst()
                .orElse(null);
    }

}
