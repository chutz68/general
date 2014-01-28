package ch.softhenge.supren.exif.factory;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import ch.softhenge.supren.exif.entity.ImageFile;

public class ImageServiceTestNewPhotos {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 

	private ImageService imageService;

	@Before
	public void setUp() throws Exception {
		imageService = new ImageService("ruro.properties", "C:\\photos\\2013_07_Schwarzwald");
		LOGGER.setLevel(Level.FINE);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
		LOGGER.addHandler(handler);
	}

	@Test
	public void testListImageFilesToRename() {
		imageService.createImageFilesMap();
	}
	
	@Test
	public void testGetListOfUnknownImageFiles() {
		Collection<ImageFile> imageFiles = imageService.getListOfUnknownImageFiles();
		assertEquals(imageFiles.size(), 0);
	}
	
}
