package ch.softhenge.solarlog.solarlog.pojo;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Test the SolarlogData5Min using the test file
 */
class SolarlogData5MinTest {

    @Test
    public void testSolarlogData5Min() throws IOException {
        String jsonFile = IOUtils.resourceToString("/record5min.json", StandardCharsets.UTF_8);
        SolarlogData5Min solarlogData5Min = new Gson().fromJson(jsonFile, SolarlogData5Min.class);
        assertThat(solarlogData5Min.getEAcUsageDay(), is(12));
        assertThat(solarlogData5Min.getInverters().get(1).getInverterNr(), is(equalTo(2)));
        assertThat(solarlogData5Min.getPhases().get(0).getPAc(), is(equalTo(-300)));
        assertThat(solarlogData5Min.getWeather().getSunriseDatetime(), is(equalTo(1667282998)));

        String myDate = "2022-10-11T21:35:00Z";
        assertThat(solarlogData5Min.getRecordTimestamp().get$date(), is(equalTo(myDate)));
        Instant instant = Instant.parse(myDate);
        assertThat(solarlogData5Min.getRecordTimestampAsInstant(), is(equalTo(instant)));

        myDate = "2022-10-11T21:40:02Z";
        assertThat(solarlogData5Min.getUpdateTimestamp().get$date(), is(equalTo(myDate)));
        instant = Instant.parse(myDate);
        assertThat(solarlogData5Min.getUpdateTimestampAsInstant(), is(equalTo(instant)));
    }

    @Test
    public void testSolarlogData5MinFromToJson() throws IOException, JSONException {
        String jsonFile = IOUtils.resourceToString("/record5min.json", StandardCharsets.UTF_8);
        SolarlogData5Min solarlogData5Min = new Gson().fromJson(jsonFile, SolarlogData5Min.class);
        String jsonFileFromObject = new Gson().toJson(solarlogData5Min);
        JSONAssert.assertEquals(jsonFile, jsonFileFromObject, JSONCompareMode.STRICT);
    }

}