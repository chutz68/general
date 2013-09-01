package ch.softhenge.supren.exif.common;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class TestExif {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 
	
	@Before
	public void setUp() throws Exception {
		LOGGER.setLevel(Level.FINE);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
		LOGGER.addHandler(handler);
	}	
	
	@Test
	public void testCanonJpgFile() throws ImageProcessingException, IOException {
		checkFileTags(TestFile.Ce6dImgFile);
	}

	@Test
	public void testCanonRawFile() throws ImageProcessingException, IOException {
		checkFileTags(TestFile.CR2File);
	}	

	@Test
	public void testVeryOldJPG() throws ImageProcessingException, IOException {
		checkFileTags(TestFile.OldImgFile);
	}	
	
	private void checkFileTags(TestFile testFile) throws IOException, ImageProcessingException {
		File file = testFile.getFile();
		Metadata meta = ImageMetadataReader.readMetadata(file);
		assertNotNull(meta);
		
		ExifSubIFDDirectory exifSubDir = meta.getDirectory(ExifSubIFDDirectory.class);
		
		if (testFile.getExifDate() == null) {
			assertNull(testFile.getExifDate());
			assertNull(exifSubDir);
		} else {
			Date pictureDate = exifSubDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
			assertNotNull(pictureDate);
			
			Format fileFormat = new SimpleDateFormat("YYYYMMDD");
			String pictureDateFormat = fileFormat.format(pictureDate);
			String pictureExpFormat = fileFormat.format(testFile.getExifDate().getTime());
			assertEquals(pictureExpFormat, pictureDateFormat);
			
			LOGGER.fine("dir: " + exifSubDir + " fileURL: " +  testFile.getFile() + "		: " + pictureDate);
		}
		
		for (Directory directory : meta.getDirectories()) {
			for (Tag tag : directory.getTags()) {
				LOGGER.finer(tag.toString());
			}
		}
	}
}
