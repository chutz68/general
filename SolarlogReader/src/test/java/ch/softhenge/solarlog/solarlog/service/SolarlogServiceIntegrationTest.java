package ch.softhenge.solarlog.solarlog.service;

import ch.softhenge.solarlog.solarlog.pojo.Solarlog300Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.Month;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This test checks whether we can call the Solarlog device which is located in the local network.
 */

@SpringBootTest
public class SolarlogServiceIntegrationTest {

    @Autowired
    private ISolarlogService solarlogService;

    @Test
    public void testGetSolarlog300DataFromAPIAsString() {
        String jsonResult = solarlogService.getSolarlog300DataFromAPIAsString("ruroslocal");
        assertThat(jsonResult, startsWith("{\"801\":{\"170\""));
    }

    @Test
    public void testGetSolarlog300DataFromAPI() {
        Solarlog300Data solarlog300Data = solarlogService.getSolarog300DataFromAPI("ruroslocal");
        LocalDateTime ldtexpected = LocalDateTime.of(2022, Month.NOVEMBER, 5, 12, 50);
        assertThat(solarlog300Data.getSolarlogDateField(Solarlog300Data.SOLARLOG300_REGISTER.CREATEDDATE), greaterThan(ldtexpected));
        assertThat(solarlog300Data.getSolarlogIntegerField(Solarlog300Data.SOLARLOG300_REGISTER.PACWRALL), is(greaterThanOrEqualTo(0)));
        assertThat(solarlog300Data.getSolarlogIntegerField(Solarlog300Data.SOLARLOG300_REGISTER.EAC_DAYSUM_CNT), is(greaterThanOrEqualTo(0)));
        assertThat(solarlog300Data.getSolarlogIntegerField(Solarlog300Data.SOLARLOG300_REGISTER.EAC_TOTAL_CNT), is(greaterThan(0)));
    }

    @Test
    public void testGetSolarlog300DataAsStringNotExists() {
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> solarlogService.getSolarlog300DataFromAPIAsString("notexists"));
        assertThat(runtimeException.getMessage(), startsWith("The Solarlog Property File"));
    }

    @Test
    public void testGetSolarlog300DataNotExists() {
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> solarlogService.getSolarog300DataFromAPI("notexists"));
        assertThat(runtimeException.getMessage(), startsWith("The Solarlog Property File"));
    }

}
