package ch.softhenge.solarlog.solarlog.pojo;


import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test the SolarlogData using the Solarlog test file
 */
class SolarlogRegisterDataTest {

    @Test
    public void testSolarlogRegisterDataReader() throws IOException {
        String jsonFile = IOUtils.resourceToString("/solarlogdataNeuenhof.json", StandardCharsets.UTF_8);
        SolarlogRegisterData solData = new SolarlogRegisterData(jsonFile, ZoneId.of("Europe/Paris"));
        LocalDateTime ldtexpected = LocalDateTime.of(2022, Month.NOVEMBER, 5, 12, 50);
        assertThat(solData.getSolarlogDateField(SolarlogRegisterData.SOLARLOG_REGISTER.CREATEDDATE), is(equalTo(ldtexpected)));
        assertThat(solData.getSolarlogIntegerField(SolarlogRegisterData.SOLARLOG_REGISTER.PACWRALL), is(equalTo(1036)));
        assertThat(solData.getSolarlogIntegerField(SolarlogRegisterData.SOLARLOG_REGISTER.EAC_DAYSUM_CNT), is(equalTo(7370)));
        assertThat(solData.getSolarlogIntegerField(SolarlogRegisterData.SOLARLOG_REGISTER.EAC_TOTAL_CNT), is(equalTo(27495067)));
    }

}