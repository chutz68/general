package ch.softhenge.solarlog.solarlog.pojo;


import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Month;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test the Solarlog300Data using the Solarlog test file
 */
class Solarlog300DataTest {

    @Test
    public void testSolarlogDataReader() throws IOException {
        String jsonFile = IOUtils.resourceToString("/solarlogdataNeuenhof.json", StandardCharsets.UTF_8);
        Solarlog300Data sol300 = new Solarlog300Data(jsonFile);
        LocalDateTime ldtexpected = LocalDateTime.of(2022, Month.NOVEMBER, 5, 12, 50);
        assertThat(sol300.getSolarlogDateField(Solarlog300Data.SOLARLOG300_REGISTER.CREATEDDATE), is(equalTo(ldtexpected)));
        assertThat(sol300.getSolarlogIntegerField(Solarlog300Data.SOLARLOG300_REGISTER.PACWRALL), is(equalTo(1036)));
        assertThat(sol300.getSolarlogIntegerField(Solarlog300Data.SOLARLOG300_REGISTER.EAC_DAYSUM_CNT), is(equalTo(7370)));
        assertThat(sol300.getSolarlogIntegerField(Solarlog300Data.SOLARLOG300_REGISTER.EAC_TOTAL_CNT), is(equalTo(27495067)));
    }

}