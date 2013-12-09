package ch.softhenge.supren.exif.factory;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class ExifService {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 
	
	/**
	 * 
	 */
	public ExifService() {
	}
	
	/**
	 * Returns the Original Date of the image from Exif Tag.
	 * If the image file has no Exif Tag, return null
	 * 
	 * @param imageFile
	 * @return
	 */
	public Date getPictureDate(File imageFile) {
		Metadata meta = getExifMetadata(imageFile);
		ExifSubIFDDirectory exifSubDir = meta.getDirectory(ExifSubIFDDirectory.class);
		return exifSubDir == null ? null : exifSubDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
	}
	
	/**
	 * Returns the camera model as String form Exif Tag
	 * If the image file has no Exif Tag, retutn null
	 * 
	 * @param imageFile
	 * @return
	 */
	public String getCameraModel(File imageFile) {
		Metadata meta = getExifMetadata(imageFile);
		ExifIFD0Directory exifDir = meta.getDirectory(ExifIFD0Directory.class);
		return exifDir == null ? null : exifDir.getString(ExifIFD0Directory.TAG_MODEL);	
	}
	
	
	/**
	 * Gets the Exif Metadata from an image File
	 * 
	 * @param imageFile
	 * @return
	 */
	private Metadata getExifMetadata(File imageFile) {
		if (imageFile == null) {
			LOGGER.warning("Image File is null");
			return null;
		}
		Metadata meta;
		try {
			meta = ImageMetadataReader.readMetadata(imageFile);
		} catch (ImageProcessingException e) {
			LOGGER.warning("Image " + imageFile.getName() + " cases an ImageProcessingException");
			return null;
		} catch (IOException e) {
			LOGGER.warning("Image " + imageFile.getName() + " cases an IOException");
			return null;
		}
		return meta;
	}
	
}
