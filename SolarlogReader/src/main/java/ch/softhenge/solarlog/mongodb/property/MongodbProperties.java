
package ch.softhenge.solarlog.mongodb.property;

import java.util.List;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class MongodbProperties {

    @SerializedName("mongodbcluster")
    private String mMongodbcluster;
    @SerializedName("mongodbcollection1min")
    private String mMongodbcollection1min;
    @SerializedName("mongodbcollection5min")
    private String mMongodbcollection5min;
    @SerializedName("mongodbdatabases")
    private List<Mongodbdatabasis> mMongodbdatabases;
    @SerializedName("mongodbpassword")
    private String mMongodbpassword;
    @SerializedName("mongodburl")
    private String mMongodburl;
    @SerializedName("mongodbuser")
    private String mMongodbuser;

    public String getMongodbcluster() {
        return mMongodbcluster;
    }

    public void setMongodbcluster(String mongodbcluster) {
        mMongodbcluster = mongodbcluster;
    }

    public String getMongodbcollection1min() {
        return mMongodbcollection1min;
    }

    public void setMongodbcollection1min(String mongodbcollection1min) {
        mMongodbcollection1min = mongodbcollection1min;
    }

    public String getMongodbcollection5min() {
        return mMongodbcollection5min;
    }

    public void setMongodbcollection5min(String mongodbcollection5min) {
        mMongodbcollection5min = mongodbcollection5min;
    }

    public List<Mongodbdatabasis> getMongodbdatabases() {
        return mMongodbdatabases;
    }

    public void setMongodbdatabases(List<Mongodbdatabasis> mongodbdatabases) {
        mMongodbdatabases = mongodbdatabases;
    }

    public String getMongodbpassword() {
        return mMongodbpassword;
    }

    public void setMongodbpassword(String mongodbpassword) {
        mMongodbpassword = mongodbpassword;
    }

    public String getMongodburl() {
        return mMongodburl;
    }

    public void setMongodburl(String mongodburl) {
        mMongodburl = mongodburl;
    }

    public String getMongodbuser() {
        return mMongodbuser;
    }

    public void setMongodbuser(String mongodbuser) {
        mMongodbuser = mongodbuser;
    }

}
