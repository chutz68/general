
package ch.softhenge.solarlog.solarlog.property;

import com.google.gson.annotations.SerializedName;

public class Solarlogproperty {

    @SerializedName("solarlogbaseurl")
    private String mSolarlogbaseurl;
    @SerializedName("solarlogname")
    private String mSolarlogname;

    @SerializedName("zoneid")
    private String mZoneid;

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

    public String getmZoneid() {
        return mZoneid;
    }
    public void setmZoneid(String mZoneid) {
        this.mZoneid = mZoneid;
    }
}
