package ch.softhenge.supren.exif.entity;

import java.util.Date;

/**
 * This class contains the Exif Information that was read out of an image file.
 * 
 * @author werni
 *
 */
public class ExifFileInfo {

	/** Camera Model from Exif Tag: ExifIFD0Directory.TAG_MODEL **/
	private final String cameraModel;

	/** Picture Date from Exit Tag: ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL **/
	private final Date pictureDate;

	/**
	 * Constructor
	 * 
	 * @param cameraModel
	 * @param pictureDate
	 */
	public ExifFileInfo(String cameraModel, Date pictureDate) {
		this.cameraModel = cameraModel;
		this.pictureDate = pictureDate;
	}

	public String getCameraModel() {
		return cameraModel;
	}

	public Date getPictureDate() {
		return pictureDate;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(cameraModel).append(";").append(pictureDate);
		return sb.toString();
	}

}
