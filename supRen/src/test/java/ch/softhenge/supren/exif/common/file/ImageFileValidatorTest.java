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
			Integer imgNum = fileVal.getInfilePatternImgNum(testFile.getFileName(), indexOfFilePattern);

			switch(testFile) {
			case CR2File: 
				assertThat(testFile.getFileName(), 3, CoreMatchers.is(indexOfFilePattern));
				assertEquals(testFile.getFileName(), Integer.valueOf(321), imgNum);
				break;
			case Ce6dImgFile:
				assertThat(testFile.getFileName(), indexOfFilePattern, CoreMatchers.is(1));
				assertEquals(testFile.getFileName(), Integer.valueOf(652), imgNum);
				break;
			case OldImgFile:
				assertThat(testFile.getFileName(), indexOfFilePattern, CoreMatchers.is(6));
				assertEquals(testFile.getFileName(), Integer.valueOf(9), imgNum);
				break;
			}
		}
	}	
}
