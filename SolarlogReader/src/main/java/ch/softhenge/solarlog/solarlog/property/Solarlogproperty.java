
package ch.softhenge.solarlog.solarlog.property;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class Solarlogproperty {

    @SerializedName("solarlogbaseurl")
    private String mSolarlogbaseurl;
    @SerializedName("solarlogname")
    private String mSolarlogname;

    public String getSolarlogbaseurl() {
        return mSolarlogbaseurl;
    }

    public void setSolarlogbaseurl(String solarlogbaseurl) {
        mSolarlogbaseurl = solarlogbaseurl;
    }

    public String getSolarlogname() {
        return mSolarlogname;
    }

    public void setSolarlogname(String solarlogname) {
        mSolarlogname = solarlogname;
    }

}
