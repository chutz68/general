package ch.softhenge.supren.exif.service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

import ch.softhenge.supren.exif.entity.ExifFileInfo;

import com.adobe.internal.xmp.XMPException;
import com.adobe.internal.xmp.XMPMeta;
import com.adobe.internal.xmp.properties.XMPProperty;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.xmp.XmpDirectory;

/**
 * This service can read Exif Information out of an image File.
 * 
 * @author werni
 *
 */
public class ExifServiceMetaDataExtractor implements ExifService {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); 
	
	/* (non-Javadoc)
	 * @see ch.softhenge.supren.exif.service.ExifService#getExifInfoFromImageFile(java.io.File)
	 */
	@Override
	public ExifFileInfo getExifInfoFromImageFile(File imageFile, String fileName) {
		Metadata meta = getExifMetadata(imageFile, fileName);
		if (meta == null) {
			LOGGER.warning(fileName + " has no readable metadata");
			return null;
		}
		
		Collection<XmpDirectory> xmps = meta.getDirectoriesOfType(XmpDirectory.class);
		Integer rating = 0;
		if (xmps != null) {
			for (XmpDirectory xmp : xmps) {
				XMPMeta xmpMeta = xmp.getXMPMeta();
				try {
					XMPProperty property = xmpMeta.getProperty("http://ns.adobe.com/xap/1.0/", "Rating");
					if (property == null) {
						rating = 0;
					} else {
						rating = Integer.valueOf(property.getValue());
					}
					break;
				} catch (XMPException e) {
					LOGGER.warning(fileName + " causes an XMPException " + e.getMessage());
					break;
				}
			}
		}
		if (rating == null) {
			rating = 0;
		}
        
		ExifIFD0Directory exifIFD0Directory = null;
		Collection<ExifIFD0Directory> directoriesFD0 = meta.getDirectoriesOfType(ExifIFD0Directory.class);
		if (directoriesFD0 != null) {
			for (ExifIFD0Directory directory : directoriesFD0) {
				exifIFD0Directory = directory;
				break;
			}			
		}
		
		String cameraModel = null;
		String cameraMake = null;
		if (exifIFD0Directory != null) {
			cameraModel = exifIFD0Directory.getString(ExifIFD0Directory.TAG_MODEL);
			cameraMake = exifIFD0Directory.getString(ExifIFD0Directory.TAG_MAKE);
		}
		
		ExifSubIFDDirectory exifSubIFDDirectory = null;
		Collection<ExifSubIFDDirectory> directoriesIFD = meta.getDirectoriesOfType(ExifSubIFDDirectory.class);
		if (directoriesIFD != null) {
			for (ExifSubIFDDirectory directory : directoriesIFD) {
				exifSubIFDDirectory = directory;
				break;
			}			
		}
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
		if (exifSubIFDDirectory != null) {
			pictureDate = exifSubIFDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
			exposureTime = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
			exposureProgram = exifSubIFDDirectory.getInteger(ExifSubIFDDirectory.TAG_EXPOSURE_PROGRAM);
			exposureMode = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_MODE);
			exposureBias = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_BIAS);
			exposureFNumber = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_FNUMBER);
			iso = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_ISO_SPEED);
			exifVersion = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_EXIF_VERSION);
			aperture = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_APERTURE);
			lensModel = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_LENS_MODEL);
			lensSpecification = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_LENS_SPECIFICATION);
			focalLength = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH);
			shutterSpeed = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_SHUTTER_SPEED);
			if (pictureDate == null) {
				pictureDate = exifIFD0Directory.getDate(ExifIFD0Directory.TAG_DATETIME);
			}
		}
		return new ExifFileInfo(cameraModel, pictureDate, rating);
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
