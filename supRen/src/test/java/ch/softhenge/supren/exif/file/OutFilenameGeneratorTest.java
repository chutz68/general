package ch.softhenge.supren.exif.file;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ch.softhenge.supren.exif.property.UserPropertyReader;
import ch.softhenge.supren.exif.property.UserPropertyReader.PropertyName;

public class OutFilenameGeneratorTest {

	private OutFilenameGenerator outFilenameGenerator;
	
	@Before
	public void setUp() throws Exception {
		UserPropertyReader upr = new UserPropertyReader("ruro.properties");
		Map<Integer, String> outfilePatternMap = upr.getPropertyMapOfProperty(PropertyName.OutfilePattern);
		String outfilePattern = outfilePatternMap.get(UserPropertyReader.INDEX_IF_EXACTLYONE);
		Map<Integer, String> outfilePatternGroupMap = upr.getPropertyMapOfProperty(PropertyName.OutfilePatternGroup);
		outFilenameGenerator = new OutFilenameGenerator(outfilePattern, outfilePatternGroupMap);
	}

	@Test
	public void testCreateFileNameE6DJuly2014() {
		Calendar cal = new GregorianCalendar(2014, Calendar.JULY, 17);
		String cameraModel4ch = "E-6D";
		Integer imageNumber = 334;
		String newFileName = outFilenameGenerator.createFileName(cal.getTime(), cameraModel4ch , imageNumber);
		assertEquals("20140717_E-6D_0334", newFileName);
	}

}
