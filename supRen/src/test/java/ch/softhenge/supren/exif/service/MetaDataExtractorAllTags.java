package ch.softhenge.supren.exif.service;

import static org.junit.Assert.*;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import ch.softhenge.supren.exif.common.TestFile;

public class MetaDataExtractorAllTags {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 

	private Metadata metadata;
	
	@Before
	public void setUp() throws Exception {
		TestFile ce6dimgfile = TestFile.Ce6dImgFile;
		metadata = ImageMetadataReader.readMetadata(ce6dimgfile.getFile());
	}

	@Test
	public void testListAllTags() {
		
		for (Directory directory : metadata.getDirectories()) {
		    for (Tag tag : directory.getTags()) {
		    	LOGGER.info(tag.toString());
		    }
		}
	}
}
