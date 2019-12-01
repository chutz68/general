package ch.softhenge.supren.exif.service;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import ch.softhenge.supren.exif.entity.ExifFileInfo;

/**
 * New Exif Services reads tags from image file using Meta Data Extractor
 * 
 * @author Werni
 *
 */
public class NewExifServiceMetaDataExtractor implements ExifService {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 

	@Override
	public ExifFileInfo getExifInfoFromImageFile(File imageFile, String fileName) {
		Metadata metadata = getExifMetadata(imageFile, fileName);
		if (metadata == null) {
			LOGGER.warning(fileName + " has no readable metadata");
			return null;
		}
		Map<Integer, String> allExtractorTags = MetaDataExtractorTags.getAllExtractorTags();
		Date pictureDate = null;
		String aperture = null;
		String exposureTime;
		Integer exposureProgram;
		String exposureMode;
		String exposureBias;
		String fNumber;
		String iso;
		String exifVersion;
		String exposureFNumber;
		String lensModel;
		String lensSpecification;
		String focalLength;
		String shutterSpeed;
		for (Directory directory : metadata.getDirectories()) {
		    for (Tag tag : directory.getTags()) {
		    	if (allExtractorTags.containsKey(tag.getTagType())) {
		    		allExtractorTags.put(tag.getTagType(), tag.getDescription());
		    		if (MetaDataExtractorTags.DateTimeOriginal.getTagType() == tag.getTagType()) {
		    			tag.toString();
		    		}
		    	}
		    }
		}
		return null;
	}
	
	/**
	 * Gets the Exif Metadata from an image File
	 * 
	 * @param imageFile
	 * @return
	 */
	private Metadata getExifMetadata(File imageFile, String fileName) {
		if (imageFile == null) {
			LOGGER.warning(fileName + " Image File is null");
			return null;
		}
		Metadata meta;
		try {
			//FIXME A ugly hack to prevet a crash of the ImageMetadataReader with the file MVI_0239.MP4
            if (fileName.toUpperCase().endsWith("MP4")) {
    			LOGGER.warning(fileName + " MP4 Files are not handled, skippe file");
            	return null;
            }
			meta = ImageMetadataReader.readMetadata(imageFile);
		} catch (ImageProcessingException e) {
			LOGGER.warning("Image " + fileName + " causes an ImageProcessingException");
			return null;
		} catch (IOException e) {
			LOGGER.warning("Image " + fileName + " causes an IOException");
			return null;
		}
		return meta;
	}

}
