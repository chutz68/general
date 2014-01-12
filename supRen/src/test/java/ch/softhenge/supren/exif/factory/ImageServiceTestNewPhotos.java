package ch.softhenge.supren.exif.factory;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

public class ImageServiceTestNewPhotos {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 

	private ImageService imageService;

	@Before
	public void setUp() throws Exception {
		imageService = new ImageService("ruro.properties", "D:\\photos\\2013_12_15Einsiedeln");
		LOGGER.setLevel(Level.FINE);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
		LOGGER.addHandler(handler);
	}

	@Test
	public void testListFilesInDir() {
		imageService.listAllImageFilesInDir();
	}

	@Test
	public void testListImageFilesToRename() {
		imageService.listImageFilesToRename();
	}
	
}
