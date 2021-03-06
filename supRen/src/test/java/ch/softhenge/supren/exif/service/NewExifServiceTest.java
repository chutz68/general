package ch.softhenge.supren.exif.service;

import static org.junit.Assert.*;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import ch.softhenge.supren.exif.common.TestFile;
import ch.softhenge.supren.exif.entity.ExifFileInfo;
import ch.softhenge.supren.exif.service.ExifServiceMetaDataExtractor;

public class NewExifServiceTest {

	private ExifService newExifService;

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 

	
	@Before
	public void setUp() throws Exception {
		newExifService = new NewExifServiceMetaDataExtractor();
	}

	@Test
	public void testGetExifInfoFromImageFile() {
		TestFile ce6dimgfile = TestFile.Ce6dImgFile;
		
		File file = ce6dimgfile.getFile();
		ExifFileInfo exifFileInfo = newExifService.getExifInfoFromImageFile(file, ce6dimgfile.getFileName());
		
		Format fileFormat = new SimpleDateFormat("YYYYMMDD");
		String pictureDateFormat = fileFormat.format(exifFileInfo.getPictureDate());
		String pictureExpFormat = fileFormat.format(ce6dimgfile.getExifDate().getTime());

		LOGGER.info(exifFileInfo.toString());
		
		assertEquals(pictureExpFormat, pictureDateFormat);
		assertEquals("Canon EOS 6D", exifFileInfo.getCameraModel());
	}
	
	@Test
	public void testGetPictureDateNegative() {
		TestFile oldImageFile = TestFile.OldImgFile;
		
		File file = oldImageFile.getFile();
		ExifFileInfo exifFileInfo = newExifService.getExifInfoFromImageFile(file, oldImageFile.getFileName());
		assertNull(exifFileInfo.getPictureDate());
		assertNull(exifFileInfo.getCameraModel());
	}
}
