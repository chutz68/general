
package ch.softhenge.solarlog.mongodb.property;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class Mongodbdatabasis {

    @SerializedName("databasename")
    private String mDatabasename;
    @SerializedName("name")
    private String mName;

    public String getDatabasename() {
        return mDatabasename;
    }

    public void setDatabasename(String databasename) {
        mDatabasename = databasename;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

}
