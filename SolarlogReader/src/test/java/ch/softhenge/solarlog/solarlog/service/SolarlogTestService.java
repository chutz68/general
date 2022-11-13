package ch.softhenge.solarlog.solarlog.service;

import ch.softhenge.solarlog.solarlog.pojo.Solarlog300Data;
import org.apache.commons.io.IOUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * A Test Class of the Solarlog Service that reads a file from classpath instead of calling the webservice
 */
public class SolarlogTestService implements ISolarlogService{

    private final SolarlogService solarlogService;
    private final String jsonFile;

    public SolarlogTestService() throws IOException {
        jsonFile = IOUtils.resourceToString("/solarlogdataNeuenhof.json", StandardCharsets.UTF_8);
        solarlogService = new SolarlogService(new RestTemplate());
    }

    @Override
    public String getSolarlog300DataFromAPIAsString(String solarlogName) {
        if (solarlogService.getSolarlogproperty(solarlogName) == null) {
            throw new RuntimeException("The Solarlog Property File doesn't contain the solarlogger with the name '" + solarlogName + "'");
        }
        return jsonFile;
    }

    @Override
    public Solarlog300Data getSolarog300DataFromAPI(String solarlogName) {
        if (solarlogService.getSolarlogproperty(solarlogName) == null) {
            throw new RuntimeException("The Solarlog Property File doesn't contain the solarlogger with the name '" + solarlogName + "'");
        }
        return new Solarlog300Data(jsonFile);
    }
}
