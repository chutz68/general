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

public class CopyBestOfTest {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 
	
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
	public void testCopyBestOfToNewFolderPhotos() throws IOException {
		File file = new File("bestOfCopy_" + dateFormat.format(new Date()));
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
	    bw = new BufferedWriter(fw);
		imageService = new ImageService("ruro.properties", "D:\\photos\\2017_Luna", false, new ExifServiceMetaDataExtractor());
		String copyBestOfToNewFolder = imageService.copyBestOfToNewFolder("D:\\photosBestOf\\original", "\\20");
		bw.write(copyBestOfToNewFolder);
		bw.close();
	}
	
}
