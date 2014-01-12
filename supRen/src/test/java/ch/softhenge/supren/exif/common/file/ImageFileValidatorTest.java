package ch.softhenge.supren.exif.common.file;

import static org.junit.Assert.*;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import ch.softhenge.supren.exif.common.TestFile;
import ch.softhenge.supren.exif.file.ImageFileValidator;
import ch.softhenge.supren.exif.property.UserPropertyReader;

public class ImageFileValidatorTest {

	@Test
	public void testIndexOfKnownFilePattern() {
		UserPropertyReader uruserProteryReader = new UserPropertyReader("ruro.properties");
		ImageFileValidator fileVal = new ImageFileValidator(uruserProteryReader);
		for (TestFile testFile : TestFile.values()) {
			Integer indexOfFilePattern = fileVal.getIndexOfKnownFilePattern(testFile.getFileName());
			Integer infilePatternImgNum = fileVal.getInfilePatternImgNum(testFile.getFileName(), indexOfFilePattern);

			switch(testFile) {
			case CR2File: 
				assertThat(testFile.getFileName(), indexOfFilePattern, CoreMatchers.is(3));
				break;
			case Ce6dImgFile:
				assertThat(testFile.getFileName(), indexOfFilePattern, CoreMatchers.is(1));
				break;
			case OldImgFile:
				assertThat(testFile.getFileName(), indexOfFilePattern, CoreMatchers.is(6));
				break;
			}
		}
	}	
}
