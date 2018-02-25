package ch.softhenge.supren.exif.service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

import ch.softhenge.supren.exif.entity.ExifFileInfo;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.properties.XMPProperty;
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
	public ExifFileInfo getExifInfoFromImageFile(File imageFile) {
		Metadata meta = getExifMetadata(imageFile);
		if (meta == null) {
			LOGGER.warning(imageFile + " has no readable metadata");
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
					e.printStackTrace();
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
		return new ExifFileInfo(cameraModel, pictureDate, rating);
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
