package ch.softhenge.solarlog.solarlog.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

/**
 * This test checks whether we can call the Solarlog device which is located in the
 */

@SpringBootTest
public class SolarlogServiceIntegrationTest {

    @Autowired
    private SolarlogService solarlogService;

    @Test
    public void testSolarlogConnection() {
        String jsonResult = solarlogService.getSolarlogDataFromAPIAsString("ruroslocal");
        assertThat(jsonResult, startsWith("{\"801\":{\"170\""));
    }
}
