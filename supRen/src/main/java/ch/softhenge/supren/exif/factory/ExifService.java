package ch.softhenge.supren.exif.factory;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import ch.softhenge.supren.exif.entity.ExifFileInfo;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

/**
 * This service can read Exif Information out of an image File.
 * 
 * @author werni
 *
 */
public class ExifService {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 
	
	/**
	 * Return a new ExifFileInfo Object with Exif infos from the image File imageFile.
	 * Return an ExifFileInfo Object containing null values if no Exif info is available from the file.
	 * 
	 * Be careful when using this service with many files. The Exif Tag reading is pretty slow.
	 * 
	 * @param imageFile
	 * @return
	 */
	public ExifFileInfo getExifInfoFromImageFile(File imageFile) {
		Metadata meta = getExifMetadata(imageFile);
		if (meta == null) return null;

		ExifIFD0Directory exifIFD0Directory = meta.getDirectory(ExifIFD0Directory.class);
		String cameraModel;
		if (exifIFD0Directory == null) {
			cameraModel = null;
		} else {
			cameraModel = exifIFD0Directory.getString(ExifIFD0Directory.TAG_MODEL);
		}

		ExifSubIFDDirectory exifSubIFDDirectory = meta.getDirectory(ExifSubIFDDirectory.class);
		Date pictureDate;
		if (exifSubIFDDirectory == null) {
			pictureDate = null;
		} else {
			pictureDate = exifSubIFDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
		}
		return new ExifFileInfo(cameraModel, pictureDate);
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
			LOGGER.warning("Image " + imageFile.getName() + " causes an ImageProcessingException");
			return null;
		} catch (IOException e) {
			LOGGER.warning("Image " + imageFile.getName() + " causes an IOException");
			return null;
		}
		return meta;
	}
	
}
