package ch.softhenge.supren.exif.service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants;

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
 * This service can read Exif Information out of an image File using the apache
 * imaging library.
 * 
 * @author werni
 *
 */
public class ExifServiceApacheImaging implements ExifService {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.softhenge.supren.exif.service.ExifService#getExifInfoFromImageFile(
	 * java.io.File)
	 */
	@Override
	public ExifFileInfo getExifInfoFromImageFile(File imageFile) {
		TiffField make = null;
		HashMap<String, String> metaDataMap = new HashMap<>();

		Integer rating = 0;
		String cameraModel = null;
		Date pictureDate = null;

		try {
			IImageMetadata metadata = Sanselan.getMetadata(imageFile);
			if (metadata != null) {
				for (Object x : metadata.getItems()) {
					String xString = x.toString();
					String[] arr = xString.split(": ");
					// make, model, time, location, Software
					if (arr[0].contains("Software") || arr[0].toUpperCase().equals("MODEL")
							|| arr[0].toUpperCase().equals("MAKE") || arr[0].toUpperCase().contains("CREATE DATE")) {
						metaDataMap.put(arr[0], arr[1]);
					}
				}

				if (metadata instanceof JpegImageMetadata) {
					make = ((JpegImageMetadata) metadata).findEXIFValue(ExifTagConstants.EXIF_TAG_MAKE);
					((JpegImageMetadata) metadata).dump();
				}
			}
		}
		catch (Exception e) {

		}

		return new ExifFileInfo(cameraModel, pictureDate, rating);
	}

}
