package ch.softhenge.solarlog.solarlog.service;

import ch.softhenge.solarlog.solarlog.pojo.SolarlogRegisterData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.*;

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
    public void testGetSolarlogDataFromAPIAsString() {
        String jsonResult = solarlogService.getSolarlogDataFromAPIAsString("ruros300");
        assertThat(jsonResult, startsWith("{\"801\":{\"170\""));
    }

    @Test
    public void testGetSolarlogDataFromAPI() {
        SolarlogRegisterData solarlogRegisterData = solarlogService.getSolarogDataFromAPI("ruros300");
        LocalDateTime ldtbefore5Minutes = LocalDateTime.now().minusMinutes(5);
        LocalDateTime solarlogDate = solarlogRegisterData.getSolarlogDateField(SolarlogRegisterData.SOLARLOG_REGISTER.CREATEDDATE);
        assertThat(solarlogDate, greaterThan(ldtbefore5Minutes));
        LocalDateTime ldtafter5Minutes = LocalDateTime.now().plusMinutes(5);
        assertThat(solarlogDate, lessThan(ldtafter5Minutes));
        System.out.println("Current Date: " + solarlogDate);
        assertThat(solarlogDate.toString(), matchesRegex("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}$"));

        ZonedDateTime zonedSolarlogDate = solarlogRegisterData.getSolarlogDateFieldUTC(solarlogDate);
        System.out.println("Current Date: " + zonedSolarlogDate);
        assertThat(zonedSolarlogDate.toString(), matchesRegex("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$"));

        assertThat(solarlogRegisterData.getSolarlogIntegerField(SolarlogRegisterData.SOLARLOG_REGISTER.PACWRALL), is(greaterThanOrEqualTo(0)));
        assertThat(solarlogRegisterData.getSolarlogIntegerField(SolarlogRegisterData.SOLARLOG_REGISTER.EAC_DAYSUM_CNT), is(greaterThanOrEqualTo(0)));
        assertThat(solarlogRegisterData.getSolarlogIntegerField(SolarlogRegisterData.SOLARLOG_REGISTER.EAC_TOTAL_CNT), is(greaterThan(0)));
    }

    @Test
    public void testGetSolarlogDataAsStringNotExists() {
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> solarlogService.getSolarlogDataFromAPIAsString("notexists"));
        assertThat(runtimeException.getMessage(), startsWith("The Solarlog Property File"));
    }

    @Test
    public void testGetSolarlogDataNotExists() {
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> solarlogService.getSolarogDataFromAPI("notexists"));
        assertThat(runtimeException.getMessage(), startsWith("The Solarlog Property File"));
    }

}
