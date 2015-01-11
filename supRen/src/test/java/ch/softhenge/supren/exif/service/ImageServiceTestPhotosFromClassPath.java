package ch.softhenge.supren.exif.service;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import ch.softhenge.supren.exif.entity.FilePattern;
import ch.softhenge.supren.exif.entity.ImageFile;

/**
 * Test Photos from Class Path
 * 
 * @author werni
 *
 */
public class ImageServiceTestPhotosFromClassPath {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 

	private ImageService imageService;

	@Before
	public void setUp() throws Exception {
		String fileURL = this.getClass().getClassLoader().getResource("imgfiles").getPath();
		imageService = new ImageService("ruro.properties", fileURL);
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
		imageService.createImageFilesMap();
		Map<FilePattern, Collection<ImageFile>> mapOfImageFiles = imageService.getMapOfImageFiles();
		imageService.enrichImageFilesWithExifInfo(new StringBuffer());
		int cntAll = 0;
		int cntKnown = 0;
		int cntUnkP = 0;
		int cntOutP = 0;
		for (Collection<ImageFile> imageFiles : mapOfImageFiles.values()) {
			for (ImageFile imageFile : imageFiles) {
				LOGGER.info(imageFile.getOriginalFileName());
				cntAll++;
				if (imageFile.isKnownCameraModel()) {
					cntKnown++;
				}
				if (imageFile.getFilePattern().isOutPattern()) {
					cntOutP++;
				}
				if (imageFile.getFilePattern().isUnknownPattern()) {
					cntUnkP++;
				}
			}
		}
		assertThat("Count all is wrong", cntAll, CoreMatchers.is(18));
		assertThat("Count Known is wrong", cntKnown, CoreMatchers.is(16));
		assertThat("Count Unknown Pattern is wrong", cntUnkP, CoreMatchers.is(2));
		assertThat("Count Outfile Pettern is wrong", cntOutP, CoreMatchers.is(1));
	}

	@Test
	public void testMvCommand() {
		imageService.createMvAndUndoCommands();
		String mvCommand = imageService.getMvCommand();
		checkMvCommand(mvCommand);
		LOGGER.fine("mvCommands");
		LOGGER.fine(mvCommand);
	}

	@Test
	public void testMvUndoCommand() {
		imageService.createMvAndUndoCommands();
		String mvUndoCommand = imageService.getMvUndoCommand();
		checkMvCommand(mvUndoCommand);
		LOGGER.fine("mvUndoCommand");
		LOGGER.fine(mvUndoCommand);
	}

	@Test
	public void testMvAlreadyDone() {
		imageService.createMvAndUndoCommands();
		String mvAlreadyDone = imageService.getMvAlreadyDone();
		assertThat(mvAlreadyDone, CoreMatchers.containsString("20091031_E400_2177.JPG"));

		LOGGER.fine("mvAlreadyDone");
		LOGGER.fine(mvAlreadyDone);
	}
	
	@Test
	public void testMvError() {
		imageService.createMvAndUndoCommands();
		String mvError = imageService.getMvError();
		int countMatches = org.apache.commons.lang3.StringUtils.countMatches(mvError, "# ImageFile ");
		assertThat("Number of error mv commands", countMatches, CoreMatchers.is(3));
		assertThat(mvError, CoreMatchers.containsString("C6ZH_019.JPG"));
		assertThat(mvError, CoreMatchers.containsString("EOS600D_20121208_0583.jpg"));
		assertThat(mvError, CoreMatchers.containsString("Img_0001.jpg"));
		assertThat(mvError, CoreMatchers.containsString("Filepattern is unknown"));
		assertThat(mvError, CoreMatchers.containsString("Unknown Camera type"));

		LOGGER.fine("mvError");
		LOGGER.fine(mvError);
	}
	
	@Test
	public void testGetListOfUnknownImageFiles() {
		Collection<ImageFile> imageFiles = imageService.getListOfUnknownImageFiles();
		assertEquals(imageFiles.size(), 0);
	}
	
	@Test
	public void testCreateCsvSeperatedStringOfImageFiles() {
		String csvText = imageService.createCsvSeperatedStringOfImageFiles();
		LOGGER.info(csvText);
	}
	
	private void checkMvCommand(String mvCommand) {
		int countMatches = org.apache.commons.lang3.StringUtils.countMatches(mvCommand, "mv ");
		assertThat("Number of mv commands", countMatches, CoreMatchers.is(14));
		assertThat(mvCommand, CoreMatchers.containsString("P9090055.JPG"));
		assertThat(mvCommand, CoreMatchers.containsString("20010909_O210_0055.JPG"));
		assertThat(mvCommand, CoreMatchers.containsString("DSC01939.ARW"));
		assertThat(mvCommand, CoreMatchers.containsString("20140717_S100_1939.ARW"));
		assertThat(mvCommand, CoreMatchers.containsString("IMG_1426.jpg"));
		assertThat(mvCommand, CoreMatchers.containsString("20121208_E600_1426.jpg"));
	}
	
}
