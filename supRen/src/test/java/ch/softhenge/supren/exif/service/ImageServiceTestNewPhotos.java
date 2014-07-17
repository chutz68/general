package ch.softhenge.supren.exif.service;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import ch.softhenge.supren.exif.entity.ImageFile;
import ch.softhenge.supren.exif.service.ImageService;

public class ImageServiceTestNewPhotos {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 

	private ImageService imageService;

	@Before
	public void setUp() throws Exception {
		imageService = new ImageService("ruro.properties", "D:\\photos\\2014_07_05-12Gardasee");
		LOGGER.setLevel(Level.FINE);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
		LOGGER.addHandler(handler);
	}

	@Test
	public void testListImageFilesToRename() {
		imageService.createImageFilesMap();
		imageService.resetImageFileList();
		imageService.createImageFilesMap();
	}

	@Test
	public void testMvCommand() {
		imageService.createImageFilesMap();
		imageService.getMvCommandToRenameFiles();
		String mvCommand = imageService.getMvCommandToRenameFiles();
		LOGGER.fine(mvCommand);
	}
	
	@Test
	public void testGetListOfUnknownImageFiles() {
		Collection<ImageFile> imageFiles = imageService.getListOfUnknownImageFiles();
		assertEquals(imageFiles.size(), 0);
	}
	
}
