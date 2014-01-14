package ch.softhenge.supren.exif.entity;

import java.io.File;

public class ImageFile {

	/**The File itself**/
	private final File imageFile;
	/**Camera Model from Exif Tag: ExifIFD0Directory.TAG_MODEL**/
	private final String imageNumber;
	/**File Pattern that matched**/
	private final String filePattern;
	
	/**The exif info of the image file**/
	private ExifFileInfo exifFileInfo;
	/**4-character camera model. Can be either 4 char or null**/
	private String cameraModel4ch;	
	
	/**
	 * Constructor
	 * 
	 * @param imageFile
	 * @param imageNumber
	 * @param filePattern
	 */
	public ImageFile(File imageFile, String imageNumber, String filePattern)  {
		this.imageFile = imageFile;
		this.imageNumber = imageNumber;
		this.filePattern = filePattern;
		
		assert imageFile != null;
		assert imageNumber == null || (Integer.valueOf(imageNumber) >= 1000 && Integer.valueOf(imageNumber) <= 9999);
		assert cameraModel4ch == null || cameraModel4ch.length() == 4;
	}

	public ExifFileInfo getExifFileInfo() {
		return exifFileInfo;
	}

	public void setExifFileInfo(ExifFileInfo exifFileInfo) {
		this.exifFileInfo = exifFileInfo;
	}

	public String getCameraModel4ch() {
		return cameraModel4ch;
	}

	public void setCameraModel4ch(String cameraModel4ch) {
		this.cameraModel4ch = cameraModel4ch;
	}

	public File getImageFile() {
		return imageFile;
	}

	public String getImageNumber() {
		return imageNumber;
	}

	public String getFilePattern() {
		return filePattern;
	}
	
	
	
}
