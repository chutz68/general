package ch.softhenge.solarlog.solarlog.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

/**
 * This test checks whether we can call the Solarlog device which is located in the
 */

@SpringBootTest
public class SolarlogServiceIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Test
    public void testSolarlogConnection() {
        final String uri = "http://192.168.1.39/getjp";
        String input = "{\"801\":{\"170\":null}}";
        String retval = restTemplate.postForObject(uri, input, String.class);
        System.out.println(retval);
    }
}
