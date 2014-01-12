package ch.softhenge.supren.exif.property;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.ValidationException;

import org.hamcrest.number.OrderingComparison;
import org.junit.Test;

import ch.softhenge.supren.exif.property.UserPropertyReader.PropertyName;

public class UserPropertyReaderTest {

	@Test
	public void testUserPropertyReaderRuro() throws ValidationException {
		UserPropertyReader ur = new UserPropertyReader("ruro.properties");
		Map<PropertyName, Map<Integer, String>> propertyMap = ur.getMapOfPropertyMap();

		assertThat(propertyMap.size(), is(9));
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
				expect = 9;
				checkPropertyEntry(expect, propertyEntry);
				break;
			case InfilePatternImgNumGroup:
				expect = 9;
				checkPropertyEntry(expect, propertyEntry);
				break;
		    case CameraModel:
		    	expect = 14;
				checkPropertyEntry(expect, propertyEntry);
				break;
			case CameraModel4ch:
				expect = 14;
				checkPropertyEntry(expect, propertyEntry);
				break;
			case OutfilePattern:
				assertThat(propertyEntry.getValue().size(), is(1));
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
			default:
				throw new ValidationException("Unhandled property: " + propertyEntry.getKey());
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
