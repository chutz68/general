package ch.softhenge.solarlog.solarlog.service;

import ch.softhenge.solarlog.solarlog.pojo.SolarlogRegisterData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This test checks whether we can call the Solarlog device which is located in the local network.
 */
@SpringBootTest
public class SolarlogServiceTest {

    private static ISolarlogService solarlogService;

    @BeforeAll
    public static void beforeAll() throws IOException {
        solarlogService = new SolarlogTestService();
    }

    @Test
    public void testGetSolarlogDataFromAPIAsString() {
        String jsonResult = solarlogService.getSolarlogDataFromAPIAsString("ruros300");
        assertThat(jsonResult, startsWith("{"));
        assertThat(jsonResult, containsString("\"801\":"));
        assertThat(jsonResult, containsString("\"170\":"));
    }

    @Test
    public void testGet() {
        SolarlogRegisterData solarlogRegisterData = solarlogService.getSolarogDataFromAPI("ruros300");
        LocalDateTime ldtexpected = LocalDateTime.of(2022, Month.NOVEMBER, 5, 12, 50);
        assertThat(solarlogRegisterData.getSolarlogDateField(SolarlogRegisterData.SOLARLOG_REGISTER.CREATEDDATE), is(equalTo(ldtexpected)));
        assertThat(solarlogRegisterData.getSolarlogIntegerField(SolarlogRegisterData.SOLARLOG_REGISTER.PACWRALL), is(equalTo(1036)));
        assertThat(solarlogRegisterData.getSolarlogIntegerField(SolarlogRegisterData.SOLARLOG_REGISTER.EAC_DAYSUM_CNT), is(equalTo(7370)));
        assertThat(solarlogRegisterData.getSolarlogIntegerField(SolarlogRegisterData.SOLARLOG_REGISTER.EAC_TOTAL_CNT), is(equalTo(27495067)));
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
