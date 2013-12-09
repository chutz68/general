package ch.softhenge.supren.exif.entity;

import java.io.File;

public class ImageFile {

	/**The File itself**/
	private final File file;
	/**The pattern of the file **/
	private final String filePattern;
	/**A Part of the image Filename is a number that can be used**/
	private final Integer fileNumberPart; 
	
	/**
	 * 
	 * @param file
	 * @param filePattern
	 * @param fileNumberPart
	 */
	public ImageFile(File file, String filePattern, Integer fileNumberPart)  {
		this.file = file;
		this.filePattern = filePattern;
		this.fileNumberPart = fileNumberPart;
	}
}
