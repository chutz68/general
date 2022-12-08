package ch.softhenge.solarlog.solarlog.pojo;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SolarlogData5MinTestFactory {

    /**
     * return records of the requested day + adds 3 hours before teat day and 3 hours after that day
     * @param year year as yyyy
     * @param month month
     * @param dayOfMonth the day of the month
     * @return a list of SolarlogData5Min ordered that contains of 5 minute values
     *      * @throws IOException
     */
    public List<SolarlogData5Min> create5minDataRecords(int year, Month month, int dayOfMonth) throws IOException {
        List<SolarlogData5Min> solarlog5MinList = new ArrayList<>();
        String jsonFile = IOUtils.resourceToString("/record5min.json", StandardCharsets.UTF_8);
        SolarlogData5Min solarlogData5Min = new Gson().fromJson(jsonFile, SolarlogData5Min.class);

        LocalDate ldateStart = LocalDate.of(year, month, dayOfMonth).minusDays(1);
        Instant instStart = ldateStart.atStartOfDay().toInstant(ZoneOffset.UTC).plusSeconds(21 * 60 * 60);

        for (int i = 0; i <= 30 * 60 / 5; i++) {
            Instant newInstant = instStart.plusSeconds(i * 5 * 60);
            SolarlogData5Min solarlogData5MinCopy = cloneSolarlogData5Min(solarlogData5Min, newInstant, i + 1);
            solarlog5MinList.add(solarlogData5MinCopy);
        }

        return solarlog5MinList;
    }

    /**
     * creates a new Solarlog Object copied from the original one
     *
     * @param orig
     * @param recordTimestamp
     * @param recordVersion
     * @return new Solarlog Object
     */
    public SolarlogData5Min cloneSolarlogData5Min(SolarlogData5Min orig, Instant recordTimestamp, int recordVersion) {
        SolarlogData5Min newSolarlog = new SolarlogData5Min();
        newSolarlog.setHeatpump(orig.getHeatpump());
        newSolarlog.setInverters(orig.getInverters());
        newSolarlog.setPhases(orig.getPhases());
        newSolarlog.setEAcInverterDay(orig.getEAcInverterDay());
        newSolarlog.setPAcInverter(orig.getPAcInverter());
        newSolarlog.setPAcUsage(orig.getPAcUsage());
        newSolarlog.setRecordState(orig.getRecordState());
        newSolarlog.setWeather(orig.getWeather());

        String recordTimeStampString = recordTimestamp.toString();
        SolarlogData5Min.RecordTimestamp rts = new SolarlogData5Min.RecordTimestamp();
        rts.set$date(recordTimeStampString);

        newSolarlog.setRecordTimestamp(rts);
        newSolarlog.setRecordVersion(recordVersion);

        Instant updateTimestamp = newSolarlog.getRecordTimestampAsInstant().plusSeconds(90);
        String updateTimestampString = updateTimestamp.toString();
        SolarlogData5Min.UpdateTimestamp uts = new SolarlogData5Min.UpdateTimestamp();
        uts.set$date(updateTimestampString);
        newSolarlog.setUpdateTimestamp(uts);

        return newSolarlog;
    }

    @Test
    public void testSolarlog5MinCloner() throws IOException {
        String jsonFile = IOUtils.resourceToString("/record5min.json", StandardCharsets.UTF_8);
        SolarlogData5Min solarlogData5Min = new Gson().fromJson(jsonFile, SolarlogData5Min.class);

        String myDate = "2022-12-11T20:10:05Z";
        Instant instant = Instant.parse(myDate);

        SolarlogData5Min solarlogDataClone = cloneSolarlogData5Min(solarlogData5Min, instant, 2);
        assertThat(solarlogDataClone.getRecordTimestampAsInstant(), is(equalTo(instant)));
        assertThat(solarlogDataClone.getRecordVersion(), is(equalTo(2)));
        assertThat(solarlogDataClone.getUpdateTimestampAsInstant(), is(greaterThan(solarlogDataClone.getRecordTimestampAsInstant())));
    }

    @Test
    public void testSolarlog5MinCopier() throws IOException {
        List<SolarlogData5Min> solarlogList = create5minDataRecords(2022, Month.DECEMBER, 5);
        assertThat(solarlogList.size(), is(equalTo(361)));
        solarlogList.sort(SolarlogData5Min::compareTo);
        SolarlogData5Min solarLog5MinPrev = null;
        for (SolarlogData5Min solarLog5Min : solarlogList) {
            if (solarLog5MinPrev != null) {
                assertThat(solarLog5Min.getRecordTimestampAsInstant(), is(equalTo(solarLog5MinPrev.getRecordTimestampAsInstant().plusSeconds(300))));
            }
            solarLog5MinPrev = solarLog5Min;
        }
        String lastRecordDate = solarLog5MinPrev.getRecordTimestampAsInstant().toString();
        assertThat(lastRecordDate, is(equalTo("2022-12-06T03:00:00Z")));
    }
}
