package ch.softhenge.supren.exif.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

public class RenameNewPhotos {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 
	private final static long DAYS_BACK = 30;
	private final static String[] DIRECTORIES = { "C:\\photos",  "D:\\photos\\transfer" };
	
	private ImageService imageService;
	private DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd-HHmm");

	@Before
	public void setUp() throws Exception {
		LOGGER.setLevel(Level.FINE);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
		LOGGER.addHandler(handler);
	}

	
	
	@Test
	public void testCreateCsvSeperatedStringOfImageFilesandMv() throws IOException {
		for (String directory : DIRECTORIES) {
			imageService = new ImageService("ruro.properties", directory);
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
		}
	}
	
}
