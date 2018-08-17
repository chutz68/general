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

public class RenameNewPhotosMetaDataExtractorTest {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 
	private final static long DAYS_BACK = 0;
	private final static boolean FORCE_UNKNOWN_IMAGES = false;
	//private final static String[] DIRECTORIES = { "D:\\", "C:\\" };
	//private final static String[] DIRECTORIES = { "D:\\photos", "C:\\Users", "C:\\photos" };
	private final static String[] DIRECTORIES = { "I:\\" };
	//private final static String[] DIRECTORIES = { "D:\\photos\\videos" };
	
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
		File file = new File("mvCommand_" + dateFormat.format(new Date()));
		FileWriter fw = new FileWriter(file);
	    BufferedWriter bw = new BufferedWriter(fw);
	    bw = new BufferedWriter(fw);
	    
		File fileErr = new File("error_" + dateFormat.format(new Date()));
		FileWriter fwErr = new FileWriter(fileErr);
	    BufferedWriter bwErr = new BufferedWriter(fwErr);
	    bwErr = new BufferedWriter(fwErr);

	    for (String directory : DIRECTORIES) {
			imageService = new ImageService("ruro.properties", directory, FORCE_UNKNOWN_IMAGES, new ExifServiceMetaDataExtractor());
			imageService.createMvAndUndoCommands(DAYS_BACK);
			String mvCommand = imageService.getMvCommand();
			bw.write(mvCommand);
			String error = imageService.getMvError();
			bwErr.write(error);			
		}
	    bw.close();	
	    bwErr.close();	
	}
	
}
