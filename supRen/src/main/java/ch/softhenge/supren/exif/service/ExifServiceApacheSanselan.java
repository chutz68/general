package ch.softhenge.supren.exif.service;

import ch.softhenge.supren.exif.entity.ExifFileInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.Date;
import java.util.logging.Logger;


/**
 * This service can read Exif Information out of an image File using the apache
 * imaging library.
 * 
 * @author werni
 *
 */
public class ExifServiceApacheSanselan implements ExifService {

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.softhenge.supren.exif.service.ExifService#getExifInfoFromImageFile(
	 * java.io.File)
	 */
	@Override
	public ExifFileInfo getExifInfoFromImageFile(File imageFile, String fileName) {
		Integer rating = 0;
		String cameraModel = null;
		Date pictureDate = null;

		try {
			IImageMetadata metadata = Sanselan.getMetadata(imageFile);
			
			if (metadata instanceof JpegImageMetadata) {
				TiffField modelField = ((JpegImageMetadata) metadata).findEXIFValue(ExifTagConstants.EXIF_TAG_MODEL);
				if (modelField != null) {
					cameraModel = modelField.getStringValue();
				}
				TiffField ratingField = ((JpegImageMetadata) metadata).findEXIFValue(ExifTagConstants.EXIF_TAG_RATING);
				if (ratingField != null) {
					rating = ratingField.getIntValue();
				}
				
				TiffField modifiedField = ((JpegImageMetadata) metadata).findEXIFValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
				if (modifiedField == null) {
					modifiedField = ((JpegImageMetadata) metadata).findEXIFValue(ExifTagConstants.EXIF_TAG_MODIFY_DATE);
				}
				if (modifiedField != null) {
					String stringValue = modifiedField.getStringValue().trim();

					String dateString = stringValue.split(" ")[0];
					String timeString = stringValue.split(" ")[1];

					String[] dateParts = dateString.split(":");
					String[] timeParts = timeString.split(":");

					LocalDate localDate = LocalDate.of(
							Integer.valueOf(dateParts[0]),
							Integer.valueOf(dateParts[1]),
							Integer.valueOf(dateParts[2]));
					LocalTime localTime = LocalTime.of(
							Integer.valueOf(timeParts[0]),
							Integer.valueOf(timeParts[1]),
							Integer.valueOf(timeParts[2]));

					 LocalDateTime ldt = LocalDateTime.of(localDate, localTime);
					 ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
					 pictureDate = Date.from(zdt.toInstant());
				}
				//((JpegImageMetadata) metadata).dump();				
			}
			//ImageInfo imageInfo = Sanselan.getImageInfo(imageFile);
			//imageInfo.dump();
			
		}
		catch (ImageReadException e) {
			LOGGER.warning("Image " + imageFile.getName() + " causes an ImageReadException " + e.getMessage());
		}
		catch (IOException e) {
			LOGGER.warning("Image " + imageFile.getName() + " causes an IO Exception " + e.getMessage());
		}
		catch (NumberFormatException e) {
			LOGGER.warning("Image " + imageFile.getName() + " causes an NumberFormatException " + e.getMessage());			
		}

		return new ExifFileInfo(null, cameraModel, pictureDate, rating, null, null, null, null, null, null, null);
	}

}
