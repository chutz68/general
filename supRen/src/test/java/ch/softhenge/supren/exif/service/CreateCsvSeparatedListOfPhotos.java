package ch.softhenge.supren.exif.service;

import org.junit.Before;
import org.junit.Test;

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

public class CreateCsvSeparatedListOfPhotos {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 
	private final static long DAYS_BACK = 0;
//	private final static String DIRECTORY = "C:\\photosbest\";
//	private final static String DIRECTORY = "D:\\photos\\transfer";
	private final static String DIRECTORY = "C:\\photosbest\\2020_10Sardinien";
	
	private ImageService imageService;
	private DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd-HHmm");

	@Before
	public void setUp() throws Exception {
		imageService = new ImageService("ruro.properties", DIRECTORY, false, new ExifServiceMetaDataExtractor());
		LOGGER.setLevel(Level.FINE);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
		LOGGER.addHandler(handler);
	}


	@Test
	public void testCreateCsvSeperatedStringOfImageFilesandMv() throws IOException {
		String csvText = imageService.createCsvSeperatedStringOfImageFiles(DAYS_BACK);
		File file = new File("csvFileOut_" + dateFormat.format(new Date()) + ".csv");
		FileWriter fw = new FileWriter(file);
		
	    BufferedWriter bw = new BufferedWriter(fw);
	    bw.write(csvText);
	    bw.close();
	}
	
}
