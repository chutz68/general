package ch.softhenge.supren.exif.factory;

import static org.junit.Assert.*;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import ch.softhenge.supren.exif.common.TestFile;

public class ExifServiceTest {

	private ExifService exifService;

	@Before
	public void setUp() throws Exception {
		exifService = new ExifService();
	}

	@Test
	public void testGetPictureDatePositive() {
		TestFile ce6dimgfile = TestFile.Ce6dImgFile;
		
		File file = ce6dimgfile.getFile();
		Date pictureDate = exifService.getPictureDate(file);
		assertNotNull(pictureDate);
		
		Format fileFormat = new SimpleDateFormat("YYYYMMDD");
		String pictureDateFormat = fileFormat.format(pictureDate);
		String pictureExpFormat = fileFormat.format(ce6dimgfile.getExifDate().getTime());
		assertEquals(pictureExpFormat, pictureDateFormat);
	}
	
	@Test
	public void testGetPictureDateNegative() {
		TestFile oldImageFile = TestFile.OldImgFile;
		
		File file = oldImageFile.getFile();
		Date pictureDate = exifService.getPictureDate(file);
		assertNull(pictureDate);
	}
	
	
	@Test
	public void testGetCameraModelPositive() {
		TestFile ce6dimgfile = TestFile.Ce6dImgFile;
		
		File file = ce6dimgfile.getFile();
		String cameraModel = exifService.getCameraModel(file);
		assertEquals("Canon EOS 6D", cameraModel);
	}
	
	@Test
	public void testGetCameraModelNegative() {
		TestFile oldImageFile = TestFile.OldImgFile;
		
		File file = oldImageFile.getFile();
		String cameraModel = exifService.getCameraModel(file);
		assertNull(cameraModel);
	}
	
}
