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
	
	private final int rating;

	/**
	 * Constructor
	 * 
	 * @param cameraModel
	 * @param pictureDate
	 */
	public ExifFileInfo(String cameraModel, Date pictureDate, int rating) {
		this.cameraModel = cameraModel;
		this.pictureDate = pictureDate;
		this.rating = rating;
	}

	public String getCameraModel() {
		return cameraModel;
	}

	public Date getPictureDate() {
		return pictureDate;
	}
	
	public int getRating() {
		return rating;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(cameraModel).append(";").append(pictureDate).append("; Rating: ").append(rating);
		return sb.toString();
	}

}
