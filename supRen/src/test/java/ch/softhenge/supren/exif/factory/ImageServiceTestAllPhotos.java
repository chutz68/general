package ch.softhenge.supren.exif.factory;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import ch.softhenge.supren.exif.entity.ImageFile;

public class ImageServiceTestAllPhotos {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 

	private ImageService imageService;

	@Before
	public void setUp() throws Exception {
		imageService = new ImageService("ruro.properties", "D:\\photos");
		LOGGER.setLevel(Level.FINE);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
		LOGGER.addHandler(handler);
	}

	@Test
	//@Ignore("Test takes about 180 s")
	public void testListImageFilesToRename() {
		imageService.createImageFilesMap();
		Map<String, Collection<ImageFile>> mapOfImageFileCollection = imageService.getMapOfImageFiles();
		for (Entry<String, Collection<ImageFile>> imageFiles : mapOfImageFileCollection.entrySet()) {
			LOGGER.info("Image Files of pattern " + imageFiles.getKey() + " has " + imageFiles.getValue().size() + " values");
		}
	}
	
}
