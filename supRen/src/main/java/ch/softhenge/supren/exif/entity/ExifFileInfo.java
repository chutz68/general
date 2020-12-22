package ch.softhenge.supren.exif.entity;

import java.util.Date;

/**
 * This class contains the Exif Information that was read out of an image file.
 * 
 * @author werni
 *
 */
public class ExifFileInfo {

	private final String cameraMake;

	/** Camera Model from Exif Tag: ExifIFD0Directory.TAG_MODEL **/
	private final String cameraModel;

	/** Picture Date from Exit Tag: ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL **/
	private final Date pictureDate;
	
	private final int rating;

	private final String exposerTime;

	private final Integer exposureProgram;

	private final String exposerFNumber;

	private final String iso;

	private final String lensModel;

	private final String lensFocalLength;

    private final String lensSpecification;


	/**
	 * Constructor
	 *
	 * @param cameraModel
	 * @param pictureDate
	 */
	public ExifFileInfo(String cameraMake, String cameraModel, Date pictureDate, int rating, String exposerTime, Integer exposureProgram,
						String exposerFNumber, String iso, String lensModel, String lensFocalLength, String lensSpecification) {
		this.cameraMake = cameraMake;
		this.cameraModel = cameraModel;
		this.pictureDate = pictureDate;
		this.rating = rating;
		this.exposerTime = exposerTime;
		this.exposureProgram = exposureProgram;
		this.exposerFNumber = exposerFNumber;
		this.iso = iso;
		this.lensModel = lensModel;
		this.lensFocalLength = lensFocalLength;
		this.lensSpecification = lensSpecification;
	}

	public String getCameraMake() {
		return cameraMake != null ? cameraMake : "";
	}

	public String getCameraModel() {
		return cameraModel != null ? cameraModel : "";
	}

	public Date getPictureDate() {
		return pictureDate;
	}
	
	public int getRating() {
		return rating;
	}

	public String getExposerTime() {
		return exposerTime != null ? exposerTime : "";
	}

	public Integer getExposureProgram() {
		return exposureProgram != null ? exposureProgram : -1;
	}

	public String getExposerFNumber() {
		return exposerFNumber != null ? exposerFNumber : "";
	}

	public String getIso() {
		return iso != null ? iso : "";
	}

	public String getLensModel() {
		return lensModel != null ? lensModel : "";
	}

	public String getLensFocalLength() {
		return lensFocalLength != null ? lensFocalLength : "";
	}

	public String getLensSpecification() {
		return lensSpecification != null ? lensSpecification : "";
	}

	public static String createCsvTitle(String separator) {
		return "directory" + separator + "filename" + separator + "cameraMake" + separator + "cameraModel" + separator + "pictureDate" + separator + "rating" + separator + "exposerTime" + separator
				+ "exposureProgram" + separator + "exposerFNumber" + separator + "iso" + separator + "lensModel" +  separator + "lensFocalLength" + separator + "lensSpecification";
	}

	public String createCsvLine(String directory, String filename, String separator) {
		return directory + separator + filename + separator + getCameraMake() + separator + getCameraModel() + separator + getPictureDate() + separator + getRating() + separator + getExposerTime() + separator
				+ getExposureProgram() + separator + getExposerFNumber() + separator + getIso() + separator + getLensModel() +  separator + getLensFocalLength() + separator + getLensSpecification();
	}

	@Override
	public String toString() {
		return "ExifFileInfo{" +
				"cameraMake='" + cameraMake + '\'' +
				", cameraModel='" + cameraModel + '\'' +
				", pictureDate=" + pictureDate +
				", rating=" + rating +
				", exposerTime='" + exposerTime + '\'' +
				", exposureProgram=" + exposureProgram +
				", exposerFNumber='" + exposerFNumber + '\'' +
				", iso='" + iso + '\'' +
				", lensModel='" + lensModel + '\'' +
				", lensFocalLength='" + lensFocalLength + '\'' +
				", lensSpecification='" + lensSpecification + '\'' +
				'}';
	}
}
