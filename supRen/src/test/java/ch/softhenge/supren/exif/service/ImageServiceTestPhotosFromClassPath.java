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
		imageService = new ImageService("ruro.properties", fileURL, false, new ExifServiceMetaDataExtractor());
		LOGGER.setLevel(Level.FINE);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
		LOGGER.addHandler(handler);
	}

	@Test
	public void testListImageFilesToRename() {
		imageService.createImageFilesMap(null, null);
		imageService.resetImageFileList();
		imageService.createImageFilesMap(null, null);
		imageService.createImageFilesMap(null, null);
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
		assertThat("Count all is wrong", cntAll, CoreMatchers.is(6));
		assertThat("Count Known is wrong", cntKnown, CoreMatchers.is(4));
		assertThat("Count Unknown Pattern is wrong", cntUnkP, CoreMatchers.is(1));
		assertThat("Count Outfile Pattern is wrong", cntOutP, CoreMatchers.is(3));
	}

	@Test
	public void testMvCommand() {
		imageService.createMvAndUndoCommands(null);
		String mvCommand = imageService.getMvCommand();
		checkMvCommand(mvCommand);
		LOGGER.fine("mvCommands");
		LOGGER.fine(mvCommand);
	}

	@Test
	public void testMvUndoCommand() {
		imageService.createMvAndUndoCommands(null);
		String mvUndoCommand = imageService.getMvUndoCommand();
		checkMvCommand(mvUndoCommand);
		LOGGER.fine("mvUndoCommand");
		LOGGER.fine(mvUndoCommand);
	}

	@Test
	public void testMvAlreadyDone() {
		imageService.createMvAndUndoCommands(null);
		String mvAlreadyDone = imageService.getMvAlreadyDone();
		assertThat(mvAlreadyDone, CoreMatchers.containsString("20091031_E400_2177.JPG"));

		LOGGER.fine("mvAlreadyDone");
		LOGGER.fine(mvAlreadyDone);
	}
	
	@Test
	public void testMvError() {
		imageService.createMvAndUndoCommands(null);
		String mvError = imageService.getMvError();
		int countMatches = org.apache.commons.lang3.StringUtils.countMatches(mvError, "# ImageFile ");
		assertThat("Number of error mv commands", countMatches, CoreMatchers.is(2));
		assertThat(mvError, CoreMatchers.containsString("C6ZH_019.JPG"));
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
		String csvText = imageService.createCsvSeperatedStringOfImageFiles(null);
		LOGGER.info(csvText);
	}
	
	private void checkMvCommand(String mvCommand) {
		int countMatches = org.apache.commons.lang3.StringUtils.countMatches(mvCommand, "mv ");
		assertThat("Number of mv commands", countMatches, CoreMatchers.is(1));
		assertThat(mvCommand, CoreMatchers.containsString("IMG_0652.jpg"));
	}
	
}
