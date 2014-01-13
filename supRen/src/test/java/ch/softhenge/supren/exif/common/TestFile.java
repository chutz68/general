package ch.softhenge.supren.exif.common;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;

public enum TestFile {
	
    CR2File (		  "20130301_0321_E6D.CR2", 	new GregorianCalendar(2013, Calendar.MARCH, 01, 17, 02, 59)),
	Ce6dImgFile (	  "IMG_0652.jpg", 			new GregorianCalendar(2013, Calendar.APRIL, 01, 11, 14, 58)),
	OldImgFile (	  "P9040009.JPG", 			null),
	ImgFileNoFileNum ("f12345678.JPG", 			null),
	ImgFileNoPattern ("Img4453un.JPG", 			null)
	;
	
	private String filename;
	private Calendar exifDate;
	
	private TestFile(String filename, Calendar exifDate) {
		this.filename = filename;
		this.exifDate = exifDate;
	}
	
	public String getFileName() {
		return filename;
	}
	
	public Calendar getExifDate() {
		return exifDate;
	}
	
	public File getFile() {
		String fileURL = this.getClass().getClassLoader().getResource(filename).getFile();
		return new File(fileURL);
	}
}
