package ch.softhenge.solarlog.solarlog.service;

import ch.softhenge.solarlog.solarlog.pojo.Solarlog300Data;

/**
 * The Interface of the Solarlog Service
 */
public interface ISolarlogService {
    /**
     * Returns the solarlog 300 json result as String
     *
     * @param solarlogName the name of the solarlog
     * @return the json as string
     */
    String getSolarlog300DataFromAPIAsString(String solarlogName);

    /**
     * Returns the solarlog 300 json result as a Solarloglog300Data object
     *
     * @param solarlogName the name of the solarlog
     * @return the Solarloglog300Data
     */
    Solarlog300Data getSolarog300DataFromAPI(String solarlogName);
}
