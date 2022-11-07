package ch.softhenge.solarlog.solarlog.service;

import ch.softhenge.solarlog.solarlog.pojo.Solarlog300Data;
import org.hibernate.validator.internal.IgnoreForbiddenApisErrors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

/**
 * This test checks whether we can call the Solarlog device which is located in the local network.
 */

@SpringBootTest
public class SolarlogServiceIntegrationTest {

    @Autowired
    private SolarlogService solarlogService;

    @Test
    public void testSolarlogConnection() {
        String jsonResult = solarlogService.getSolarlogDataFromAPIAsString("ruroslocal");
        assertThat(jsonResult, startsWith("{\"801\":{\"170\""));

        Solarlog300Data solarlog300Data = new Solarlog300Data(jsonResult);
        System.out.println("creaDate: " + solarlog300Data.getCreatedDate());
        System.out.println("pACWrAll: " + solarlog300Data.getPacWrAll());
    }


}
