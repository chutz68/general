package ch.softhenge.supren.exif.property;

import ch.softhenge.supren.exif.property.UserPropertyReader.PropertyName;
import org.hamcrest.number.OrderingComparison;
import org.junit.Test;

import java.util.Map;
import java.util.Map.Entry;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class UserPropertyReaderTest {

	@Test
	public void testUserPropertyReaderRuro() throws Exception, Exception {
		UserPropertyReader ur = new UserPropertyReader("ruro.properties");
		Map<PropertyName, Map<Integer, String>> propertyMap = ur.getMapOfPropertyMap();

		assertThat(propertyMap.size(), is(11));
		int expect;
		for (Entry<PropertyName, Map<Integer, String>> propertyEntry : propertyMap.entrySet()) {
			switch (propertyEntry.getKey()) {
			case FileExtensionPattern:
				assertThat(propertyEntry.getValue().size(), is(1));
				break;
			case fileExtensionList:
				assertThat(propertyEntry.getValue().size(), is(1));
				break;
			case InfilePattern:
				expect = 40;
				checkPropertyEntry(expect, propertyEntry);
				break;
			case InfilePatternImgNumGroup:
				expect = 40;
				checkPropertyEntry(expect, propertyEntry);
				break;
		    case CameraModel:
		    	expect = 58;
				checkPropertyEntry(expect, propertyEntry);
				break;
			case CameraModel4ch:
				expect = 58;
				checkPropertyEntry(expect, propertyEntry);
				break;
			case OutfilePattern:
				assertThat(propertyEntry.getValue().size(), is(1));
				String property = propertyEntry.getValue().get(UserPropertyReader.INDEX_IF_EXACTLYONE);
				assertNotNull(property);
				break;
			case OutfilePatternImgNumGroup:
				assertThat(propertyEntry.getValue().size(), is(1));
				break;
			case OutfilePatternGroup:
				expect = 5;
				checkPropertyEntry(expect, propertyEntry);
				break;
			case OutfileCase:
				assertThat(propertyEntry.getValue().size(), is(1));
				break;
			case CameraModel4chUnknown:
				assertThat(propertyEntry.getValue().size(), is(1));
				break;
			default:
				throw new Exception("Unhandled property: " + propertyEntry.getKey());
			}
		}
	}

	private void checkPropertyEntry(int expect, Entry<PropertyName, Map<Integer, String>> propertyEntry) {
		assertThat("for " + propertyEntry.getKey(), propertyEntry.getValue().size(), is(expect));
		for (Entry<Integer, String> stringEntry : propertyEntry.getValue().entrySet()) {
			assertThat(stringEntry.getKey(), is(OrderingComparison.lessThanOrEqualTo(expect)));
		}
	}

}
