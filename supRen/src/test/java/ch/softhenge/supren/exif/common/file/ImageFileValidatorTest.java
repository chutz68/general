package ch.softhenge.supren.exif.common.file;

import static org.junit.Assert.*;

import java.util.Map.Entry;
import java.util.regex.Pattern;

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
			Entry<Integer, Pattern> filePatternEntry = fileVal.getIndexOfKnownFilePattern(testFile.getFileName());
			Integer indexOfFilePattern = filePatternEntry.getKey();
			String imgNum = fileVal.getInfilePatternImgNum(testFile.getFileName(), indexOfFilePattern );

			switch(testFile) {
			case CR2File: 
				assertThat(testFile.getFileName(), 3, CoreMatchers.is(indexOfFilePattern));
				assertEquals(testFile.getFileName(), "0321", imgNum);
				break;
			case Ce6dImgFile:
				assertThat(testFile.getFileName(), indexOfFilePattern, CoreMatchers.is(1));
				assertEquals(testFile.getFileName(), "0652", imgNum);
				break;
			case OldImgFile:
				assertThat(testFile.getFileName(), indexOfFilePattern, CoreMatchers.is(6));
				assertEquals(testFile.getFileName(), "0009", imgNum);
				break;
			case ImgFileNoFileNum:
				assertThat(testFile.getFileName(), indexOfFilePattern, CoreMatchers.is(9));
				assertNull(testFile.getFileName(), imgNum);
				break;
			case ImgFileNoPattern:
				assertNull(testFile.getFileName(), indexOfFilePattern);
				assertNull(testFile.getFileName(), imgNum);
				break;
			}
		}
	}	
}
