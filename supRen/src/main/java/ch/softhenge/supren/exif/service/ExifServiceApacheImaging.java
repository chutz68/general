package ch.softhenge.supren.exif.service;

import ch.softhenge.supren.exif.entity.ExifFileInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;

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
	public ExifFileInfo getExifInfoFromImageFile(File imageFile, String fileName) {
		Integer rating = 0;
		String cameraModel = null;
		Date pictureDate = null;

		try {
			ImageMetadata metadata = Imaging.getMetadata(imageFile);
			
			if (metadata instanceof JpegImageMetadata) {
	            final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
	            TiffField modelField = jpegMetadata.findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_MODEL_2);
				if (modelField != null) {
					cameraModel = modelField.getStringValue();
				}
	            //TiffField ratingField = jpegMetadata.findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_RATING);
				//TiffField ratingField = ((JpegImageMetadata) metadata).findEXIFValue(ExifTagConstants.EXIF_TAG_RATING);
				//if (ratingField != null) {
				//	rating = ratingField.getIntValue();
				//}
				
	            TiffField modifiedField = jpegMetadata.findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
				//if (modifiedField == null) {
				//	modifiedField = ((JpegImageMetadata) metadata).findEXIFValue(ExifTagConstants.EXIF_TAG_MODIFY_DATE);
				//}
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
			
		}
		catch (ImageReadException e) {
			LOGGER.warning("Image " + imageFile.getName() + " causes an ImageReadException");
		}
		catch (IOException e) {
			LOGGER.warning("Image " + imageFile.getName() + " causes an IO Exception");
		}

		return new ExifFileInfo(null, cameraModel, pictureDate, rating, null, null, null, null, null, null, null);
	}

}
