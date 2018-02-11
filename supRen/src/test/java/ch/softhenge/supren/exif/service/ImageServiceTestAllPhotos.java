package ch.softhenge.supren.exif.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ch.softhenge.supren.exif.entity.FilePattern;
import ch.softhenge.supren.exif.entity.ImageFile;

public class ImageServiceTestAllPhotos {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 
	private final static long DAYS_BACK = 10;
	private final static String DIRECTORY = "D:\\photos\\2017_04NapoliIschia";
//	private final static String DIRECTORY = "D:\\photos\\transfer";
//	private final static String DIRECTORY = "D:\\photos\\2016_09_25MunichOktoberfest";
	
	private ImageService imageService;
	private DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd-HHmm");

	@Before
	public void setUp() throws Exception {
		imageService = new ImageService("ruro.properties", DIRECTORY, false);
		LOGGER.setLevel(Level.FINE);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
		LOGGER.addHandler(handler);
	}

	@Test
	@Ignore
	public void testListImageFilesToRename() {
		imageService.createImageFilesMap(DAYS_BACK);
		imageService.createImageFilesMap(DAYS_BACK);
		Map<FilePattern, Collection<ImageFile>> mapOfImageFileCollection = imageService.getMapOfImageFiles();
		for (Entry<FilePattern, Collection<ImageFile>> imageFiles : mapOfImageFileCollection.entrySet()) {
			LOGGER.info("Image Files of pattern " + imageFiles.getKey() + " has " + imageFiles.getValue().size() + " values");
		}
	}
	
	
	@Test
	public void testCreateCsvSeperatedStringOfImageFilesandMv() throws IOException {
		String csvText = imageService.createCsvSeperatedStringOfImageFiles(DAYS_BACK);
		File file = new File("csvFileOut_" + dateFormat.format(new Date()) + ".csv");
		FileWriter fw = new FileWriter(file);
		
	    BufferedWriter bw = new BufferedWriter(fw);
	    bw.write(csvText);
	    bw.close();

		imageService.createMvAndUndoCommands(DAYS_BACK);
		String mvCommand = imageService.getMvCommand();
		file = new File("mvCommand_" + dateFormat.format(new Date()));
		fw = new FileWriter(file);
	    bw = new BufferedWriter(fw);
	    bw.write(mvCommand);
	    bw.close();

//	    String mvUndoCommand = imageService.getMvUndoCommand();
//		file = new File("mvUndoCommand_" + dateFormat.format(new Date()));
//		fw = new FileWriter(file);
//	    bw = new BufferedWriter(fw);
//	    bw.write(mvUndoCommand);
//	    bw.close();

//	    String mvAlreadyDone = imageService.getMvAlreadyDone();
//		file = new File("mvAlreadyDone_" + dateFormat.format(new Date()));
//		fw = new FileWriter(file);
//	    bw = new BufferedWriter(fw);
//	    bw.write(mvAlreadyDone);
//	    bw.close();
//	    
//	    String mvErrorCommand = imageService.getMvError();
//		file = new File("mvErrorCommand_" + dateFormat.format(new Date()));
//		fw = new FileWriter(file);
//	    bw = new BufferedWriter(fw);
//	    bw.write(mvErrorCommand);
//	    bw.close();	
	    
	}
	
}
