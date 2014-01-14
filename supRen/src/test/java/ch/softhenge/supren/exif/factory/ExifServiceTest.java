package ch.softhenge.supren.exif.factory;

import static org.junit.Assert.*;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;

import org.junit.Before;
import org.junit.Test;

import ch.softhenge.supren.exif.common.TestFile;
import ch.softhenge.supren.exif.entity.ExifFileInfo;

public class ExifServiceTest {

	private ExifService exifService;

	@Before
	public void setUp() throws Exception {
		exifService = new ExifService();
	}

	@Test
	public void testGetExifInfoFromImageFile() {
		TestFile ce6dimgfile = TestFile.Ce6dImgFile;
		
		File file = ce6dimgfile.getFile();
		ExifFileInfo exifFileInfo = exifService.getExifInfoFromImageFile(file);
		
		Format fileFormat = new SimpleDateFormat("YYYYMMDD");
		String pictureDateFormat = fileFormat.format(exifFileInfo.getPictureDate());
		String pictureExpFormat = fileFormat.format(ce6dimgfile.getExifDate().getTime());

		assertEquals(pictureExpFormat, pictureDateFormat);
		assertEquals("Canon EOS 6D", exifFileInfo.getCameraModel());
	}
	
	@Test
	public void testGetPictureDateNegative() {
		TestFile oldImageFile = TestFile.OldImgFile;
		
		File file = oldImageFile.getFile();
		ExifFileInfo exifFileInfo = exifService.getExifInfoFromImageFile(file);
		assertNull(exifFileInfo.getPictureDate());
		assertNull(exifFileInfo.getCameraModel());
	}
}
