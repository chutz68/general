package ch.softhenge.solarlog.solarlog.service;

import ch.softhenge.solarlog.solarlog.pojo.SolarlogData;

/**
 * The Interface of the Solarlog Service
 */
public interface ISolarlogService {
    /**
     * Returns the solarlog json result as String
     *
     * @param solarlogName the name of the solarlog
     * @return the json as string
     */
    String getSolarlogDataFromAPIAsString(String solarlogName);

    /**
     * Returns the solarlog json result as a SolarloglogData object
     *
     * @param solarlogName the name of the solarlog
     * @return the SolarloglogData
     */
    SolarlogData getSolarogDataFromAPI(String solarlogName);
}
