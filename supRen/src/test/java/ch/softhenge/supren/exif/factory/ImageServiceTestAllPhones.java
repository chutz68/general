package ch.softhenge.supren.exif.factory;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ImageServiceTestAllPhones {

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
	public void testListFilesInDir() {
		imageService.listAllImageFilesInDir();
	}

	@Test
	@Ignore("Test takes about 180 s")
	public void testListImageFilesToRename() {
		imageService.createListOfImageFilesToRename();
	}
	
}
