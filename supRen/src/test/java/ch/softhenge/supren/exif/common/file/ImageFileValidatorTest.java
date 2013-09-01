package ch.softhenge.supren.exif.common.file;

import static org.junit.Assert.*;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import ch.softhenge.supren.exif.common.TestFile;
import ch.softhenge.supren.exif.file.ImageFileValidator;
import ch.softhenge.supren.exif.property.UserPropertyReader;
import ch.softhenge.supren.exif.property.UserPropertyReader.PropertyName;

public class ImageFileValidatorTest {

	@Test
	public void testFilesCheck() {
		UserPropertyReader ur = new UserPropertyReader("ruro.properties");
		Map<PropertyName, Map<Integer, String>> propertyMap = ur.getMapOfPropertyMap();		
		
		Map<Integer, String> filePatternMap = propertyMap.get(PropertyName.InfilePattern);
		Map<Integer, String> fileExtPatternMap = propertyMap.get(PropertyName.FileExtensionPattern);
		ImageFileValidator fileVal = new ImageFileValidator(filePatternMap, fileExtPatternMap.get(UserPropertyReader.INDEX_IF_EXACTLYONE));
		for (TestFile testFile : TestFile.values()) {
			boolean validImage = fileVal.validate(testFile.getFileName());
			assertThat(testFile.getFileName(), validImage, CoreMatchers.is(true));
		}
	}
}
