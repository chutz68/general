package ch.softhenge.supren.exif.entity;

import java.io.File;
import java.util.Date;

public class ImageFile {

	/**The File itself**/
	private final File imageFile;
	/**Camera Model from Exif Tag: ExifIFD0Directory.TAG_MODEL**/
	private final String cameraModel;
	/**The 4-digit imageNumber. Can be 4 digit or null. Taken from Image File Patterns**/
	private final String imageNumber;
	/**4-character camera model. Can be either 4 char or null**/
	private final String cameraModel4ch;
	/**Picture Date from Exit Tag: ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL**/
	private final Date pictureDate;
	/**File Pattern that matched**/
	private final String filePattern;
	
	/**
	 * Constructor
	 * 
	 * @param imageFile
	 * @param cameraModel
	 * @param pictureDate
	 * @param imageNumber
	 * @param cameraModel4ch
	 * @param filePatternIndex
	 */
	public ImageFile(File imageFile, String cameraModel, Date pictureDate, String imageNumber, String cameraModel4ch, String filePattern)  {
		this.imageFile = imageFile;
		this.cameraModel = cameraModel;
		this.pictureDate = pictureDate;
		this.imageNumber = imageNumber;
		this.cameraModel4ch = cameraModel4ch;
		this.filePattern = filePattern;
		
		assert imageFile != null;
		assert imageNumber == null || (Integer.valueOf(imageNumber) >= 1000 && Integer.valueOf(imageNumber) <= 9999);
		assert cameraModel4ch == null || cameraModel4ch.length() == 4;
	}
}
