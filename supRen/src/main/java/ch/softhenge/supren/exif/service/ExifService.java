package ch.softhenge.supren.exif.service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
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

		ExifIFD0Directory exifIFD0Directory = null;
		Collection<ExifIFD0Directory> directoriesFD0 = meta.getDirectoriesOfType(ExifIFD0Directory.class);
		if (directoriesFD0 != null) {
			for (ExifIFD0Directory directory : directoriesFD0) {
				exifIFD0Directory = directory;
				break;
			}			
		}
		String cameraModel;
		if (exifIFD0Directory == null) {
			cameraModel = null;
		} else {
			cameraModel = exifIFD0Directory.getString(ExifIFD0Directory.TAG_MODEL);
		}

		ExifSubIFDDirectory exifSubIFDDirectory = null;
		Collection<ExifSubIFDDirectory> directoriesIFD = meta.getDirectoriesOfType(ExifSubIFDDirectory.class);
		if (directoriesIFD != null) {
			for (ExifSubIFDDirectory directory : directoriesIFD) {
				exifSubIFDDirectory = directory;
				break;
			}			
		}
		Date pictureDate;
		if (exifSubIFDDirectory == null) {
			pictureDate = null;
		} else {
			pictureDate = exifSubIFDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
			if (pictureDate == null) {
				pictureDate = exifIFD0Directory.getDate(ExifIFD0Directory.TAG_DATETIME);
			}
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
